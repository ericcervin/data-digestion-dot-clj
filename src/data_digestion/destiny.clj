(ns data-digestion.destiny  
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))

(def db-spec {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname "resources/destiny.db"})

(defn drop-card-table! []
  (sql/db-do-commands db-spec
   (sql/drop-table-ddl :cards)))

(defn create-card-table! []
  (sql/db-do-commands db-spec
    (sql/create-table-ddl
      :cards
      [[:cardSet "varchar(64)"]
       [:position :int]
       [:affiliation "varchar(64)"]
       [:faction "varchar(64)"]
       [:name "varchar (64)"]
       [:typeName "varchar(64)"]
       [:rarity "varchar(64)"]])))

(defn load-card-table! [mp]
  (sql/insert-multi! db-spec :cards
     (map #(hash-map :cardSet (get % "set_name") :position (get % "position") 
                     :affiliation (get % "affiliation_name") :faction (get % "faction_name")
                     :name (get % "name") :typeName (get % "type_name") :rarity (get % "rarity")) mp)))
    
  

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
    
    ;;some more subtotals
    (println (map #(str (first %) "\t" (count (last %)) "\n") (group-by #(str (get % "affiliation_name") "-"(get % "faction_name")) card-map)))
    
    ;;write reports
    (export-card-tsv "resources/card_list_all.tsv" card-map)
    (export-card-tsv "resources/card_list_villian_command.tsv" villain-command-cards)
    (export-card-tsv "resources/card_list_villian_command_compatible.tsv" villain-command-compatible-cards)
    
    
    ;;drop card table
    (drop-card-table!)
    
    ;;create card table
    (create-card-table!)
    
    ;;insert rows
    (load-card-table! card-map)))
    
    ;;print totals again querying database
    ;;(println (sql/query db-spec ["Select Count(*), affiliation, faction from cards group by affiliation, faction"]))))

    

    
        
        

