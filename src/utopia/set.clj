(ns utopia.set
  "Namespace providing set-like extensions for Utopia"
  (:require [clojure.core :as core])
  (:refer-clojure :exclude [map]))


(defn map-keys
  "Returns a transducer for mapping `f` over all keys in a map-entry.

  If called with `map`, returns a new map with `f` applied over all keys."
  ([f] (core/map (fn [[k v]] [(f k) v])))
  ([f map] (into {} (map-keys f) map)))

(defn map-values
  "Returns a transducer for mapping `f` over all values in a map-entry.

  If called with `map`, returns a new map with `f` applied over all values."
  ([f] (core/map (fn [[k v]] [k (f v)])))
  ([f map] (into {} (map-values f) map)))

(defn remove-values
  "Return a transducer which will only match map-entries for which the
  `pred` called on values returned logical `false`.

  If called with `map`, will return a new map executing the transducer."
  ([f] (remove (fn [[_ v]] (f v))))
  ([f map] (into {} (remove-values f) map)))

(defn namespace-keys
  "Returns a transducer which will namespace all keys in a map.

  If called with `map` returns a new map executing the transducer."
  ([ns] (map-keys (comp (partial keyword ns) name)))
  ([ns map] (into {} (namespace-keys ns) map)))
