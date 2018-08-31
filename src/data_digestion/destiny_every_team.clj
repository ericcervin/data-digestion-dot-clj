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


(def force-balance
    {"05029" 1
     "04002" 1
     "03040" 1
     "02021" 1
     "02002" 2
     "01029" 2})


(defn character-query [] (let [db-spec {:classname "org.sqlite.JDBC" :subprotocol "sqlite" :subname "resources/destiny/OUT/destiny.db"}
                               qry "Select cardsetcode, position, isunique, cardcode, name, 
                                    factioncode, affiliation, cminpoints, cmaxpoints, cHealth
                                    from card 
                                    where typename = \"Character\" "
                               results (sql/query db-spec [qry] {:as-arrays? false})]
                           results))


(defn char-nickname [c]
  (let [char-code (str (:cardcode c))
        name (:name c)]
       (get-in char-nicknames [char-code :nickname] name)))


(defn new-char [c,dice]
  (let [char-code (:cardcode c)
        char-aliases (get-in char-nicknames [char-code :dupes] [char-code])
        name (char-nickname c)
        short-char-name (clojure.string/replace name #" " "")
        char-name (if (= dice 2)  (str "E" short-char-name) short-char-name)
        char-affiliation (:affiliation c)
        char-faction (:factioncode c)
        char-points (if (= dice 2) (:cmaxpoints c) (:cminpoints c))
        f-bal (get force-balance char-code 0)
        char-points (+ char-points f-bal)
        char-uniq (:isunique c)
        char-health (:chealth c)]
    {:char-name char-name 
     :char-aliases char-aliases
     :char-affiliation char-affiliation
     :char-faction char-faction
     :char-points char-points
     :char-code char-code
     :char-uniq char-uniq
     :char-health char-health}))


(defn new-team [c]
  (let [team-name (:char-name c)
        team-aliases (:char-aliases c)
        team-affiliation (:char-affiliation c)
        team-faction (conj #{} (:char-faction c))
        team-faction-count (count team-faction)
        team-points (:char-points c)
        team-mems [(:char-code c)]
        team-health (:char-health c)]
   {:team-name team-name 
    :team-aliases team-aliases
    :team-affiliation team-affiliation
    :team-faction team-faction
    :team-faction-count team-faction-count
    :team-points team-points
    :team-mems team-mems
    :team-health team-health}))




(defn affiliation-compatible? [t c]
   (let [team-affil (:team-affiliation t)
         char-affil (:char-affiliation c)]
      (or (= team-affil char-affil)
          (= team-affil "Neutral")
          (= char-affil "Neutral"))))

(defn unique-to-team? [t c]
  (let [char-uniq (:char-uniq c)
        char-code (:char-code c)
        team-mems (:team-mems t)
        team-aliases (:team-aliases t)]
    (or (and (= char-uniq 1) (= 0 (count (some #{char-code} team-mems))) (= 0 (count (some #{char-code} team-aliases))))
        (= char-uniq 0))))
      
(defn valid-new-mem? [t c]
  (and (unique-to-team? t c)
       (affiliation-compatible? t c)))
       
(defn within-point-limit? [t c low high]
  (let [team-points (:team-points t)
        char-points (:char-points c)
        total-points (+ team-points char-points)]
     (and (>= total-points low) (<= total-points high))))
  
  

(defn add-char-to-team [t,c]
  (let [old-team-name (:team-name t)
        char-name (:char-name c)
        char-aliases (:char-aliases c)
        split-old-team-name (clojure.string/split old-team-name #"_")
        all-names (conj split-old-team-name char-name)
        team-name (apply str (interpose "_" (sort all-names)))
        old-team-aliases (:team-aliases t)
        team-aliases (concat old-team-aliases char-aliases)
        old-team-affiliation (:team-affiliation t)
        team-affiliation (if (=  old-team-affiliation "Neutral") 
                             (:char-affiliation c)
                             old-team-affiliation)
        team-faction (conj (:team-faction t) (:char-faction c))
        team-faction-count (count team-faction)
        team-points (+ (:team-points t) (:char-points c))
        team-mems (conj (:team-mems t) (:char-code c))
        team-health (+ (:team-health t) (:char-health c))]
   {:team-name team-name 
    :team-aliases team-aliases
    :team-affiliation team-affiliation
    :team-faction team-faction
    :team-faction-count team-faction-count
    :team-points team-points
    :team-mems team-mems
    :team-health team-health}))

(defn dedupe-teams [tms]
  (let [unique-team-map (reduce #(assoc %1 (:team-name %2) %2) {} tms)]
    (sort-by :team-name (vals unique-team-map))))
   
(defn export-team-tsv [file mp]
  (with-open [writer (io/writer file)]
    (.write writer (str (clojure.string/join "\t" ["Team" "Affiliation" "Faction Count" "Affiliation/Faction Count" "Factions" "Member Count" "Health" "Points"]) "\n"))
    (doseq [i mp]
      (let [{team-name :team-name team-affiliation :team-affiliation team-faction-count :team-faction-count
             team-faction :team-faction team-health :team-health team-mems :team-mems team-points :team-points} i
            team-faction (clojure.string/join "_" (sort team-faction))
            team-member-count (count team-mems)
            team_affiliation_faction_count (str team-affiliation "_" team-faction-count)] 
        (.write writer (str (clojure.string/join "\t" [team-name team-affiliation team-faction-count team_affiliation_faction_count team-faction team-member-count team-health team-points]) "\n"))))))  

(defn -main []
  (let [chars (character-query)
        unique-chars (filter #(= (:isunique %) 1)  chars)
        one-dice-chars (mapv #(new-char % 1) chars)
        two-dice-chars (mapv #(new-char % 2) unique-chars)
        all-chars (concat one-dice-chars two-dice-chars)
        
        one-char-teams (map new-team all-chars)
        two-char-teams (for [t one-char-teams c all-chars :when (and (valid-new-mem? t c) (within-point-limit? t c 0 30))]
                         (add-char-to-team t c))
        unique-two-char-teams (dedupe-teams two-char-teams)
        three-char-teams (for [t unique-two-char-teams c all-chars :when (and (valid-new-mem? t c) (within-point-limit? t c 0 30))]
                           (add-char-to-team t c))
        unique-three-char-teams (dedupe-teams three-char-teams)
        four-char-teams (for [t unique-three-char-teams c all-chars :when (and (valid-new-mem? t c) (within-point-limit? t c 0 30))]
                          (add-char-to-team t c))
        unique-four-char-teams (dedupe-teams four-char-teams)
        five-char-teams (for [t unique-four-char-teams c all-chars :when (and (valid-new-mem? t c) (within-point-limit? t c 0 30))]
                          (add-char-to-team t c))
        unique-five-char-teams (dedupe-teams five-char-teams)
        all-unique-teams (concat one-char-teams unique-two-char-teams unique-three-char-teams unique-four-char-teams unique-five-char-teams)
        teams-26-to-30-points (filter #(and (>= (:team-points %) 26) (<= (:team-points %) 30)) all-unique-teams)]
    
    ;;103 unique. 33 nonunique. 239 one char teams     
     (println (str 
                ;;(count all-chars) " chars \n" 
                ;;(count unique-chars) " unique chars \n" 
                ;;(count one-char-teams) " one char teams \n"
                ;;(count two-char-teams) " two char teams \n"
                ;;(count unique-two-char-teams) " unique two char teams \n"
                ;;(count three-char-teams) " three char teams \n"
                ;;(count unique-three-char-teams) " unique three char teams \n"
                ;;(count four-char-teams) " four char teams \n"
                ;;(count unique-four-char-teams) " unique four char teams \n"
                ;;(count five-char-teams) " five char teams \n"
                ;;(count unique-five-char-teams) " unique five char teams \n"
                ;;(count all-unique-teams) " unique teams \n"
                (count teams-26-to-30-points) " 26 to 30 point teams\n"))
    (export-team-tsv "resources/destiny/OUT/every_team_26_to_30_pts.tsv" teams-26-to-30-points)))  
    
    ;;(println (filter #(= "Anakin1_Anakin2_CassianAndor" (:team-name %)) teams-26-to-30-points))))
  
