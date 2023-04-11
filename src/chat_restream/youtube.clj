(ns chat-restream.youtube
  (:require
    [cheshire.core :as json]
    [org.httpkit.client :as http]
    [clojure.set :as set]
    [bogus.core :refer [debug]]))


(defn poll-stream
  [stream-id]


  (let [message-path
        [:addChatItemAction 
         :item 
         :liveChatTextMessageRenderer]
        
        previous
        (parse-long (slurp "timestamp"))
        
        html 
        (:body
          @(http/request {:url 
                          (str "https://www.youtube.com/live_chat?is_popout=1&v=" stream-id) 
                          
                          :method 
                          :get
                          
                          :user-agent 
                          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36"}))
        
        json 
        (last (re-find #"window\[\"ytInitialData\"\] = (.*);</script>" html))
        
        chat
        (json/parse-string json true)
        
        actions
        (get-in chat [:contents :liveChatRenderer :actions])

        msg-actions
        (->> actions
          (filter 
            (fn [i] 
              (some? (get-in i message-path))))
          (filter
            (fn [i] 
              (>
                (parse-long (:timestampUsec (get-in i message-path)))
                previous))))
          
        messages
        (map 
          (fn [m]
             (get-in m message-path))
          msg-actions)
        
        max-time
        (as-> messages m
          (map :timestampUsec m)
          (map parse-long m)
          (conj m previous)
          (reduce max m))
        
        
        simple-messages
        (mapv
          (fn [m]
            {:author 
             (get-in m [:authorName :simpleText])
             
             :message
             (->> m
               :message
               :runs
               (filter (fn [i] (some? (find i :text))))
               (map :text)
               (reduce str)
               )})
          messages)
        
        s
        (spit "timestamp" max-time)]
    simple-messages))


(defn poll-stream-test
  [stream-id]


  (let [message-path
        [:addChatItemAction 
         :item 
         :liveChatTextMessageRenderer]
        
        previous
        (parse-long (slurp "timestamp"))
        
        html 
        (:body
          @(http/request {:url 
                          (str "https://www.youtube.com/live_chat?is_popout=1&v=" stream-id) 
                          
                          :method 
                          :get
                          
                          :user-agent 
                          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36"}))
        
        json 
        (last (re-find #"window\[\"ytInitialData\"\] = (.*);</script>" html))
        
        chat
        (json/parse-string json true)
        
        actions
        (get-in chat [:contents :liveChatRenderer :actions])

        msg-actions
        (->> actions
          (filter 
            (fn [i] 
              (some? (get-in i message-path))))
          (filter
            (fn [i] 
              (>
                (parse-long (:timestampUsec (get-in i message-path)))
                previous))))
          
        messages
        (map 
          (fn [m]
             (get-in m message-path))
          msg-actions)
        
        max-time
        (as-> messages m
          (map :timestampUsec m)
          (map parse-long m)
          (conj m previous)
          (reduce max m))
        
        
        simple-messages
        (mapv
          (fn [m]
            {:author 
             (get-in m [:authorName :simpleText])
             
             :message
             (->> m
               :message
               :runs
               (filter (fn [i] (some? (find i :text))))
               (map :text)
               (reduce str)
               )})
          messages)
        
        s
        (spit "timestamp" max-time)]
    chat))

(comment

  
  (spit "chat.edn" (poll-stream-test "qfYXHwdggYQ")))

