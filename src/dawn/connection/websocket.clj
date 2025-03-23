(ns dawn.connection.websocket
  (:require
   [clojure.string :as str]
   [dawn.cmd-parser :refer [str->cmd]]
   [dawn.messages :refer [messages]]
   [dawn.player :as player]
   [manifold.stream :as s]
   [org.httpkit.server :as kit]
   [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
   [taoensso.telemere :as tel])
  (:gen-class))

(defn on-open
  "Try to log in/sign up the user and associate the client id with a user id.
  If it fails close the connection."
  [chn client-id]
  (tel/log! :debug ["Opened websocket with client id" client-id])
  (kit/send! chn (:welcome messages)))

(defn on-close [chn status-code client-id] (player/on-player-disconnect client-id))

(defn on-receive [chn msg client-id]
  (if-let [client (player/get-player-by-client-id client-id)]
    (throw (ex-info "Not implemented" {}))
    (player/on-player-connect (str->cmd msg) client-id #((kit/send! chn %) nil))))

(defn handler [ring-req]
  (if-not (:websocket? ring-req)
    {:status 200 :headers {"content-type" "text/html"} :body "Connect WebSockets to this URL, passing a client uuid."}
    (let [client-id (random-uuid)]
      (tel/event! ::websocket-connected {:let [client-id client-id]
                                         :data {:client-id client-id}
                                         :msg ["Connected client" client-id]})
      (kit/as-channel ring-req
        {:on-open    (fn [ch] (on-open ch client-id))
         :on-receive (fn [ch message] (on-receive ch message client-id))
         :on-close   (fn [ch status-code] (on-close ch status-code client-id))}))))

(def middleware-options (-> api-defaults
                            (assoc-in [:websocket :keepalive] true)))

(def app (wrap-defaults #'handler middleware-options))

(defn run-ws-server [port]
  (kit/run-server #'app {:port port}))
