(ns sinai.rules
  (:require-macros [sinai.rules.macros :as m]))

(defn get-messages
  [[_ messages]]
  messages)

(defn return
  [value]
  (fn [state]
    [value nil]))

(defn bind
  [rule f]
  (fn [state]
    (let [[value messages] (rule state)
          [value more-messages] ((f value) state)]
      [value (concat messages more-messages)])))

(defn bind-each
  [rule f]
  (fn [state]
    (let [[values messages] (rule state)
          values-and-more-messages (map #((f %) state) values)]
      [nil (apply concat
                   (map get-messages values-and-more-messages))])))

(defn get-state
  [state]
  [state nil])

(defn apply-rules
  [state rules]
  (apply concat
         (map #(get-messages (% state)) rules)))

(defn get-in-state
  [& path]
  (m/do state <- get-state
        (return (get-in state path))))

(def get-scene
  (get-in-state :app :scene))

(defn get-in-scene
  [& path]
  (m/do scene <- get-scene
        (return (get-in scene path))))

(def get-entities
  (get-in-scene :entities))

(defn send-message
  [message-type & data]
  (fn [state]
    [nil [(concat [message-type] data)]]))

(defn update-entity
  [e f & args]
  (send-message :update-entity e #(apply f % args)))