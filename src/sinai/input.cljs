(ns sinai.input)

(def blank
  {:current {}
   :previous {}})

(def bindings (atom {}))

(defn bind
  [& args]
  (doseq [[name value] (partition 2 args)]
    (swap! bindings assoc name value)))

(defn ->key
  [value]
  (if (contains? @bindings value)
    (->key (get @bindings value))
    value))

(defn press
  [{:keys [current]} key]
  {:previous current
   :current (assoc current key :down)})

(defn release
  [{:keys [current]} key]
  {:previous current
   :current (assoc current key :up)})

(defn is-down-in?
  [state key]
  (= :down (get state key)))

(def is-up-in? (complement is-down-in?))

(defn is-down?
  [{:keys [current]} key]
  (is-down-in? current (->key key)))

(defn was-pressed?
  [{:keys [current previous]} key]
  (let [key (->key key)]
    (and (is-down-in? current key)
         (is-up-in? previous key))))

(defn is-up?
  [{:keys [current]} key]
  (is-up-in? current (->key key)))

(defn was-released?
  [{:keys [current previous]} key]
  (let [key (->key key)]
    (and (is-up-in? current key)
         (is-down-in? previous key))))
