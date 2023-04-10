(ns chat-restream.polling
  (:require 
    [tg-bot-api.telegram :as telegram]
    [chat-restream.handling :as handling]
    [cheshire.core :as json]))


(defn save-offset [offset-file offset]
  (spit offset-file (str offset)))


(defn load-offset [offset-file]
  (try
    (-> offset-file slurp Long/parseLong)
    (catch Throwable _
      nil)))

(defmacro with-safe-log
  "
  A macro to wrap Telegram calls (prevent the whole program from crushing).
  "
  [& body]
  `(try
     ~@body
     (catch Throwable e#
       (println (ex-message e#)))))


(defn run-polling
  [config]

  (let [offset-file "TELEGRAM_OFFSET"

        offset
        (load-offset offset-file)]

    (loop [offset offset]

      (let [updates
            (with-safe-log
              (telegram/get-updates config {:offset offset}))

            new-offset
            (or (some-> updates peek :update_id inc)
                offset)]

        (println (format
                   "Got %s updates, next offset: %s, updates: %s"
                   (count updates)
                   new-offset
                   (json/generate-string updates {:pretty true})))

        (when offset
          (save-offset offset-file new-offset))
        
        (if (seq updates) 
        
          (doseq [update updates]
            (println update)
            (handling/the-handler config (:message update)))
          
          (handling/the-poller config))
        
        

        (recur new-offset)))))
