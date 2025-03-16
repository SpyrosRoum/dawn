(ns dawn.dawn
  (:require
   [org.httpkit.server :as kit]
   [ring.middleware.defaults :refer [wrap-defaults]]
   [taoensso.telemere :as tel])
  (:gen-class))

(def channels (atom #{}))

(defn on-open    [ch]             (swap! channels conj ch))
(defn on-close   [ch status-code] (swap! channels disj ch))
(defn on-receive [ch message]
  (doseq [ch @channels]
    (kit/send! ch (str "Message: " message))))

(defn handler [ring-req]
  (if-not (:websocket? ring-req)
    {:status 200 :headers {"content-type" "text/html"} :body "Connect WebSockets to this URL."}
    (kit/as-channel ring-req
                    {:on-open    #'on-open
                     :on-receive #'on-receive
                     :on-close   #'on-close})))

(def app (wrap-defaults #'handler {:websocket {:keepalive true}}))

(comment
  (def server (kit/run-server #'app {:port 8080}))
  ,)
