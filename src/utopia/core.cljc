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

(defn extract
  "Returns a tuple of a vector of the vals of `keys` and `map` with `keys` removed.

  Example:

  (extract {:name \"Bob\" :age 42 :weight 200} [:age :weight])
  ;; [[42 200] {:name \"Bob\"}]"
  [map keys]
  (loop [keys    keys
         vals    (transient [])
         new-map (transient map)]
    (if (seq keys)
      (let [k (first keys)
            v (get map k)]
        (recur (rest keys) (conj! vals v) (dissoc! new-map k)))
      [(persistent! vals) (persistent! new-map)])))

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

(defn map-keys
  "Returns a transducer for mapping `f` over all keys in a map-entry.

  If called with `map`, returns a new map with `f` applied over all keys."
  ([f] (core/map (fn [[k v]] [(f k) v])))
  ([f map] (when map (into {} (map-keys f) map))))

(defn map-vals
  "Returns a transducer for mapping `f` over all values in a map-entry.

  If called with `map`, returns a new map with `f` applied over all values."
  ([f] (core/map (fn [[k v]] [k (f v)])))
  ([f map] (when map (into {} (map-vals f) map))))

(defn map-leaves
  "Returns a transducer for mapping `f` over all leaf values in a map-entry. Nested maps will be
  traversed.

  If called with `map` returns a new map with `f` applied over all leaves. "
  ([f] (core/map (fn [[k v]] [k (if (map? v)
                                  (map-leaves f v)
                                  (f v))])))
  ([f map] (when map (into {} (map-leaves f) map))))

(defn remove-vals
  "Return a transducer which will only match map-entries for which the
  `pred` called on values returned logical `false`.

  If called with `map`, will return a new map executing the transducer."
  ([f] (remove (fn [[_ v]] (f v))))
  ([f map] (when map (into {} (remove-vals f) map))))

(defn filter-vals
  "Return a transducer which will only match map-entries for which the
  `pred` called on values returned logical `true`.

  If called with `map`, will return a new map executing the transducer."
  ([f] (filter (fn [[_ v]] (f v))))
  ([f map] (when map (into {} (filter-vals f) map))))

(defn remove-keys
  "Return a transducer which will only match map-entries for which the
  `pred` called on keys returned logical `false`.

  If called with `map`, will return a new map executing the transducer."
  ([f] (remove (fn [[k _]] (f k))))
  ([f map] (when map (into {} (remove-keys f) map))))

(defn filter-keys
  "Return a transducer which will only match map-entries for which the
  `pred` called on keys returned logical `true`.

  If called with `map`, will return a new map executing the transducer."
  ([f] (filter (fn [[k _]] (f k))))
  ([f map] (when map (into {} (filter-keys f) map))))

(defn ns-keys
  "Returns a transducer which will namespace all keys in a map.

  If called with `map` returns a new map executing the transducer."
  ([ns] (map-keys (comp (partial keyword ns) name)))
  ([ns map] (when map (into {} (ns-keys ns) map))))

(defn assoc-some
  "Like `clojure.core/assoc` but only associates the key and value if the `val` is `some?`."
  [m & {:as kvs}]
  (reduce-kv
   assoc
   m
   (filter-vals some? kvs)))


(defn fex
  "Similar `clojure.core/fnil`. Returns a function which will call `f` when called
  and return `ex-val` in the case that any exception occurs."
  [f ex-val]
  (fn [& args]
    (try (apply f args)
         (catch Exception _
           ex-val))))

(defn find-paths
  "Returns a sequence of vector paths for which `pred` returns true.

  For nested maps, it will be called on the `pred` will be called with the `val` of the map entry.

  Note that this performs a pre-walk and in the case of nested maps or vectors, `pred` will be
  called with them before traversing their children."
  [pred coll]
  (let [results (transient [])
        find-paths*
        (fn find-paths* [path coll]
          (cond
           (map? coll)       (doall (map #(find-paths* path %) coll))
           (map-entry? coll) (if (pred (val coll))
                               (conj! results (conj path (key coll)))
                               (find-paths* (conj path (key coll)) (val coll)))
           (vector? coll)    (doall (map-indexed #(find-paths* (conj path %1) %2) coll))
           (pred coll)       (conj! results path)
           :else             nil))]
    (find-paths* [] coll)
    (persistent! results)))
