(ns dawn.db
  (:require
   [mount.core :refer [defstate]]
   [xtdb.api :as xt]
   [xtdb.client :refer [start-client]])
  (:gen-class))

(defstate conn :start (start-client "http://localhost:3000"))
