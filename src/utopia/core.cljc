(ns utopia.core
  "Namespace providing extensions to clojure's standard library"
  (:require [clojure.core :as core])
  (:refer-clojure :exclude [rand]))


(defn- deep-merge*
  "Recursive helper function for deep-merge. Keeps the right-most value
  within nested maps."
  [a b]
  (if (and (map? a) (map? b))
    (merge-with deep-merge* a b)
    b))

(defn deep-merge
  "Similar to merge, but nested maps will be merged.

  If any non-nested map value is duplicated, the right associated value will be
  kept. This includes scenarios where a left-bound map is merged with a
  right-bound primitive."
  [x & [y & maps]]
  (if-not y
    x
    (recur (deep-merge* x y) maps)))

(defn apply-if
  "Conditionally applies `(f x)` if `pred` returns `true`, otherwise returns `x`."
  [x pred f]
  (if (pred x)
    (f x)
    x))

(defn apply-when
  "Conditionally applies `(f x)` if `pred` returns `true`, otherwise returns nil"
  [x pred f]
  (when (pred x)
    (f x)))

(defn update-if-exists
  "Like `clojure.core/update`, but only calls `f` if the `k` exists
  in the `m`."
  [m & {:as kfns}]
  (into m (for [[k v] (select-keys m (keys kfns))]
            [k ((kfns k) v)])))

(defn update-if-some
  "Like `clojure.core/update`, but only calls `f` if the `k` exists
  in the `m` and it has a non-nil value."
  [m & {:as kfns}]
  (into m (for [[k v] (select-keys m (keys kfns)) :when (some? v)]
            [k ((kfns k) v)])))


(defn partition-keys
  "Similar to `clojure.core/select-keys` but returns a vector of two maps. The first is
  the map with the selected `keys`, while the other is the original `map` with the `keys` removed."
  [map keys]
  [(select-keys map keys) (apply dissoc map keys)])


(defn sum
  "Varidic function which will sum `args`. Treats nil as 0."
  [& args]
  (reduce + 0 (keep identity args)))

(defn avg
  "Varidic function which will average `args`"
  [& args]
  (/ (apply sum args) (count args)))

(defn round
  "Rounds `n` to the given `precision`"
  ([n] (round n 0))
  ([n precision]
   (let [factor (Math/pow 10 precision)]
     (/ (Math/round (* n factor))
        factor))))


(defn find-first
  "Returns the first item matching `pred` in `coll`.

  Optionally takes an `extract` function which will be applied iff the matching item is not `nil`.

  Example:

  (find-first :primary :email [{:primary false :email \"bob@gmail.com\"}
                              {:primary true :email \"foo@bar.com\"}])
  "
  ([pred coll] (find-first pred identity coll))
  ([pred extract coll]
   (when-some [item (first (filter pred coll))]
     (extract item))))

(defn indistinct
  "Returns elements in a sequence that appear more than once.

  Only returns successive elements that have been seen before.

  If called without a collection, returns a stateful transducer.

  user=> (indistinct [1 2 1 2 2 3 4 5 1])
  (1 2 2 1)"
  ([]
   (fn [rf]
     (let [seen (volatile! #{})]
       (fn
         ([] (rf))
         ([acc] (rf acc))
         ([acc item]
          (if (@seen item)
            (rf acc item)
            (do (vswap! seen conj item)
                acc)))))))
  ([coll]
   (let [seen (volatile! #{})]
     (->> coll
          (map (fn check-seen [i]
                 (if (@seen i)
                   i
                   (do (vswap! seen conj i)
                       ::drop))))
          (remove #{::drop})))))

(defn rand
  "Behaves just like `clojure.core/rand` but optionally accepts a lower bound."
  ([n] (core/rand n))
  ([low high] (+ low (core/rand (- high low)))))


(defn inspect
  "A function useful for println debugging. Prints the value (and an optional
  message) and returns it."
  ([v]
   (println v)
   v)
  ([msg v]
   (println (str msg ":") v)
   v)
  ([msg f v]
   (println (str msg ":") (f v))
   v))


(defn ns-select-keys
  "Behaves like select keys but applies a new namespace to all keys selected.
  If `new-ns` is `nil` then removes any namespace."
  ([m keys] (ns-select-keys m keys nil))
  ([m keys new-ns]
   (let [new-ns    (cond
                     (keyword? new-ns) (or (namespace new-ns)
                                           (name new-ns))
                     (string? new-ns)  new-ns)
         ->new-key (fn [[k v]] [(keyword new-ns (name k)) v])]
     (into {} (map ->new-key) m))))


(defn arg
  "Returns a function which will take any number of arguments and return
  the 1-based index argument of `n`. Mostly useful for composition.

  user=> ((a 1) 1 2 3)
  1"
  [n]
  (fn [& args]
    (nth args (dec n) nil)))


(defn dedupe-by
  "Returns a lazy sequence of the elements of coll, removing any **consecutive**
  elements that return duplicate values when passed to a function f."
  ([f]
   (fn [rf]
     (let [pv (volatile! ::none)]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result x]
          (let [prior @pv
                fx    (f x)]
            (vreset! pv fx)
            (if (= prior fx)
              result
              (rf result x))))))))
  ([f coll]
   (sequence (dedupe-by f) coll)))


(defmacro assert-args
  "assert-args lifted from clojure.core. Mostly useful for writing other macros"
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                 (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
       ~(let [more (nnext pairs)]
          (when more
            (list* `assert-args more)))))


(defmacro assert-info
  "A variant of `assert` that throws an `ex-info` based exception."
  [expr msg info]
  `(when (not ~expr)
     (throw (ex-info ~msg ~info))))