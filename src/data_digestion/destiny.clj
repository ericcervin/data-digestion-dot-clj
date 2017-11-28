(ns data-digestion.destiny  
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]))

(defn export-card-tsv [file mp]
  (with-open [writer (io/writer file)]
    (doseq [i mp]
      (.write writer (str (get i "set_name") "\t" (str (get i "position") "\t" (get i "affiliation_name") "\t" (get i "faction_name") "\t" (get i "name") "\t" (get i "type_name") "\n"))))))
        

(defn -main []
  (let [card-json (slurp "https://swdestinydb.com/api/public/cards/")
        card-map  (json/read-str card-json)
        villain-command-cards (filter #(and (= (get % "affiliation_name") "Villain") (= (get % "faction_name") "Command")) card-map)]
    
    ;;export all json
    (spit "resources/all_cards.json" card-json)
    
    ;;write counts of subsets (can compare with totals documented on Destiny wiki)
    (println (map #(str (first %) " " (count (last %))) (group-by #(get % "set_name") card-map)))
    
    ;;write reports
    (export-card-tsv "resources/card_list_all.tsv" card-map)
    (export-card-tsv "resources/card_list_villian_command.tsv" villain-command-cards)
    

    ))
        
        

