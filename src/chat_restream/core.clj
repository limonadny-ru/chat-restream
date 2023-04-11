(ns chat-restream.core 
  (:gen-class)
  (:require
    [chat-restream.polling  :as polling]
    [chat-restream.lambda   :as lambda]
    [clojure.string    :as str]
    [cheshire.core     :as json]))


(defn polling
  [config]
  (polling/run-polling config))

(defn lambda
  [config]
  (-> (lambda/->request)
      (lambda/handle-request! config)
      (lambda/response->)))

(defn -main
  [my-token chat-id admin-id & {:keys [test-server] :or {test-server false}}]
  
  (let [config 
        { :test-server test-server
          :token my-token
          :chat-id (parse-long chat-id)
          :admin-id (parse-long admin-id)
          :polling {:update-timeout 1000}
          }]
  (polling/run-polling config)
  #_(lambda config)))

(comment
  
   (binding [*in* (-> "trigger-request.json"
                 clojure.java.io/resource
                 clojure.java.io/reader)]
     
     (-main "...:..."))

  
  (-main 
    (slurp "token")
    "-1005000345705"
    "5000885090"
    {:test-server true})
  
  )
