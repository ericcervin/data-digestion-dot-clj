(ns data-digestion.destiny  
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))

(def db-spec {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname "resources/destiny/OUT/destiny.db"})

(defn assoc-extended-points-fields [mp]
   (let [c-points (get mp "points")
         c-points (if (nil? c-points) "" c-points)
         c-1d-points (get (clojure.string/split c-points #"/") 0)
         c-2d-points (get (clojure.string/split c-points #"/") 1 "")
         c-min-points c-1d-points
         c-max-points (if (> (count c-2d-points) 0)
                        c-2d-points
                        c-1d-points)]
     (merge mp {:c-points c-points :c-1d-points c-1d-points :c-2d-points c-2d-points :c-min-points c-min-points :c-max-points c-max-points})))
  

(defn drop-card-table! []
  (if (.exists (io/as-file "resources/destiny/OUT/destiny.db"))
      (if (> (count (sql/query db-spec ["Select * from sqlite_master where type = \"table\" and name = \"card\""])) 0)
          (sql/db-do-commands db-spec
            (sql/drop-table-ddl :card)))))

(defn create-card-table! []
  (sql/db-do-commands db-spec
    (sql/create-table-ddl
      :card
      [[:cardSet "varchar(64)"]
       [:position :int]
       [:cardCode "varchar(8)"]
       [:affiliation "varchar(64)"]
       [:faction "varchar(64)"]
       [:name "varchar (64)"]
       [:c1dPoints :int] 
       [:c2dPoints :int]
       [:cMinPoints :int]
       [:cMaxPoints :int]
       [:typeName "varchar(64)"]
       [:rarity "varchar(64)"]
       [:imgSrc "varchar(64)"]])))

(defn load-card-table! [mp]
  (sql/insert-multi! db-spec :card
     (map #(hash-map :cardSet (get % "set_name") :position (get % "position") :cardCode (get % "code")
                     :affiliation (get % "affiliation_name") :faction (get % "faction_name")
                     :name (get % "name")  :c1dPoints (:c-1d-points %) :c2dPoints (:c-2d-points %) 
                     :cMinPoints (:c-min-points %) :cMaxPoints (:c-max-points %)
                     :typeName (get % "type_name") :rarity (get % "rarity_name") :imgSrc (get % "imagesrc")) mp)))
    
  

(defn export-card-tsv [file mp]
  (with-open [writer (io/writer file)]
    (.write writer (str (clojure.string/join "\t" ["Set" "Position" "Code" "Affiliation" "Faction" "Name" "Type" "1d Cost" "2d Cost" "Min Cost" "Max Cost" "Rarity" "Img Src"]) "\n"))
    (doseq [i mp]
      (let [c-set (get i "set_name")
            c-position (get i "position")
            c-code (get i "code")
            c-affiliation (get i "affiliation_name")
            c-faction (get i "faction_name")
            c-name (get i "name") 
            c-type (get i "type_name")
            c-points (:c-points i)
            c-1d-points (:c-1d-points i)
            c-2d-points (:c-2d-points i)
            c-min-points (:c-min-points i)
            c-max-points (:c-max-points i)
            c-rarity (get i "rarity_name")
            c-image-src (get i "imagesrc")]
        (.write writer (str (clojure.string/join "\t" [c-set c-position c-code c-affiliation c-faction c-name c-type c-1d-points c-2d-points c-min-points c-max-points c-rarity c-image-src]) "\n"))))))
        

(defn -main []
  (let [card-json (slurp "https://swdestinydb.com/api/public/cards/")
        card-map  (json/read-str card-json)
        card-map  (map assoc-extended-points-fields card-map)
        villain-command-cards (filter #(and (= (get % "affiliation_name") "Villain") (= (get % "faction_name") "Command")) card-map)
        villain-command-compatible-cards (filter #(or (and (= (get % "affiliation_name") "Villain") (= (get % "faction_name") "General"))
                                                      (and (= (get % "affiliation_name") "Neutral") (= (get % "faction_name") "General") (not (= (get % "type_name") "Battlefield")))
                                                      (and (= (get % "affiliation_name") "Neutral") (= (get % "faction_name") "Command"))) card-map)]
    
   
    
    ;;export all json
    (spit "resources/destiny/OUT/all_cards.json" card-json)
    
    ;;write counts of subsets (can compare with totals documented on Destiny wiki)
    (println (map #(str (first %) " " (count (last %))) (group-by #(get % "set_name") card-map)))
    
    ;;some more subtotals
    (println (map #(str (first %) "\t" (count (last %)) "\n") (group-by #(str (get % "affiliation_name") "-"(get % "faction_name")) card-map)))
    
    ;;write reports
    (export-card-tsv "resources/destiny/OUT/card_list_all.tsv" card-map)
    (export-card-tsv "resources/destiny/OUT/card_list_villian_command.tsv" villain-command-cards)
    (export-card-tsv "resources/destiny/OUT/card_list_villian_command_compatible.tsv" villain-command-compatible-cards)
    
    
    ;;drop card table
    (drop-card-table!)
    
    ;;create card table in mysql
    (create-card-table!)
    
    ;;insert rows in mysql
    (load-card-table! card-map)))
    
    ;;print totals again querying mysql database
    ;;(println (sql/query db-spec ["Select Count(*), affiliation, faction from card group by affiliation, faction"]))))


