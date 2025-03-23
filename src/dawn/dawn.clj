(ns dawn.dawn
  (:require
   [dawn.connection.websocket :as ws]
   [manifold.stream :as s]
   [mount.core :as mount])
  (:gen-class))

(defn register-player [client-id send-func])

(defn -main [& args]
  (mount/start)
  (ws/run-ws-server 8000))

(comment
  (-main)
  ,)
