(ns chat-restream.youtube
  (:require
    [cheshire.core :as json]
    [org.httpkit.client :as http]
    [clojure.set :as set]))


(defn poll-stream
  [stream-id]


  (let [previous
        (clojure.edn/read-string (slurp "state.edn"))
        
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
        (filter 
          (fn [i] (some? (find i :addChatItemAction)))
          
          actions)
        
        messages
        (map 
          (fn [m]
             (-> m
               :addChatItemAction 
               :item 
               :liveChatTextMessageRenderer))
          msg-actions)
        
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
        
        
        
        diff
        (if previous
          (set/difference (set simple-messages) (set previous))
          #{})
        
        
        
        s
        (spit "state.edn" simple-messages)]
    diff))

