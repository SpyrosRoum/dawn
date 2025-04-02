(ns dawn.commands.cmd-parser
  "Parse a message to a command map"
  (:require
   [clojure.string :as str])
  (:gen-class))

(defn str->args
  "Default parser for commands.
  Parses a string to a command and its args.
  TODO: Flags?"
  [rest-msg]
  (if (empty? rest-msg)
    {:args nil}
    (let [args (str/split (str/trim rest-msg) #"\s+")]
      {:args args})))

(comment
  (let [cmd-name "look me"
        name-words (count (str/split cmd-name #"\s+"))
        msg (str/split "look me" #"\s+")
        msg-prefix (str/join " " (take name-words msg))]
    (= msg-prefix cmd-name))

  (str/starts-with? "look meme" "look me")
  ;; TODO: Turn these to a test...
  (str->cmd "arg1 arg2")
  (str->cmd "arg1")
  (str->cmd "")
  ,)
