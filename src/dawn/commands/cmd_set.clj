(ns dawn.commands.cmd-set
  (:require
   [clojure.string :as str]
   [dawn.commands.cmd-parser :refer [str->args]]
   [dawn.messages :refer [messages]]
   [taoensso.telemere :as tel]))

(defn remove-cmd [cmd-set cmd-name]
  (dissoc cmd-set cmd-name))

(defn merge-sets
  "Merge the second set into the first.
  If there are any commands with the same name,
  commands from the second set are kept."
  [cmds1 cmds2]
  (merge cmds1 cmds2))

(defmacro cmd [c] `[(meta (var ~c)) (var ~c)])

(defn add-cmd [cmd-set [cmd-meta cmd]]
  (let [cmd-name (:cmd-name cmd-meta)
        name-words (count (str/split cmd-name #"\s+"))
        args-val (:args-validate cmd-meta)
        cmd-map {:cmd cmd, :cmd-name cmd-name, :cmd-name-words name-words, :args-validate args-val}]
    (assoc cmd-set cmd-name cmd-map)))

(defprotocol CmdSet
  (get-commands [this]))

(defn get-cmd [msg cmds]
  (loop [[cmd-name & other-cmds] (keys cmds)]
    (when (some? cmd-name)
      (let [name-words (count (str/split cmd-name #"\s+"))
            msg-words (str/split msg #"\s+")
            msg-prefix (str/join " " (take name-words msg-words))]
        (if (= msg-prefix cmd-name)
          (get cmds cmd-name)
          (recur other-cmds))))))

(defn run-cmd [msg session]
  (tel/log! :info ["Trying to run command" msg])
  (if-let [cmd (get-cmd msg (:cmds session))]
    (let [raw-args (str/replace-first msg (:cmd-name cmd) "")
          parse (get cmd :args-parser str->args)
          validate (or (:args-validate cmd) (fn [args] args))]
      (try
        (-> raw-args
            parse
            validate
            ((fn [args] ((:cmd cmd) session args))))
        (catch Exception e
          (let [err-msg (get (ex-data e) :message (:generic-error messages))]
            ((:msg session) err-msg))
          (tel/error! ::run-cmd e)
          session)))
    (do
      ((:msg session) (:unknown-cmd messages))
      session)))
