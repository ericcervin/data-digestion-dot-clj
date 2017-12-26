(ns data-digestion.philosophy-usa
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))

(def db-spec {:classname "org.sqlite.JDBC" :subprotocol "sqlite" :subname "resources/philosophy_usa/OUT/philosophy-usa.db"})

(defn drop-table! [dbname]
  (if (.exists (io/as-file "resources/philosophy_usa/OUT/philosophy-usa.db"))
      (if (> (count (sql/query db-spec [(str "Select * from sqlite_master where type = \"table\" and name = \"" dbname "\"")])) 0)
          (sql/db-do-commands db-spec
            (sql/drop-table-ddl (keyword dbname))))))

(defn create-completion-table! []
  (sql/db-do-commands db-spec
    (sql/create-table-ddl
      :completion
      [[:inst "varchar(16)"][:cip "varchar(16)"][:all_cnt :int]])))

(defn create-institution-table! []
  (sql/db-do-commands db-spec
    (sql/create-table-ddl
      :institution
      [[:unitid "varchar(16)"]
       [:instnm "varchar(128)"]
       [:addr "varchar(128)"]
       [:city "varchar(64)"]
       [:stabbr "varchar(4)"]
       [:zip "varchar(16)"]])))

(defn create-cipcode-table! []
  (sql/db-do-commands db-spec
    (sql/create-table-ddl
      :cipcode
      [[:cipcode "varchar(16)"][:ciptitle "varchar(64)"]])))


(defn load-table! [db sq] (sql/insert-multi! db-spec (keyword db) sq))
    
(defn cmpn-row-map [[inst cip _ _ _ all_cnt]] {:inst inst :cip cip :all_cnt (Integer. all_cnt)})

(defn inst-row-map [[unitid instnm addr city stabbr zip]] {:unitid unitid :instnm instnm :addr addr :city city :stabbr stabbr :zip zip})

(defn cip-row-map  [[_ cipcode ciptitle]] {:cipcode cipcode :ciptitle ciptitle})

(defn -main []
  ;;completions
  (let [completion-file (slurp "resources/philosophy_usa/in/2014_2015_Completions_CIP_38_only.csv")
        c-file-lines (clojure.string/split  completion-file #"\r\n")
        c-file-arrays (map #(clojure.string/split  % #",") c-file-lines)
        c-file-maps (map cmpn-row-map (rest c-file-arrays))]
        
    (drop-table! "completion") 
    
    (create-completion-table!)
    
    (load-table! "completion" c-file-maps)
  
    (println (sql/query db-spec ["Select Count(*) as completion_table_rows from completion"]))
      
    ;;institutions
    (let [institution-file (slurp "resources/philosophy_usa/in/Institutions_Name_and_Addr.csv")
          i-file-lines (clojure.string/split  institution-file #"\r\n")
          i-file-arrays (map #(clojure.string/split  % #",") i-file-lines)
          i-file-maps (map inst-row-map (rest i-file-arrays))]
        
     (drop-table! "institution")
    
     (create-institution-table!)
    
     (load-table! "institution" i-file-maps)
      
     (println (sql/query db-spec ["Select Count(*) as institution_table_rows from institution"])))
        
    ;;cip codes (Classification of Instructional Programs)
    (let [cipcode_file (slurp "resources/philosophy_usa/in/CIPCode2010_38_only.csv")
          cip-file-lines (clojure.string/split  cipcode_file #"\r\n")
          cip-file-arrays (map #(clojure.string/split  % #",") cip-file-lines)
          cip-file-maps (map cip-row-map (rest cip-file-arrays))]
        
     (drop-table! "cipcode")
    
     (create-cipcode-table!)
    
     (load-table! "cipcode" cip-file-maps)
    
     (println (sql/query db-spec ["Select Count(*) as cipcode_table_rows from cipcode"])))))

