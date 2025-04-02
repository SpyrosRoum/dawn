(ns dawn.player
  (:require
   [clojure.string :as str]
   [dawn.commands.cmd-set :as cmd :refer [CmdSet]]
   [dawn.db :refer [conn]]
   [dawn.messages :refer [messages]]
   [mount.core :refer [defstate]]
   [taoensso.telemere :as tel]
   [xtdb.api :as xt])
  (:import
   (org.springframework.security.crypto.argon2 Argon2PasswordEncoder))
  (:gen-class))

(defstate password-encoder :start (Argon2PasswordEncoder/defaultsForSpringSecurity_v5_8))

(defn cmd-player-look
  {:cmd-name "look me"}
  [p]
  ((:msg p) "Look at player called"))

(defrecord Player [display-name]
  CmdSet
  (get-commands [this]
    (-> {}
      (cmd/add-cmd (cmd/cmd cmd-player-look)))))

(defn- fetch-character
  "Fetch the player with the given username (to lower case) or nil"
  [username]
  (let [player-map (first (xt/q conn
                            '(from :player [{:xt/id $name}, *])
                            {:args {:name (str/lower-case username)}}))]
    (when (some? player-map)
      (map->Player player-map))))

(defn create-character
  "If no other character has the given username, create a new one
  and return them."
  [username password]
  (when-not (fetch-character username)
    (xt/execute-tx conn [[:put-docs :player {:xt/id (str/lower-case username)
                                             :display-name username
                                             :hashed-password (.encode password-encoder password)}]])
    (tel/event! ::char-created-success {:level :info, :data {:name (str/lower-case username)}})
    (assoc (fetch-character username) :brand-new true)))

(defn login-character
  "Try to login player with $username.
  If password is correct return the player object"
  [username password]
  (when-let [player (fetch-character username)]
    (when (.matches password-encoder password (:hashed-password player))
      (tel/event! ::char-logged-in-success {:level :debug, :data {:name (str/lower-case username)}})
      player)))
