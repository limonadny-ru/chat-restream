(ns chat-restream.core 
  (:gen-class)
  (:require
    [chat-restream.polling  :as polling]
    [chat-restream.lambda   :as lambda]
    [chat-restream.handling :as handling]
    [chat-restream.global   :as global]
    
    [clojure.string    :as str]
    [cheshire.core     :as json]
    
    [clj-socketio-client.core :as sio]))


(defn polling
  [config]
  (polling/run-polling config))

(defn lambda
  [config]
  (-> (lambda/->request)
      (lambda/handle-request! config)
      (lambda/response->)))

(defn -main
  [tg-token da-token 
   & {:keys [test-server] :or {test-server false}}]
  (let [config 
        { :test-server test-server
          :token tg-token
          :polling {:update-timeout 1000}
          }
        
        pin 
        (format "%06d" (rand-int 999999))
        
        s
        (sio/make-socket 
              "https://socket.donationalerts.ru:443" 
              {:transports ["websocket"]} 
              {:donation
               (fn [& args] (handling/donation-handler config
                              (json/parse-string (apply str args) true)))})]
    (sio/emit!
      s
      :add-user 
      {:token (slurp "da-token")
       :type "alert_widget"})
    
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

  (global/touch :da-token)
  (-main 
    (slurp "token")
    (slurp "da-token")
    {:test-server true}))
