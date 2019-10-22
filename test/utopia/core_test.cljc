(ns utopia.core-test
  (:require [utopia.core :as sut]
            [clojure.test :refer [deftest testing is are]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.spec.alpha :as s]))


(def gen-simple-map
  "Generator for simple map"
  (gen/map (gen/one-of [gen/keyword gen/int gen/string]) gen/any))

(deftest deep-merge-test
  (testing "is identity for single arguments"
    (is (= 3 (sut/deep-merge 3)))
    (is (= {:a 1 :b 2} (sut/deep-merge {:a 1 :b 2}))))

  (testing "behaves like merge for unnested maps"
    (let [a {:a 1}
          b {:b 2}
          c {:a 3 :c 4}]
      (is (= (merge a b c)
             (sut/deep-merge a b c)))))

  (testing "supports merging deeply nested maps"
    (let [a {:a 1 :b {:x 2 :y 3}}
          b {:c 4 :d {:z 5}}
          c {:a 6 :b {:x 7 :z 8}}]
      (is (= {:a 6
              :b {:x 7 :y 3 :z 8}
              :c 4
              :d {:z 5}}
             (sut/deep-merge a b c))))))

(defspec apply-if-is-identity-if-pred-returns-false
  5
  (prop/for-all [x gen/any-equatable]
      (is (= x
             (sut/apply-if x (constantly false) (constantly :modified))))))

(defspec apply-if-applies-if-pred-returns-true
  5
  (prop/for-all [x gen/int]
      (is (= (inc x)
             (sut/apply-if x (constantly true) inc)))))

(defspec apply-when-is-nil-if-pred-returns-false
  5
  (prop/for-all [x gen/any-equatable]
      (is (nil?
           (sut/apply-when x (constantly false) (constantly :modified))))))

(defspec apply-when-applies-if-pred-returns-true
  5
  (prop/for-all [x gen/int]
      (is (= (inc x)
             (sut/apply-when x (constantly true) inc)))))

(deftest update-if-exists-test
  (testing "updates existing keys"
    (is (= {:a 2 :b 1}
           (sut/update-if-exists {:a 1 :b 2}
                                 :a inc
                                 :b dec))))
  (testing "does nothing for missing keys"
    (is (= {:a 1}
           (sut/update-if-exists {:a 1} :b inc))))
  #?(:clj
     (testing "raises an argument error if needed"
       (is (thrown? IllegalArgumentException
                    (sut/update-if-exists {} :b))))))

(deftest update-if-some-test
  (testing "updates existing keys"
    (is (= {:a 2 :b 1}
           (sut/update-if-some {:a 1 :b 2}
                               :a inc
                               :b dec))))
  (testing "does nothing for missing keys"
    (is (= {:a 1 :b nil}
           (sut/update-if-some {:a 1
                                :b nil} :b inc))))
  (testing "does nothing for missing keys"
    (is (= {:a 1}
           (sut/update-if-exists {:a 1} :b inc))))
  #?(:clj
     (testing "raises an argument error if needed"
       (is (thrown? IllegalArgumentException
                    (sut/update-if-some {} :b))))))

(defspec partition-keys-all-keys-first-is-identity
  100
  (prop/for-all [m gen-simple-map]
      (= m
         (first (sut/partition-keys m (keys m))))))

(defspec partition-keys-all-keys-second-is-empty
  100
  (prop/for-all [m gen-simple-map]
      (= {}
         (second (sut/partition-keys m (keys m))))))

(defspec partition-keys-no-keys-first-is-empty
  100
  (prop/for-all [m gen-simple-map]
      (= {} (first (sut/partition-keys m [])))))

(defspec partition-keys-no-keys-second-is-identity
  100
  (prop/for-all [m gen-simple-map]
      (= m (second (sut/partition-keys m [])))))


(deftest sum-test
  (testing "sums the numbers"
    (is 6 (sut/sum 1 2 3)))
  (testing "handles nil values"
    (is 3 (sut/sum 1 2 nil))))

(deftest avg-test
  (testing "returns the average of the numbers"
    (is 2 (sut/avg 1 2 3))))


(deftest round-test
  (testing "obeys precision"
    (are [expected n precision] (= expected (sut/round n precision))
      1.0   1.1234567 0
      1.1   1.1234567 1
      1.12  1.1234567 2
      1.123 1.1234567 3)))

(deftest find-first-test
  (testing "returns items based on pred"
    (is (= 2 (sut/find-first even? [1 2 3 4]))))
  (testing "applies extract if provided"
    (is (= 3 (sut/find-first even? inc [1 2 4])))))

