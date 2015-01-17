(ns sinai.scenes-spec
  (:require [speclj.core]
            [sinai.scenes :as s]
            [sinai.rules :as r :include-macros true])
  (:require-macros [speclj.core :refer [describe it should= should should-not should-throw with]]))

(describe "apply-handlers"
          (it "should dispatch to handlers by message type."
              (should= [:handler1 :handler2]
                       (s/apply-handlers
                         []
                         {:message1 #(conj % :handler1)
                          :message2 #(conj % :handler2)}
                         [[:message1] [:message2]])))

          (it "should forward data in the messages"
              (should= [1 2]
                       (s/apply-handlers
                         []
                         {:message (fn [state datum]
                                     (conj state datum))}
                         [[:message 1] [:message 2]]))))

(r/defrule send-message-1
  :on :frame-entered
  (r/send-message :message 1))

(r/defrule send-message-2
  :on :frame-entered
  (r/send-message :message 2))

(describe "a StandardScene"
          (let [app {:scene (s/create-scene
                              :rules [send-message-1
                                      send-message-2]
                              :handlers {:message (fn [state value]
                                                    (update-in state [:values] (fnil conj []) value))}
                              :entities {})}]
            (it "should apply its rules and handlers when updating."
                (should= [1 2]
                         (:values
                           (s/update (:scene app) app))))))
