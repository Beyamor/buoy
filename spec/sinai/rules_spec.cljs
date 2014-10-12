(ns sinai.rules-spec
  (:require [speclj.core]
            [sinai.rules :as r])
  (:require-macros [speclj.core :refer [describe it should= should should-not should-throw with]]
                   [sinai.rules.macros :as m]))

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
                                     (m/do (r/send-message :message))))

          (it "should combine messages."
              (should-yield-messages [[:message 1] [:message 2]]
                                     nil
                                     (m/do (r/send-message :message 1)
                                           (r/send-message :message 2))))

          (it "should allow state to be read."
              (should-yield-messages [[:value-of-state :state]]
                                     :state
                                     (m/do state <- r/get-state
                                           (r/send-message :value-of-state state)))))

(describe "get-in-state"
          (it "should get a value in state."
              (should-yield-messages [[:value-in-state :value]]
                                     {:value-in-state :value}
                                     (m/do value <- (r/get-in-state :value-in-state)
                                           (r/send-message :value-in-state value)))))
