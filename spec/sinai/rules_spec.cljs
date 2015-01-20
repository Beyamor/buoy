(ns sinai.rules-spec
  (:require [speclj.core]
            [sinai.rules :as r :include-macros true])
  (:require-macros [speclj.core :refer [describe it should= should should-not should-throw with]]))

(describe "get-messages"
          (it "should grab the messages returned by some rule."
              (should= [:message1 :message2]
                       (r/get-messages [:ignored [:message1 :message2]]))))

(defn should-yield-messages
  [expected-messages state rule]
  (should= expected-messages (r/get-messages 
                               (rule state))))

(describe "do"
          (it "should return its body's messages."
              (should-yield-messages [[:message]]
                                     nil
                                     (r/do (r/send-message :message))))

          (it "should combine messages."
              (should-yield-messages [[:message 1] [:message 2]]
                                     nil
                                     (r/do (r/send-message :message 1)
                                           (r/send-message :message 2))))

          (it "should allow state to be read."
              (should-yield-messages [[:value-of-state :state]]
                                     :state
                                     (r/do state <- r/get-state
                                           (r/send-message :value-of-state state))))

          (it "should allow for-each style reaching too."
              (should-yield-messages [[:value 1] [:value 2] [:value 3]]
                                     [1 2 3]
                                    (r/do value << r/get-state
                                          (r/send-message :value value)))))

(describe "let"
          (it "should allow for bindings in its body."
              (should-yield-messages [[:y 2]]
                                     1
                                     (r/do x <- r/get-state
                                           (r/let [y (inc x)]
                                             (r/send-message :y y))))))

(describe "get-in-state"
          (it "should get a value in state."
              (should-yield-messages [[:value-in-state :value]]
                                     {:value-in-state :value}
                                     (r/do value <- (r/get-in-state :value-in-state)
                                           (r/send-message :value-in-state value))))

          (it "should get nested values."
              (should-yield-messages [[:nested-value :value]]
                                     {:nested {:value :value}}
                                     (r/do value <- (r/get-in-state :nested :value)
                                           (r/send-message :nested-value value)))))

(describe "get-in-scene"
          (it "should get a value in the app's scene."
              (should-yield-messages [[:scene-value :value]]
                                     {:app {:scene {:value :value}}}
                                     (r/do value <- (r/get-in-scene :value)
                                           (r/send-message :scene-value value)))))

(describe "apply-rules"
          (it "should apply every rule and return their messages"
              (should= [[:message1] [:message2]]
                       (r/apply-rules
                         nil
                         [{:action (r/send-message :message1)}
                          {:action (r/send-message :message2)}]))))
