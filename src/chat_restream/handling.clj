(ns chat-restream.handling
  (:require
    [tg-bot-api.telegram :as telegram]
    [chat-restream.youtube :as youtube]))


(defn the-handler 
  "Bot logic here"
  [config message]
  
  (let [stream-id 
        (last 
          (re-find 
            #"^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?)|(live\/))\??v?=?([^#&?]*).*"
            (:text message)))]
    
    
    
    (if (= 
          (-> message :from :id)
          (:admin-id config))
      (do
        (spit "state.edn" "")
        (spit "stream" stream-id)
        (telegram/send-message config (-> message :chat :id) (str "Новый айди стрима: " stream-id)))
      
      (telegram/send-message config (-> message :chat :id) "Вы не админ, не пишите мне"))))


(defn the-poller
  [config]
  
  
  (let [new-messages (youtube/poll-stream (slurp "stream"))]
    
    
    
    (mapv
      
      
      (fn [m]
        
        (telegram/send-message 
          config 
          (:chat-id config) 
          (str (:author m) ": " (:message m)))
        
        )
      
      new-messages)))


(comment

  (re-find 
    #"^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?)|(live\/))\??v?=?([^#&?]*).*"
    "https://www.youtube.com/live/aNEOjZhcLxI?feature=share")
  
  )