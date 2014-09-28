(ns sinai.app
  (:require [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :as async-m]))

(defn create-canvas
  [width height]
  (let [canvas (.createElement js/document "canvas")]
    (set! (.-width canvas) width)
    (set! (.-height canvas) height)
    {:el canvas
     :context (.getContext canvas "2d")
     :width width
     :height height}))

(defn interval
  [period]
  (let [c (async/chan)]
    (.setInterval js/window
                  #(async/put! c :tick)
                  period)
    c))

(defn launch
  [& {:keys [width height]}]
  (let [canvas (create-canvas width height)
        update-interval (interval 30)]
    (set! (.-onload js/window)
          (fn []
            (.appendChild (.-body js/document) (:el canvas))
            (async-m/go-loop []
                             (async/<! update-interval)
                             (recur))))))
