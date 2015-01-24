(ns sinai.canvas)

(defprotocol Color
  (->color [this]))

(extend-protocol Color
  Keyword
  (->color [this]
    (name this)))

(defn create
  [width height]
  (let [canvas (.createElement js/document "canvas")]
    (set! (.-width canvas) width)
    (set! (.-height canvas) height)
    {:el canvas
     :context (.getContext canvas "2d")
     :width width
     :height height}))

(defn clear!
  [{:keys [context width height]}]
  (.clearRect context 0 0 width height))

(defn draw-rect!
  [{:keys [context ]} x y width height color fill?]
  (let [color (->color color)]
    (doto context
      .beginPath
      (.rect x y width height))
    (if fill?
      (do (set! (.-fillStyle context) color)
          (.fill context))
      (do (set! (.-strokeStyle context) color)
          (.stroke context)))))

(defn draw-text!
  [{:keys [context]} x y text color]
  (let [text (str text)
        color (->color color)]
    (doto context
      .beginPath
      (-> .-fillStyle (set! color))
      (.fillText text x y))))

