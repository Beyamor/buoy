(ns sinai.app)

(defn create-canvas
  [width height]
  (let [canvas (.createElement js/document "canvas")]
    (set! (.-width canvas) width)
    (set! (.-height canvas) height)
    {:el canvas
     :context (.getContext canvas "2d")
     :width width
     :height height}))

(defn launch
  [& {:keys [width height]}]
  (let [canvas (create-canvas width height)]
    (set! (.-onload js/window)
          (fn []
            (.appendChild (.-body js/document) (:el canvas))))))
