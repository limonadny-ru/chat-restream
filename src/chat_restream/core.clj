(ns chat-restream.core 
  (:gen-class)
  (:require
    [chat-restream.polling  :as polling]
    [chat-restream.lambda   :as lambda]
    [chat-restream.global   :as global]
    
    [clojure.string    :as str]
    [cheshire.core     :as json]
    
    ))


(defn polling
  [config]
  (polling/run-polling config))

(defn lambda
  [config]
  (-> (lambda/->request)
      (lambda/handle-request! config)
      (lambda/response->)))

(defn -main
  [my-token & {:keys [test-server] :or {test-server false}}]
  (let [config 
        { :test-server test-server
          :token my-token
          :polling {:update-timeout 1000}
          }
        pin (format "%06d" (rand-int 999999))]
    (if-not (global/some? :admin)
      (global/write! :pin pin))
    
    (println "Отправьте боту пин-код: " (global/read :pin))
      
    (polling/run-polling config)
    #_(lambda config)))

(comment
  
   (binding [*in* (-> "trigger-request.json"
                 clojure.java.io/resource
                 clojure.java.io/reader)]
     
     (-main "...:..."))

  
  (-main 
    (slurp "token")
    {:test-server true})
  

  
  
  )
