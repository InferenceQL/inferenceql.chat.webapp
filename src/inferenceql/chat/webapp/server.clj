(ns inferenceql.chat.webapp.server
  (:require [clojure.java.io :as io]
            [clojure.stacktrace :as stacktrace]
            [com.stuartsierra.component :as component]
            [inferenceql.chat :as chat]
            [inferenceql.query.permissive :as permissive]
            [reitit.ring :as ring]
            [reitit.ring.middleware.exception :as exception]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.format-response :refer [wrap-restful-response]]
            [ring.util.response :as response]))

(defn not-found-handler
  [_request]
  (-> (response/not-found "Not found")
      (response/header "Content-Type" "text/plain")))

(def exception-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {::exception/default (fn [exception request]
                           {:status  500
                            :exception (with-out-str (stacktrace/print-stack-trace exception))
                            :uri (:uri request)
                            :body "Internal server error"})
     ::exception/wrap (fn [handler e request]
                        (println "ERROR" (pr-str (:uri request)))
                        (stacktrace/print-stack-trace e)
                        (flush)
                        (handler e request))})))

(defn page-handler
  [_]
  (response/response (slurp (io/resource "public/index.html"))))

(defn prose-handler
  [schema]
  (fn [request]
    (let [prose (get-in request [:params "prose"])
          iql (chat/query (assoc schema :english-query prose))]
      (response/response {:iql iql}))))

(defn iql-handler
  [db]
  (fn [request]
    (let [query (get-in request [:params "query"])
          result (permissive/q query db)
          columns (-> result (meta) (:iql/columns))]
      (response/response #:iql{:columns columns
                               :rows result}))))

(defn app
  [db schema]
  (ring/ring-handler
   (ring/router
    [["/" {:get page-handler}]
     ["/api" {:middleware [[wrap-restful-format :formats [:json]]
                           [wrap-restful-response]]}
      ["/iql" {:post (#'iql-handler db)}]
      ["/prose" {:post (#'prose-handler schema)}]]
     ["/js/*" (ring/create-resource-handler {:root "js"})]])
   #'not-found-handler
   {:middleware [exception-middleware]}))

(defrecord JettyServer [port db schema]
  component/Lifecycle

  (start [component]
    (let [handler (#'app db schema)
          jetty-server (jetty/run-jetty handler {:port port :join? false})]
      (assoc component :server jetty-server)))

  (stop [{:keys [server]}]
    (when server
      (.stop server))))

(defn jetty-server
  [& {:keys [port] :as opts}]
  (assert (some? port))
  (map->JettyServer opts))
