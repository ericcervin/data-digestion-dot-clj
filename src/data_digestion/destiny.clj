(ns data-digestion.destiny  
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]))

(defn export-card-tsv [file mp]
  (with-open [writer (io/writer file)]
    (.write writer (str (clojure.string/join "\t" ["Set" "Position" "Affiliation" "Faction" "Name" "Type" "1d Points" "2d Points""Rarity"]) "\n"))
    (doseq [i mp]
      (let [c-set (get i "set_name")
            c-position (get i "position")
            c-affiliation (get i "affiliation_name")
            c-faction (get i "faction_name")
            c-name (get i "name") 
            c-type (get i "type_name")
            c-points (get i "points")
            c-points (if (nil? c-points) "" c-points)
            c-1d-points (get (clojure.string/split c-points #"/") 0)
            c-2d-points (get (clojure.string/split c-points #"/") 1)
            c-rarity (get i "rarity_name")]
        (.write writer (str (clojure.string/join "\t" [c-set c-position c-affiliation c-faction c-name c-type c-1d-points c-2d-points c-rarity]) "\n"))))))
        

(defn -main []
  (let [card-json (slurp "https://swdestinydb.com/api/public/cards/")
        card-map  (json/read-str card-json)
        villain-command-cards (filter #(and (= (get % "affiliation_name") "Villain") (= (get % "faction_name") "Command")) card-map)
        villain-command-compatible-cards (filter #(or (and (= (get % "affiliation_name") "Villain") (= (get % "faction_name") "General"))
                                                      (and (= (get % "affiliation_name") "Neutral") (= (get % "faction_name") "General") (not (= (get % "type_name") "Battlefield")))
                                                      (and (= (get % "affiliation_name") "Neutral") (= (get % "faction_name") "Command"))) card-map)]
    
    ;;export all json
    (spit "resources/all_cards.json" card-json)
    
    ;;write counts of subsets (can compare with totals documented on Destiny wiki)
    (println (map #(str (first %) " " (count (last %))) (group-by #(get % "set_name") card-map)))
    
    ;;write reports
    (export-card-tsv "resources/card_list_all.tsv" card-map)
    (export-card-tsv "resources/card_list_villian_command.tsv" villain-command-cards)
    (export-card-tsv "resources/card_list_villian_command_compatible.tsv" villain-command-compatible-cards)))
    

    
        
        

