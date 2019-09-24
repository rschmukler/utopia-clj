(ns utopia.time
  "Time extensions to utopia"
  (:require [java-time :as time]))


(defn divide
  "Returns the result of dividing duration `a` by `b`."
  [a b]
  (/ (time/as a :millis)
     (time/as b :millis)))


(defn divisible?
  "Returns whether duration `a` is evenly divisible by `b`.

  Does not support units smaller than `:millis`"
  [a b]
  (let [a-millis (time/as a :millis)
        b-millis (time/as b :millis)]
    (= 0 (mod a-millis b-millis))))


(defn round-to
  "Returns `time` rounded down to an evenly divisible multiple of `duration`.

  Automatically truncates to milliseconds."
  [time duration]
  (let [t-millis (time/to-millis-from-epoch time)
        d-millis (time/as duration :millis)
        diff     (mod t-millis d-millis)]
    (-> time
        (time/minus (time/millis diff))
        (time/truncate-to :millis))))


(defn on-or-after?
  "Returns whether `a` is equal to or after `b`.

  An inclusive version of `java-time/after?`."
  [a b]
  (or (= a b)
      (time/after? a b)))

(defn on-or-before?
  "Returns whether `a` is equal to or before `b`.

  An inclusive version of `java-time/before?`."
  [a b]
  (or (= a b)
      (time/before? a b)))
