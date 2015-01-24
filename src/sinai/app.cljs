(ns sinai.app
  (:require [cljs.core.async :as async]
            [sinai.canvas :as canvas]
            [sinai.entities :as e]
            [sinai.scenes :as scene]
            [sinai.input :as input]) 
  (:require-macros [cljs.core.async.macros :as async-m]))

(defn interval
  [period]
  (let [c (async/chan)]
    (.setInterval js/window
                  #(async/put! c :tick)
                  period)
    c))

(defn watch-for-key-events
  [key-events]
  (.addEventListener js/document "keydown"
                     (fn [e]
                       (swap! key-events conj [:pressed (.-keyCode e)]))
                     false)
  (.addEventListener js/document "keyup"
                     (fn [e]
                       (swap! key-events conj [:released (.-keyCode e)]))
                     false))

(defn apply-key-events
  [app key-events]
  (reduce (fn [app [action key]]
            (case action
              :pressed (update-in app [:input] input/press key)
              :released (update-in app [:input] input/release key)))
          app key-events))

(defn launch
  [& {:keys [width height] scene :initial-scene}]
  (let [canvas (canvas/create width height)
        update-interval (interval 30)
        key-events (atom [])
        app {:scene scene
             :input input/blank}]
    (set! (.-onload js/window)
          (fn []
            (watch-for-key-events key-events)
            (.appendChild (.-body js/document) (:el canvas))
            (async-m/go-loop [app app previous-time (js/Date.) time-deltas []]
                             (async/<! update-interval)
                             (let [current-time (js/Date.)
                                   delta (- current-time previous-time)
                                   time-deltas (-> time-deltas
                                                   (->> (take-last 2))
                                                   (concat [delta]))
                                   average-time-delta (-> time-deltas
                                                           (->> (reduce +))
                                                           (/ (count time-deltas)))
                                   fps (int (/ 1000 average-time-delta))
                                   new-key-events @key-events
                                   app (-> app
                                           (apply-key-events new-key-events)
                                           (->> (scene/update (:scene app))))]
                               (reset! key-events [])
                               (canvas/clear! canvas)
                               (doseq [:let [entities (-> app :scene :entities)]
                                       id (e/get-all-ids entities)
                                       :let [{:keys [position hitbox debug-color]} (e/get entities id)]]
                                 (canvas/draw-rect! canvas
                                                    (:x position)
                                                    (:y position)
                                                    (:width hitbox)
                                                    (:height hitbox)
                                                    (or debug-color :red)
                                                    false))
                               (canvas/draw-text! canvas
                                                  10 10 fps :black)
                               (recur app
                                      current-time
                                      time-deltas)))))))
