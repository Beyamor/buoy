(ns sinai.app
  (:require [cljs.core.async :as async]
            [sinai.canvas :as canvas]
            [sinai.scenes :as scene])
  (:require-macros [cljs.core.async.macros :as async-m]))

(defn interval
  [period]
  (let [c (async/chan)]
    (.setInterval js/window
                  #(async/put! c :tick)
                  period)
    c))

(defn launch
  [& {:keys [width height] scene :initial-scene}]
  (let [canvas (canvas/create width height)
        update-interval (interval 30)
        app {:scene scene}]
    (set! (.-onload js/window)
          (fn []
            (.appendChild (.-body js/document) (:el canvas))
            (async-m/go-loop [app app]
                             (async/<! update-interval)
                             (let [app (scene/update (:scene app) app)]
                               (canvas/clear! canvas)
                               (doseq [{:keys [position hitbox debug-color]} (-> app :scene :entities)]
                                 (canvas/draw-rect! canvas
                                                    (:x position)
                                                    (:y position)
                                                    (:width hitbox)
                                                    (:height hitbox)
                                                    (or debug-color :red)
                                                    false))
                               (recur app)))))))
