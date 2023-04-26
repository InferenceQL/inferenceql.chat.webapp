(ns inferenceql.chat.webapp.client
  (:require ["@inferenceql/inferenceql.react" :as inferenceql.react]
            ["@mantine/core" :as mantine]
            ["@mantine/hooks" :as mantine.hooks]
            ["@tabler/icons" :as icons]
            ["react-dom" :as react-dom]
            [clojure.string :as string]
            [goog.object :as object]
            [helix.core :refer [$ <> defnc]]
            [helix.hooks :as hooks]
            [lambdaisland.fetch :as fetch]))

(enable-console-print!)

(defnc app
  []
  (let [[prose set-prose!] (mantine.hooks/useInputState "")
        [query set-query!] (hooks/use-state nil)
        [result set-result!] (hooks/use-state nil)
        [error set-error!] (hooks/use-state nil)
        [asking asking-handler] (mantine.hooks/useDisclosure false)
        [executing executing-handler] (mantine.hooks/useDisclosure false)

        handle-ask!
        (fn []
          (set-query! nil)
          (set-result! nil)
          (set-error! nil)
          (.open asking-handler)
          (-> (fetch/post "/api/prose"
                          {:accept :json
                           :content-type :json
                           :mode :same-origin
                           :cache :no-cache
                           :body {:prose prose}})
              (.then (fn [result]
                       (.close asking-handler)
                       (let [{:keys [status body]} (js->clj result)]
                         (if (= 200 status)
                           (set-query! (get body "iql"))
                           (set-error! body)))))))

        handle-execute!
        (fn []
          (set-result! nil)
          (set-error! nil)
          (.open executing-handler)
          (-> (fetch/post "/api/iql"
                          {:accept :json
                           :content-type :json
                           :mode :same-origin
                           :cache :no-cache
                           :body {:query (string/trim query)}})
              (.then (fn [result]
                       (.close executing-handler)
                       (let [{:keys [status body]} (js->clj result)]
                         (if (= 200 status)
                           (set-result! (clj->js body))
                           (set-error! body)))))))

        clear!
        (fn []
          (set-prose! "")
          (set-query! nil)
          (set-result! nil)
          (set-error! nil))

        on-key-down (mantine.hooks/getHotkeyHandler
                     (clj->js [["mod+Enter" handle-ask!]
                               ["shift+Enter" handle-ask!]]))]

    (<>
     ($ mantine/Container
        ($ mantine/Textarea
           {:mt "sm"
            :onChange set-prose!
            :value prose})
        ($ mantine/Button
           {:leftIcon ($ icons/IconQuestionMark {:size 18})
            :loading asking
            :mb "sm"
            :mt "sm"
            :onClick handle-ask!}
           "Ask")

        (when query
          (<>
           ($ inferenceql.react/HighlightInput
              {:disabled (or asking executing)
               :error (boolean error)
               :mt "sm"
               :onKeyDown on-key-down
               :onChange set-query!
               :value query})
           ($ mantine/Button
              {:disabled (or asking executing)
               :leftIcon ($ icons/IconDatabase {:size 18})
               :loading executing
               :mr "sm"
               :mt "sm"
               :onClick handle-execute!}
              "Execute")
           ($ mantine/Button
              {:leftIcon ($ icons/IconArrowBackUp {:size 18})
               :disabled (or asking executing)
               :mr "sm"
               :mt "sm"
               :onClick clear!}
              "Clear")))

        (when error
          ($ mantine/Code
             {:block true
              :color "red"
              :mt "sm"}
             (pr-str error)))

        (when result
          (let [rows (object/get result "iql/rows")
                columns (object/get result "iql/columns")]
            ($ inferenceql.react/DataTable
               {:mt "lg"
                :rows rows
                :columns columns})))))))

(let [element (js/window.document.querySelector "#app")]
  (react-dom/render ($ app {})
                    element))
