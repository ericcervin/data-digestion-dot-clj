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
           (.write writer (str st "\t" (first cnt) "\t" (second cnt) "\n")))))
     
   (doseq [st set-keys]
     (let [set (set-map st)
           cards (get set "cards")
           card-count (count cards)]
       (println (str st "\t" card-count))))
     
   (with-open [writer (io/writer "resources/magic/OUT/green_cards.tsv")]
     (.write writer "set date\tset code\tcard name\tmana cost\tcolors\ttypes\tsubtypes\trarity\n")
     (doseq [st set-keys]
       (let [set (set-map st)
             set-rel-date (get set "releaseDate")
             cards (get set "cards")
             green-cards (filter #(clojure.string/index-of (get % "colors") \G) cards)]
             
             
           (doseq [card green-cards]
             (let [nme (get card "name")
                   clrs (get card "colors")
                   types (get card "types")
                   subtypes (get card "subtypes")
                   mana-cost (get card "convertedManaCost")
                   rarity (get card "rarity")]
              (.write writer (str set-rel-date "\t" st "\t" nme "\t" mana-cost "\t" clrs "\t" types "\t" subtypes "\t" rarity "\n")))))))))
  
