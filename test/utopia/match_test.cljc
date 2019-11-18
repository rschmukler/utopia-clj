(ns utopia.match-test
  (:require [utopia.match :as sut]
            #?(:clj [clojure.test :as t :refer [deftest is testing]]
               :cljs [cljs.test :as t :include-macros true :refer [deftest is testing]])))

(deftest match?-test
  (testing "returns true if the pattern matches"
    (is (sut/match? 5 5))
    (is (sut/match? 5 _))
    (is (not (sut/match? 5 [x])))))
