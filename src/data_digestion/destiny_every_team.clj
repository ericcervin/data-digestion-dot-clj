;;add code to not get PhasmaYoda and YodaPhasma

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

(defn new-char [c,dice]
  (let [short-char-name (clojure.string/replace (:name c) #" " "")
        char-name (if (= dice 2)  (str "e" short-char-name) short-char-name)
        char-affiliation (:affiliation c)
        char-points (if (= dice 2) (:cmaxpoints c) (:cminpoints c))
        char-code (:cardcode c)
        char-uniq (:isunique c)]
    {:char-name char-name 
     :char-affiliation char-affiliation
     :char-points char-points
     :char-code char-code
     :char-uniq char-uniq}))


(defn new-team [c]
  (let [team-name (:char-name c)
        team-affiliation (:char-affiliation c)
        team-points (:char-points c)
        team-mems [(:char-code c)]]
   {:team-name team-name 
    :team-affiliation team-affiliation
    :team-points team-points
    :team-mems team-mems}))


(defn affiliation-compatible? [t c]
   (let [team-affil (:team-affiliation t)
         char-affil (:char-affiliation c)]
      (or (= team-affil char-affil)
          (= team-affil "Neutral")
          (= char-affil "Neutral"))))

(defn unique-to-team? [t c]
  (let [char-uniq (:char-uniq c)
        char-code (:char-code c)
        team-mems (:team-mems t)]
    (or (and (= char-uniq 1) (= 0 (count (some #{char-code} team-mems))))
        (= char-uniq 0))))


      
(defn valid-new-mem? [t c]
  (and (unique-to-team? t c)
       (affiliation-compatible? t c)))
       

(defn add-char-to-team [t,c]
  (let [team-name (str (:team-name t) (:char-name c))
        old-team-affiliation (:team-affiliation t)
        team-affiliation (if (=  old-team-affiliation "Neutral") 
                             (:char-affiliation c)
                             old-team-affiliation)
        team-points (+ (:team-points t) (:char-points c))
        team-mems (conj (:team-mems t) (:char-code c))]
   {:team-name team-name 
    :team-affiliation team-affiliation
    :team-points team-points
    :team-mems team-mems}))


(defn -main []
  (let [chars (character-query)
        unique-chars (filter #(= (:isunique %) 1)  chars)
        one-dice-chars (mapv #(new-char % 1) chars)
        two-dice-chars (mapv #(new-char % 2) unique-chars)
        all-chars (concat one-dice-chars two-dice-chars)
        
        one-char-teams (map new-team all-chars)
        two-char-teams (for [t one-char-teams c all-chars :when (valid-new-mem? t c)]
                            (add-char-to-team t c))]
  
  ;;103 unique. 33 nonunique. 239 one char teams     
   (println (str (count all-chars) " chars " 
                 (count unique-chars) " unique chars " 
                 (count one-char-teams) " one char teams "
                 (count two-char-teams) " two char teams ")) 
   (println (take 500 (map :team-name two-char-teams)))))
  
