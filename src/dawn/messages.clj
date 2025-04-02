(ns dawn.messages
  (:require
   [clojure.string :as str])
  (:gen-class))

(def messages
  {:welcome (str/join ["Hello and welcome.\n"
                       "Use |connect <username> <password>| to continue your story or"
                       "|create <username> <password>| to start a new one."])
   :bad-connect-cmd (str/join " " ["Please use"
                                   "|connect <username> <password>| to continue your story or"
                                   "|create <username> <password>| to start a new one."])
   :username-taken "Sorry, seems like that name is taken already."
   :bad-username-pass "Sorry, wrong username or password."
   :unknown-cmd "I don't know what that means"
   :generic-error "Oops, something went wrong. Sorry."})