(deftest indistinct-test
  (let [input [1 2 3 4 5 6 1 2 3 7 8 9]]
    (testing "works over a collection"
      (is (= [1 2 3]
             (->> input
                  (sut/indistinct)
                  (into [])))))
    (testing "works as a transducer"
      (is (= [1 2 3]
             (into [] (sut/indistinct) input))))))

(defspec rand-is-bounded-prop
  100
  (prop/for-all
      [x (gen/double* {:min 0 :max 1 :NaN? false :infinite? false})
       y (gen/double* {:min 1 :max 2 :NaN? false :infinite? false})]
      (<= x (sut/rand x y) y)))

(defspec rand-is-random-prop
  100
  (prop/for-all [x (gen/such-that #(not= 0.0 %) (gen/double* {:infinite? false
                                                              :NaN? false}))]
      (not= (sut/rand x) (sut/rand x))))


(deftest inspect-test
  (testing "returns itself"
    ;; Capture stdout to avoid poluting the console
    (with-out-str
      (is (= 5 (sut/inspect 5)))
      (is (= 5 (sut/inspect "value" 5)))
      (is (= 5 (sut/inspect "value" inc 5)))))

  (testing "prints the correct output"
    (is (= "5\n" (with-out-str (sut/inspect 5))))
    (is (= "Value: 5\n" (with-out-str (sut/inspect "Value" 5))))
    (is (= "Value: 6\n" (with-out-str (sut/inspect "Value" inc 5))))))


(deftest ns-select-keys-test
  (testing "works with nil ns"
    (is (= {:foo 1 :bar 2}
           (sut/ns-select-keys {:test/foo 1
                                :bar      2}
                               [:test/foo
                                :bar]))))
  (testing "works with other ns"
    (are [ns] (= {:test/foo 1 :test/bar 2}
                 (sut/ns-select-keys {:test/foo 1
                                      :bar      2}
                                     [:test/foo
                                      :bar]
                                     ns))
      :test
      "test"
      :test/foo)))

(deftest arg-test
  (testing "returns the correct argument"
    (is (= 0 ((sut/arg 1) 0 1 2 3)))
    (is (= 1 ((sut/arg 2) 0 1 2 3)))
    (is (= 2 ((sut/arg 3) 0 1 2 3)))
    (is (= 3 ((sut/arg 4) 0 1 2 3)))
    (is (= nil ((sut/arg 5) 0 1 2 3)))))


(deftest dedupe-by-test
  (testing "removes consecutive elements"
    (let [coll [{:name "Bob" :age 31}
                {:name "Ed" :age 31}
                {:name "Stacy" :age 35}
                {:name "Nemo" :age 42}]]
      (is (= ["Bob" "Stacy" "Nemo"]
             (->> coll
                  (sut/dedupe-by :age)
                  (map :name)
                  (into [])))))))

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

(deftest map-vals-test
  (testing "maps f over all values"
    (is (= {:a 1
            :b 2
            :c 3}
           (sut/map-vals inc {:a 0
                              :b 1
                              :c 2}))))

  (testing "nil punning"
    (is (nil? (sut/map-vals inc nil)))))

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

(deftest remove-vals-test
  (testing "removes all values for which f returns true"
    (is (= {:b 1
            :c 2}
           (sut/remove-vals nil? {:a nil
                                  :b 1
                                  :c 2}))))

  (testing "nil punning"
    (is (nil? (sut/remove-vals nil? nil)))))

(deftest filter-val-test
  (testing "removes all values for which f returns false"
    (is (= {:b 1
            :c 2}
           (sut/filter-vals some? {:a nil
                                   :b 1
                                   :c 2}))))

  (testing "nil punning"
    (is (nil? (sut/filter-vals nil? nil)))))

(deftest remove-keys-test
  (testing "removes all values for which f returns true"
    (is (= {:b 1
            :c 2}
           (sut/remove-keys #{:a} {:a nil
                                   :b 1
                                   :c 2}))))

  (testing "nil punning"
    (is (nil? (sut/remove-keys nil? nil)))))

(deftest filter-keys-test
  (testing "removes all values for which f returns false"
    (is (= {:b 1
            :c 2}
           (sut/filter-keys #{:b :c} {:a nil
                                      :b 1
                                      :c 2}))))

  (testing "nil punning"
    (is (nil? (sut/filter-keys nil? nil)))))


(deftest namespace-keys-test
  (testing "applies a namespace to non-namespaces keys"
    (is (= {:test/foo "A"
            :test/bar "B"}
           (sut/namespace-keys "test"
                               {:foo "A"
                                :bar "B"}))))

  (testing "nil punning"
    (is (nil? (sut/namespace-keys "foo" nil)))))
