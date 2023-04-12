(ns chat-restream.handling
  (:require
    [tg-bot-api.telegram :as telegram]
    [chat-restream.youtube :as youtube]
    [chat-restream.global :as global])
  (:import java.time.LocalDateTime))

(defn from-admin?
  [message]
  (= 
    (str (-> message :from :id))
    (global/read :admin)))


(defn the-handler 
  "Bot logic here"
  [config message]
  
  (let [my-id (:id (telegram/get-me config))]
    (cond 
      
      ; set new stream
      (and
        (:text message)
        (from-admin? message))
      (let [stream-id 
            (last 
              (re-find 
                #"^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?)|(live\/))\??v?=?([^#&?]*).*"
                (:text message)))]
        
          (do
            (global/write! :stream stream-id)
            (global/write! :state.edn "[]")
            (telegram/send-message config (-> message :chat :id) (str "Новый айди стрима: " stream-id))))
      
      ; admin adds to chat
      (and
        (seq
          (filter 
            (fn [{:keys [id]}] (= my-id id))
            (:new_chat_members message)))
        (from-admin? message))
      (do
        (global/write! :chat (-> message :chat :id))
        (telegram/send-message 
          config
          (-> message :chat :id)
          "В этот чат я и буду рестримить трансляцию"))
      
      ; set new admin
      (= (:text message) (global/read :pin))
      (do 
        (global/write! :admin (-> message :from :id))
        (telegram/send-message 
          config 
          (-> message :from :id) 
          "Вы теперь мой админ.\n\nОтправьте мне в ЛС ссылку на публичную трансляцию, и я буду рестримить ее чат.\n\nДобавьте меня в беседу, и я буду рестримить туда."))
      
      
      (some? (:text message))
      (telegram/send-message config (-> message :chat :id) "Вы не админ, не пишите мне"))))


(defn the-poller
  [config]
  
  
  (let [new-messages (youtube/poll-stream (global/read :stream))]
    
    (if (global/some? :admin)
      (mapv
        (fn [m]
      
          (telegram/send-message 
            config 
            (if (global/some? :chat)
                (global/read :chat)
                (global/read :admin)) 
            (str "<i>" (:author m) "</i> " "\n" (:message m))
            {:parse-mode "html"})
          
          (println 
            (subs (str (LocalDateTime/now)) 11 19) 
            (str (:author m) ": " (:message m))))
        
        (sort-by :timestampUsec new-messages)))))


(comment
  
  
  (telegram/get-me {:token (slurp "token") :test-server true})

  (re-find 
    #"^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?)|(live\/))\??v?=?([^#&?]*).*"
    "https://www.youtube.com/live/aNEOjZhcLxI?feature=share"))