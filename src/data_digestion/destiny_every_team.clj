;;add code to not get PhasmaYoda and YodaPhasma -- only do the combination if they are in alpha order
;;virtual team members so not multiplevaders
;;add to readme dot md
;;add function that checks that every set of teams is within a range of cost values

(ns data-digestion.destiny-every-team  
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))


(def char-nicknames 
          {"01001" {:nickname "Phasma1" :dupes ["01001" "04002"]} 
           "01003" {:nickname "Grievous1" :dupes ["01003" "07021"]} 
           "01009" {:nickname "Dooku1" :dupes ["01009" "07001"]} 
           "01010" {:nickname "Vader1" :dupes ["01010" "02010" "06001" "07088"]} 
           "01011" {:nickname "Kylo1" :dupes ["01011" "04001"]} 
           "01020" {:nickname "Jabba1" :dupes ["01020" "07036"]} 
           "01028" {:nickname "Leia1" :dupes ["01028" "07073"]}
           "01029" {:nickname "Poe1" :dupes ["01029" "04025"]}
           "01035" {:nickname "Luke1" :dupes ["01035" "05031" "07056"]}
           "01038" {:nickname "Rey1" :dupes ["01038" "04024"]}
           "01045" {:nickname "Finn1" :dupes ["01045" "05039"]}
           "01046" {:nickname "Han1" :dupes ["01046" "05046" "08134"]}
           "02010" {:nickname "Vader2" :dupes ["01010" "02010" "06001" "07088"]}
           "02011" {:nickname "Palpatine1" :dupes ["02011" "05004"]}
           "02037" {:nickname "Obi-Wan1" :dupes ["02037" "05032"]}
           "03038" {:nickname "Ezra1" :dupes ["03038" "07054"]}
           "04001" {:nickname "Kylo2" :dupes ["01011" "04001"]}
           "04002" {:nickname "Phasma2" :dupes ["01001" "04002"]}
           "04024" {:nickname "Rey2" :dupes ["01038" "04024"]}
           "04025" {:nickname "Poe2" :dupes ["01029" "04025"]}
           "05004" {:nickname "Palpatine2" :dupes ["02011" "05004"]}
           "05031" {:nickname "Luke2" :dupes ["01035" "05031" "07056"]}
           "05032" {:nickname "Obi-Wan2" :dupes ["02037" "05032"]}
           "05039" {:nickname "Finn2" :dupes ["01045" "05039"]}
           "05046" {:nickname "Han2" :dupes ["01046" "05046" "08134"]}
           "06001" {:nickname "Anakin1" :dupes ["01010" "02010" "06001" "07088"]}
           "07001" {:nickname "Dooku2" :dupes ["01009" "07001"]}
           "07021" {:nickname "Grievous2" :dupes ["01003" "07021"]}
           "07036" {:nickname "Jabba2" :dupes ["01020" "07036"]}
           "07054" {:nickname "Ezra2" :dupes ["03038" "07054"]}
           "07056" {:nickname "Luke3" :dupes ["01035" "05031" "07056"]}
           "07073" {:nickname "Leia2" :dupes ["01028" "07073"]}
           "07088" {:nickname "Anakin2" :dupes ["01010" "02010" "06001" "07088"]}
           "08134" {:nickname "Han3" :dupes ["01046" "05046" "08134"]}})

(defn character-query [] (let [db-spec {:classname "org.sqlite.JDBC" :subprotocol "sqlite" :subname "resources/destiny/OUT/destiny.db"}
                               qry "Select cardsetcode, position, isunique, cardcode, name, factioncode, affiliation, cminpoints, cmaxpoints
                                    from card 
                                    where typename = \"Character\" "
                               results (sql/query db-spec [qry] {:as-arrays? false})]
                           results))


(defn char-nickname [c]
  (let [char-code (str (:cardcode c))
        name (:name c)]
       (get-in char-nicknames [char-code :nickname] name)))


(defn new-char [c,dice]
  (let [name (char-nickname c)
        short-char-name (clojure.string/replace name #" " "")
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
  (let [team-name (str (:team-name t) "_" (:char-name c))
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
   (println (take 5 (map :team-name two-char-teams)))))
  
