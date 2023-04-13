(ns chat-restream.global)



(defn read
  [k]
  (slurp (name k)))


(defn write!
  [k d]
  (spit (name k) d))

(defn touch
  [k]
  (spit (name k) ""))


(defn some?
  [k]
  (seq (slurp (name k))))


(defn recipient []
  (if (some? :chat)
      (read :chat)
      (read :admin)))