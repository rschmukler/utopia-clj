(ns utopia.match
  "Extensions to `core.match`"

  (:require [clojure.core.match :refer [match]]))

(defmacro match?
  "Returns whether the provided `expr` matches the given `match-pattern`."
  [expr match-pattern]
  `(match ~expr
     ~match-pattern true
     :else false))
