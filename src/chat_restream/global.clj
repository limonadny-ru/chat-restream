(ns chat-restream.global)



(defn read
  [k]
  (slurp (name k)))

(defn write!
  [k d]
  (spit (name k) d))

(defn some?
  [k]
  (seq (slurp (name k))))