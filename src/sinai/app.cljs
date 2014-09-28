(ns sinai.app
  (:require [cljs.core.async :as async]
            [sinai.canvas :as canvas])
  (:require-macros [cljs.core.async.macros :as async-m]))

(defn interval
  [period]
  (let [c (async/chan)]
    (.setInterval js/window
                  #(async/put! c :tick)
                  period)
    c))

(defn launch
  [& {:keys [width height]}]
  (let [canvas (canvas/create width height)
        update-interval (interval 30)]
    (set! (.-onload js/window)
          (fn []
            (.appendChild (.-body js/document) (:el canvas))
            (async-m/go-loop []
                             (async/<! update-interval)
                             (canvas/clear! canvas)
                             (let [x (* (Math/random) 100)
                                   y (* (Math/random) 100)]
                               (canvas/draw-rect! canvas x y 50 50 :red false))
                             (recur))))))
