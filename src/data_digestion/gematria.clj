(ns data-digestion.gematria
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))

(def db-spec {:classname "org.sqlite.JDBC" :subprotocol "sqlite" :subname "resources/gematria/OUT/gematria.db"})

(defn drop-table! [tbname]
  (if (.exists (io/as-file (:subname db-spec)))
      (if (> (count (sql/query db-spec [(str "Select * from sqlite_master where type = \"table\" and name = \"" tbname "\"")])) 0)
          (sql/db-do-commands db-spec
            (sql/drop-table-ddl (keyword tbname))))))

(defn create-gematria-table! []
  (sql/db-do-commands db-spec
    (sql/create-table-ddl
      :gematria
      [[:word "varchar(32)"][:wordlen :int][:wordvalue :int]])))


(defn load-table! [tb sq] (sql/insert-multi! db-spec (keyword tb) sq))
    
(defn calculate-word-value [wrd]
  (let [word wrd
        lc-word (clojure.string/lower-case wrd)
        values (map #(- (int %) 96) lc-word)
        total-value (apply + values)]
     total-value))

(defn wf-row-map  [wrd] {:word wrd :wordlen (count wrd) :wordvalue (calculate-word-value wrd)})

(defn -main []
  ;;completions
  (let [wordfreq-file (slurp "resources/gematria/IN/google-10000-english.txt")
        wf-file-lines (clojure.string/split  wordfreq-file #"\n")
        wf-file-maps (map wf-row-map wf-file-lines)]
        
    (drop-table! "gematria") 
    
    (create-gematria-table!)
    
    (load-table! "gematria" wf-file-maps)
  
    (println (sql/query db-spec ["Select Count(*) as gematria_table_rows from gematria"]))
    
    (println (sql/query db-spec ["Select max(wordlen) as gematria_longest_word from gematria"]))
    
    (println (sql/query db-spec ["Select max(wordvalue) as gematria_top_word_value from gematria"]))
      
    (println (sql/query db-spec ["Select * from gematria
                                 where wordvalue > 200
                                 order by wordvalue DESC"]))))
      
    
      
   
    
   
    
      
      
      
