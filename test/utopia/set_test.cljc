(ns utopia.set-test
  (:require [utopia.set :as sut]
            [clojure.test :refer [deftest testing is]]))

(deftest map-keys-test
  (testing "maps f over all keys"
    (is (= {1 :a
            2 :b
            3 :c}
           (sut/map-keys inc {0 :a
                              1 :b
                              2 :c}))))
  (testing "nil punning"
    (is (nil? (sut/map-keys inc nil)))))

(deftest map-values-test
  (testing "maps f over all values"
    (is (= {:a 1
            :b 2
            :c 3}
           (sut/map-values inc {:a 0
                                :b 1
                                :c 2}))))

  (testing "nil punning"
    (is (nil? (sut/map-values inc nil)))))

(deftest map-leaves-test
  (testing "maps f over all leaves"
    (is (= {:a 1
            :b 2
            :c {:a 1}}
           (sut/map-leaves inc {:a 0
                                :b 1
                                :c {:a 0}}))))

  (testing "nil punning"
    (is (nil? (sut/map-leaves inc nil)))))

(deftest remove-values-test
  (testing "removes all values for which f returns true"
    (is (= {:b 1
            :c 2}
           (sut/remove-values nil? {:a nil
                                    :b 1
                                    :c 2}))))

  (testing "nil punning"
    (is (nil? (sut/remove-values nil? nil)))))


(deftest namespace-keys-test
  (testing "applies a namespace to non-namespaces keys"
    (is (= {:test/foo "A"
            :test/bar "B"}
           (sut/namespace-keys "test"
                               {:foo "A"
                                :bar "B"}))))

  (testing "nil punning"
    (is (nil? (sut/namespace-keys "foo" nil)))))
