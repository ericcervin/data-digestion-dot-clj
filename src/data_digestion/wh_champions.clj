(ns data-digestion.wh_champions
  (:require [qbits.spandex :as sp]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))


(defn export-card-tsv [file mp]
  (with-open [writer (io/writer file)]
    (.write writer (str (clojure.string/join "\t" ["Number" "Alliance" "Category" "Class" "Name" "Rarity"]) "\n"))
    (doseq [i mp]
      (let [c-number (get-in i [:_source :collectorNumber])
            c-alliance (get-in i [:_source :alliance])
            c-category (get-in i [:_source :category :en])
            c-class (get-in i [:_source :class :en])
            c-name (get-in i [:_source :name])
            c-rarity (get-in i [:_source :rarity])]
        (.write writer (str (clojure.string/join "\t" [c-number c-alliance c-category c-class c-name c-rarity]) "\n"))))))        

(defn -main []
  (let [c (sp/client {:hosts ["https://carddatabase.warhammerchampions.com"]})
        response  (sp/request c {:url "/warhammer-cards/_search"              
                                 :method :post
                                 :body {:size 1000}})
        response-body (:body response)
        card-count (get-in response-body [:hits :total])
        all-cards (get-in response-body [:hits :hits])
        death-cards (filter #(= (get-in % [:_source :alliance]) "Death") all-cards)]
        
      ;;write counts of subsets (can compare with totals documented on Warhammer Champions)
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
       
     (println (str "Total Cards: "card-count))))
     
      
