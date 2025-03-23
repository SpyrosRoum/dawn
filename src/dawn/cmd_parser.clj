(ns dawn.cmd-parser
  "Parse a message to a command map"
  (:require
   [clojure.string :as str])
  (:gen-class))

(defn str->cmd
  "Parse a string to a command and its flags"
  [msg]
  (let [[first & args] (str/split (str/trim msg) #"\s+")
        [cmd & flags] (str/split first #"/")]
    {:cmd cmd, :flags flags, :args args}))

(comment
  ;; TODO: Turn these to a test...
  (parse-str "blah/one/two arg1 arg2")
  (parse-str "blah /arg1 arg2")
  (parse-str "blah")
  (parse-str "blah    one\ttwo")
  ,)
