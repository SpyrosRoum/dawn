(ns dawn.connection.websocket
  (:require
   [clojure.string :as str]
   [dawn.commands.cmd-set :refer [run-cmd]]
   [dawn.messages :refer [messages]]
   [dawn.player :as player]
   [dawn.session :as sess]
   [manifold.stream :as s]
   [org.httpkit.server :as kit]
   [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
   [taoensso.telemere :as tel])
  (:gen-class))

(defn on-open
  "Try to log in/sign up the user and associate the client id with a user id.
  If it fails close the connection."
  [chn client-id]
  (tel/log! :debug ["Opened websocket with client id:" client-id])
  (let [session (sess/mk-empty-session (fn [m] (kit/send! chn m) nil))]
    (sess/add-session client-id session)
    (sess/on-new-session session)))

(defn on-close [chn status-code client-id] (sess/on-session-disconnect client-id))

(defn on-receive [chn msg client-id]
  (when-let [s (sess/get-session-by-client-id client-id)]
    (let [new-sess (run-cmd msg s)]
      (sess/add-session client-id new-sess))))

(defn handler [ring-req]
  (if-not (:websocket? ring-req)
    {:status 200 :headers {"content-type" "text/html"} :body "Connect WebSockets to this URL, passing a client uuid."}
    (let [client-id (random-uuid)]
      (tel/event! ::websocket-connected {:let [client-id client-id]
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
