(ns data-digestion.wh_champions
  (:require [qbits.spandex :as sp]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))


(def db-spec {:classname "org.sqlite.JDBC" :subprotocol "sqlite" :subname "resources/wh_champions/OUT/wh_champions.db"})

(defn create-card-table! []
  (sql/db-do-commands db-spec
    (sql/create-table-ddl
      :card
      [[:setNum :int]
       [:setName "varchar(64)"]
       [:cardNumber :int]
       [:alliance "varchar(64)"]
       [:category "varchar(64)"]
       [:class "varchar(64)"]
       [:name "varchar(64)"]
       [:rarity "varchar(64)"]])))
       

(defn drop-table! [tbname]
  (if (.exists (io/as-file (:subname db-spec)))
      (if (> (count (sql/query db-spec [(str "Select * from sqlite_master where type = \"table\" and name = \"" tbname "\"")])) 0)
          (sql/db-do-commands db-spec
            (sql/drop-table-ddl (keyword tbname))))))

(defn load-card-table! [mp]
  (sql/insert-multi! db-spec :card
     (map #(hash-map :setNum (:number (first (get-in % [:_source :set])))
                     :setName (:name (first (get-in % [:_source :set])))
                     :cardNumber (get-in % [:_source :collectorNumber])
                     :alliance (get-in % [:_source :alliance])
                     :category (get-in % [:_source :category :en])
                     :class (get-in % [:_source :class :en])
                     :name (get-in % [:_source :name])
                     :rarity (get-in % [:_source :rarity])) mp))) 

(defn export-card-tsv [file mp]
  (with-open [writer (io/writer file)]
    (.write writer (str (clojure.string/join "\t" ["Set" "SetNum""Number" "Alliance" "Category" "Class" "Name" "Rarity"]) "\n"))
    (doseq [i mp]
      (let [s-set (first (get-in i [:_source :set]))
            s-number (:number s-set)
            s-name (:name s-set)
            c-number (get-in i [:_source :collectorNumber])
            c-alliance (get-in i [:_source :alliance])
            c-category (get-in i [:_source :category :en])
            c-class (get-in i [:_source :class :en])
            c-name (get-in i [:_source :name])
            c-rarity (get-in i [:_source :rarity])
            ]
        (.write writer (str (clojure.string/join "\t" [s-number s-name c-number c-alliance c-category c-class c-name c-rarity]) "\n"))))))        

(defn -main []
  (let [c (sp/client {:hosts ["https://carddatabase.warhammerchampions.com"]})
        response  (sp/request c {:url "/warhammer-cards/_search"              
                                 :method :post
                                 :body {:size 1000}})
        response-body (:body response)
        card-count (get-in response-body [:hits :total])
        all-cards (get-in response-body [:hits :hits])
        death-cards (filter #(= (get-in % [:_source :alliance]) "Death") all-cards)]
        
      ;;write counts of subsets (can compare with totals documented on Warhammer Champions site)
     (println (map #(str (first %) " " (count (last %))) (group-by #(get-in % [:_source :alliance]) all-cards)))
     (println (map #(str (first %) " " (count (last %))) (group-by #(get-in % [:_source :rarity]) all-cards)))
     (println (map #(str (first %) " " (count (last %))) (group-by #(get-in % [:_source :category :en]) all-cards)))
    
    
     ;;some more subtotals
     (println (sort (map #(str (first %) "\t" (count (last %)) ",") (group-by #(str (get-in % [:_source :alliance]) "-"(get-in % [:_source :rarity])) all-cards))))
     (println (sort (map #(str (first %) "\t" (count (last %)) ",") (group-by #(str (get-in % [:_source :alliance]) "-"(get-in % [:_source :category :en])) all-cards))))
     (println (sort (map #(str (first %) "\t" (count (last %)) ",") (group-by #(str (get-in % [:_source :category :en]) "-"(get-in % [:_source :rarity])) all-cards))))
    
       
     (spit "resources/wh_champions/OUT/all_cards.json" response-body)
     (export-card-tsv "resources/wh_champions/OUT/card_list_all.tsv" all-cards)
     (export-card-tsv "resources/wh_champions/OUT/card_list_death.tsv" death-cards) 
       
     (println (str "Total Cards: "card-count))
     
      ;;drop card table
     (drop-table! "card")

      ;;create card table in sqlite
     (create-card-table!)
    
      ;;insert rows in sqlite
     (load-card-table! all-cards)
    
      ;;print totals again querying sqlite database)
     (println (sql/query db-spec ["Select Count(*), Alliance from card group by Alliance"]))
     (println (sql/query db-spec ["Select setName, Count(*) from card group by setName"]))
    
     (sp/close! c)))
