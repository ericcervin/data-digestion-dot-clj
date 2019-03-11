(ns data-digestion.magic  
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))


(defn set-subtype-counts [st]
  (let [set-cards (get st "cards")
        set-subtype-vectors (mapv #(get % "subtypes") set-cards)
        set-subtypes (reduce into [] set-subtype-vectors)
        set-subtype-counts (frequencies set-subtypes)]
    set-subtype-counts))


(defn -main []
  (let [set-json (slurp "resources/magic/IN/Standard.json")
        set-map  (json/read-str set-json)
        set-keys (keys set-map)
        set-card-counts (mapv #(hash-map :set % :card-count (get (set-map %) "totalSetSize")) set-keys)]
  
    ;;export all json
   ;;(spit "resources/magic/OUT/standard_sets.json" set-json)
  
  
   (with-open [writer (io/writer "resources/magic/OUT/set_subtype_counts.tsv")]
     (.write writer "Set\tSubtype\tCount\n")
     (doseq [st set-keys]
        (doseq [cnt (set-subtype-counts (set-map st))]
           (.write writer (str st "\t" (first cnt) "\t" (second cnt) "\n")))))))
  
