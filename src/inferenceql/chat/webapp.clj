(ns inferenceql.chat.webapp
  (:require [clojure.edn :as edn]
            [clojure.java.browse :as browse]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [inferenceql.chat.webapp.server :as server]
            [inferenceql.inference.gpm :as gpm])
  (:import [java.io PushbackReader]))

(defn read-db
  [path]
  (edn/read {:readers gpm/readers} (PushbackReader. (io/reader path))))

(defn read-schema
  [path]
  (edn/read (PushbackReader. (io/reader path))))

(defn new-system
  [& {:as opts}]
  (let [opts (merge opts {:port 8080})]
    (server/jetty-server opts)))

(defn run
  [& {:keys [port] :or {port 8080} :as opts}]
  (assert (some? port))
  (assert (contains? opts :db))
  (assert (contains? opts :schema))
  (let [opts (-> opts
                 (update :db read-db)
                 (update :schema read-schema))]
    (component/start (new-system opts))
    (browse/browse-url (str "http://localhost:" port))))
