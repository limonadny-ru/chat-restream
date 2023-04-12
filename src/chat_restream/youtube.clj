(ns chat-restream.youtube
  (:require
    [cheshire.core :as json]
    [org.httpkit.client :as http]
    [clojure.set :as set]
    [bogus.core :refer [debug]]
    
    [chat-restream.global :as global]))


(defn poll-stream
  [stream-id]


  (let [message-path
        [:addChatItemAction 
         :item 
         :liveChatTextMessageRenderer]
        
        state
        (clojure.edn/read-string (global/read :state.edn))
        
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

        messages
        (->> actions
          (filter 
            (fn [i] 
              (some? (get-in i message-path))))
          
          (map 
            (fn [m]
               (get-in m message-path)))
          
          (map
            (fn [m]
              (assoc m :timestampUsec (parse-long (:timestampUsec m)))))
          
          (sort-by :timestampUsec)
          
          (take-last 10))
        
        
        simple-messages
        (mapv
          (fn [m]
            {:timestampUsec
             (:timestampUsec m)
             
             :author 
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
        
        diff
        (set/difference (set simple-messages) (set state))
        
        s
        (global/write! :state.edn simple-messages)]
    (println diff)
    diff))

(comment
  
  (sort-by :a [{:a 1} {:a 3} {:a 2}])
  
  (poll-stream "qfYXHwdggYQ"))

