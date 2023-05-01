(ns user
  (:require [clojure.string :as string]
            [clojure.tools.namespace.repl :as repl]
            [com.stuartsierra.component :as component]
            [inferenceql.chat.webapp :as webapp]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as server]))

(def db-path "/Users/zane/projects/inferenceql.auto-modeling/data/db.edn")
(def schema-path "/Users/zane/projects/inferenceql.auto-modeling/data/schema.edn")

(defonce system nil)

(defn nilsafe
  "Returns a function that calls f on its argument if its argument is not nil."
  [f]
  (fn [x]
    (when x
      (f x))))

(def db (delay (webapp/read-db db-path)))
(def schema (delay (webapp/read-schema schema-path)))

(defn distinct-values
  [column rows]
  (into #{}
        (comp (map #(get % column))
              (distinct)
              (remove nil?)
              (remove string/blank?))
        rows))


(defn chat-schema
  [db schema]
  (let [[table-name rows] (-> db (:iql/tables) (first))]
    {:table-name table-name
     :model-name (-> db (:iql/models) (keys) (first))
     :columns (into []
                    (comp (remove #(= :ignore (val %)))
                          (map (fn [[name type]]
                                 (let [name (symbol name)
                                       type (case type
                                              :numerical "INT"
                                              :nominal (str "ENUM("
                                                            (->> (distinct-values name rows)
                                                                 (take 3)
                                                                 (map pr-str)
                                                                 (string/join ", " ))
                                                            ")"))]
                                   {:name name :type type}))))
                    schema)}))

(defn init
  "Constructs the current development system."
  []
  (server/start!)
  (shadow/watch :app)
  (alter-var-root #'system
                  (fn [_] (webapp/new-system
                           {:port 8080
                            :db @db
                            :schema (chat-schema @db @schema)}))))

(defn start
  "Starts the current development system."
  []
  (alter-var-root #'system component/start))

(defn stop
  "Shuts down and destroys the current development system."
  []
  (alter-var-root #'system (nilsafe component/stop)))

(defn go
  "Initializes the current development system and starts it running."
  []
  (init)
  (start))

(defn reset []
  (stop)
  (shadow/watch-compile! :app)
  (repl/refresh :after 'user/go))

(comment

  (set! *print-level* 4)
  (set! *print-length* 5)

  (server/start!)
  (server/stop!)
  (shadow/watch :app)
  (shadow/compile :app)

  (go)
  (reset)
  (stop)
  (start)

  system

  ,)
