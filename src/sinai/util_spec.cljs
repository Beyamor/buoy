(ns sinai.util-spec
  (:require [speclj.core]
            [sinai.util :as util])
  (:require-macros [speclj.core :refer [describe it should= should should-not should-throw with]]))

(describe "signum"
          (it "should return its input's sign."
              (should= 1 (util/signum 10))
              (should= 0 (util/signum 0))
              (should= -1 (util/signum -10))))
