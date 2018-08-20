(ns data-digestion.destiny-every-team  
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))


(defn character-query [] (let [db-spec {:classname "org.sqlite.JDBC" :subprotocol "sqlite" :subname "resources/destiny/OUT/destiny.db"}
                               qry "Select cardsetcode, position, isunique, cardcode, name, factioncode, affiliation, cminpoints, cmaxpoints
                                    from card 
                                    where typename = \"Character\" "
                               results (sql/query db-spec [qry] {:as-arrays? false})]
                           results))

(defn new-team [c, dice]
  (let [short-team-name (clojure.string/replace (:name c) #" " "")
        team-name (if (= dice 2)  (str "e" short-team-name) short-team-name)
        team-affiliation (:affiliation c)
        team-points (if (= dice 2) (:cmaxpoints c) (:cminpoints c))]
    {:team-name team-name 
     :team-affiliation team-affiliation
     :team-points team-points}))


(defn -main []
  (let [all-chars (character-query)
        unique-chars (filter #(= (:isunique %) 1)  all-chars)
        one-dice-teams (mapv #(new-team % 1) all-chars)
        two-dice-teams (mapv #(new-team % 2) unique-chars)
        all-teams (concat one-dice-teams two-dice-teams)]
        
        
    ;;103 unique. 33 nonunique. 239 one char teams     
   ;;(println (str (count all-chars) " " (count unique-chars))))) 
   (println all-teams)))
  
