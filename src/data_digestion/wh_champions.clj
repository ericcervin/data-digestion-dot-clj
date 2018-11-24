(ns data-digestion.wh_champions
  (:require [qbits.spandex :as sp]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))


(defn export-card-tsv [file mp]
  (with-open [writer (io/writer file)]
    (.write writer (str (clojure.string/join "\t" ["Number" "Faction" "Category" "Class" "Name" "Rarity"]) "\n"))
    (doseq [i mp]
      (let [c-number (get-in i [:_source :collectorNumber])
            c-faction (get-in i [:_source :alliance])
            c-category (get-in i [:_source :category :en])
            c-class (get-in i [:_source :class :en])
            c-name (get-in i [:_source :name])
            c-rarity (get-in i [:_source :rarity])]
        (.write writer (str (clojure.string/join "\t" [c-number c-faction c-category c-class c-name c-rarity]) "\n"))))))        

(defn -main []
  (let [c (sp/client {:hosts ["https://carddatabase.warhammerchampions.com"]})
        response  (sp/request c {:url "/warhammer-cards/_search"              
                                 :method :post
                                 :body {:size 1000}})
        response-body (:body response)
        card-count (get-in response-body [:hits :total])
        all-cards (get-in response-body [:hits :hits])
        death-cards (filter #(= (get-in % [:_source :alliance]) "Death") all-cards)]
        ;;all-cards (mapv #(get-in % [:_source :alliance]) death-list)]
    (do
     (spit "resources/wh_champions/OUT/all_cards.json" response-body)
     (export-card-tsv "resources/wh_champions/OUT/card_list_all.tsv" all-cards)
     (export-card-tsv "resources/wh_champions/OUT/card_list_death.tsv" death-cards) 
     ;;(spit "resources/wh_champions/OUT/alliance_list.txt" alliance-list
     ;;(spit "resources/wh_champions/OUT/death_list.txt" death-list
     (println card-count)
     (println (count death-cards)))))
      



