(ns dawn.player
  (:require
   [clojure.string :as str]
   [dawn.db :refer [conn]]
   [dawn.messages :refer [messages]]
   [mount.core :refer [defstate]]
   [taoensso.telemere :as tel]
   [xtdb.api :as xt])
  (:import
   (org.springframework.security.crypto.argon2 Argon2PasswordEncoder))
  (:gen-class))

(defstate password-encoder :start (Argon2PasswordEncoder/defaultsForSpringSecurity_v5_8))

(def players (atom {}))

(defn- fetch-player
  "Fetch the player with the given username (to lower case) or nil"
  [username]
  (first (xt/q conn
           '(from :player [{:xt/id $name}, *])
           {:args {:name (str/lower-case username)}})))

(defn- create-player
  "If no other player has the given username, create a new one
  and return them."
  [username password]
  (when-not (fetch-player username)
    (xt/execute-tx conn [[:put-docs :player {:xt/id (str/lower-case username)
                                             :display-name username
                                             :hashed-password (.encode password-encoder password)}]])
    (tel/event! ::char-created-success {:level :debug, :data {:name (str/lower-case username)}})
    (assoc (fetch-player username) :brand-new true)))

(defn- login-player
  "Try to login player with $username.
  If password is correct return the player object"
  [username password]
  (when-let [player (fetch-player username)]
    (when (.matches password-encoder password (:hashed-password player))
      (tel/event! ::char-logged-in-success {:level :debug, :data {:name (str/lower-case username)}})
      player)))

(defn- get-connect-func [cmd]
  (condp = (:cmd cmd)
    "create" [create-player :username-taken]
    "connect" [login-player :bad-username-pass]))

(defn- auth-player
  "Handle a connect or create command"
  [cmd msg-func]
  (if (and (= (count (:args cmd)) 2) (or (= (:cmd cmd) "create") (= (:cmd cmd) "connect")))
    (let [[connect-func err-tag] (get-connect-func cmd)]
      (if-let [player (connect-func (first (:args cmd)) (second (:args cmd)))]
        player
        (msg-func (get messages err-tag))))
    (msg-func (:bad-connect-cmd messages))))

(defn on-player-connect
  "Called when we receive a message from a client with no corresponding player.
  We should try to sing them up or authenticate them and set up the event stream"
  [cmd client-id msg-func]
  (when-let [player (auth-player cmd msg-func)]
    (tel/event! ::player-connected {:level :trace :data {:player player}})
    (swap! players assoc client-id (assoc player :msg msg-func))
    (msg-func (format "Welcome, %s" (:display-name player)))))

(defn on-player-disconnect
  "Called when a client closes"
  [client-id]
  (swap! players dissoc client-id))

(defn get-player-by-client-id [client-id]
  (get @players client-id))

(comment
  (reset! players {})
  ,)
