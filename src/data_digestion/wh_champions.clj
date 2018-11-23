(ns data-digestion.wh_champions
  (:require [qbits.spandex :as sp]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))


(defn -main []
  (let [c (sp/client {:hosts ["https://carddatabase.warhammerchampions.com"]})
        response  (sp/request c {:url "/warhammer-cards/_search"              
                                 :method :post
                                 :body {:size 100}})
        response-body (:body response)
        card-count (get-in response-body [:hits :total])
        card-list (get-in response-body [:hits :hits])
        alliance-list (mapv #(get-in % [:_source :alliance]) card-list)]
    (do
     (spit "resources/wh_champions/OUT/all_cards.json" response-body)
     (spit "resources/wh_champions/OUT/alliance_list.txt" alliance-list)
     (println card-count))))
      



