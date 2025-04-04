(ns dawn.session
  "The currently active sessions.
  A Session holds the player with his state"
  (:require
   [dawn.commands.cmd-set :as cmd :refer [CmdSet]]
   [dawn.messages :refer [messages]]
   [dawn.player :as player]
   [taoensso.telemere :as tel]))

;; client-id -> Session
(def sessions (atom {}))

(defn char-connected-cmds [cmds]
  (-> cmds
      (cmd/remove-cmd "create")
      (cmd/remove-cmd "login")))

(defn expect-username-pass [args]
  (if-not (= (count (:args args)) 2)
    (throw (ex-info
             "Wrong number of args, expected username and password"
             {:message (:bad-connect-cmd messages)}))
    args))

(defn cmd-login-char
  {:cmd-name "login"
   :args-validate expect-username-pass}
  [session {args :args}]
  (let [username (first args)
        password (second args)
        character (player/login-character username password)]
    (if (some? character)
      (do
        ((:msg session) (format "Welcome back, %s" (:display-name character)))
        (-> session
          (assoc :char character)
          (update :cmds char-connected-cmds)
          (update :cmds cmd/merge-sets (cmd/get-commands character))))
        (throw (ex-info
                 "Bad username or password"
                 {:message (:bad-username-pass messages)})))))

(defn cmd-create-char
  {:cmd-name "create"
   :args-validate expect-username-pass}
  [session {args :args}]
  (let [username (first args)
        password (second args)
        character (player/create-character username password)]
    (if (some? character)
      (do
        ((:msg session) (format "Welcome aboard, %s" (:display-name character)))
        (-> session
          (assoc :char character)
          (update :cmds char-connected-cmds)
          (update :cmds cmd/merge-sets (cmd/get-commands character))))
      (throw (ex-info
               "This character name is already taken."
               {:message (:username-taken messages)})))))

(defrecord Session [char msg cmds]
  CmdSet
  (get-commands [this]
    (-> {}
        (cmd/add-cmd (cmd/cmd cmd-create-char))
        (cmd/add-cmd (cmd/cmd cmd-login-char)))))

(defn get-session-by-client-id [client-id]
  (get @sessions client-id))

(defn on-session-disconnect
  "Called when a client closes"
  [client-id]
  (swap! sessions dissoc client-id))

(defn mk-empty-session
  "Make an un-authenticated session
  with no player attached"
  [msg-func]
  (let [s (->Session nil msg-func {})]
    (assoc s :cmds (cmd/get-commands s))))

(defn add-session [client-id session]
  (swap! sessions assoc client-id session))

(defn on-new-session [s]
  (tel/log! :debug "Running new session handler")
  ((:msg s) (:welcome messages)))

(comment
  (keys @sessions)
  (let [s (get @sessions #uuid "b613df2a-119e-4cb5-bcc6-0830446e39e5")]
    (:cmds s))
  ,)
