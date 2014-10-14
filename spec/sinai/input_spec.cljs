(ns sinai.input-spec
  (:require [speclj.core]
            [sinai.input :as input])
  (:require-macros [speclj.core :refer [describe it should= should should-not should-throw with]]))

(describe "input"
          (let [input (-> input/blank
                          (input/press 0))]
            (it "should allow things to be pressed."
                (should (input/is-down? input 0))
                (should (input/was-pressed? input 0))))

          (let [input (-> input/blank
                          (input/press 0)
                          (input/release 0))]
            (it "should allow things to be released"
                (should (input/is-up? input 0))
                (should (input/was-released? input 0)))))

(input/bind :test 0)
(describe "creating key bindings"
          (it "should allow global bindings to be created."
              (should (-> input/blank
                          (input/press 0)
                          (input/was-pressed? :test)))))
