# data-digestion

A collection of Clojure code for digesting data from APIs, downloads, etc. Final output TBD but it'll probably involve SQLite, JSON, and TSV.


destiny.clj - Downloads Star Wars Destiny game data from api. 
              Makes local copy of all JSON data.
              Exports files containing useful (to me) subsets of that data. 
              Exports data to local SQLite database.
              

destiny_every_team.clj - Pulls all Star Wars characters from the SQLite database from destiny.clj.
                         Considering the rules of Star Wars Destiny, exports every possible character team.

discogs.clj - Downloads list of releases in my record collection using the Discogs api.
              Exports basic release information into a tsv file.
              Exports data to local SQLite database.

gematria.clj - See: https://en.wikipedia.org/wiki/Gematria
               Assigns numerical values to the 10000 most common English words.
               Saves words, their lengths, and their values in a SQLite database.
               

magic.clj  - Imports Magic the Gathering data taken from https://mtgjson.com/
             Exports reports/summaries into tsvs

philosophy_usa.clj - Imports data taken from National Center for Education Statistics.
                     #s of philosophy/religious studies degrees 
                     completed at US universities in the 2014 - 2015 academic year.
                     
wh_champions.clj - Downloads Warhammer Age of Sigmar: Champions game data from api. 
                   Makes local copy of all JSON data.
                   Exports files containing useful (to me) subsets of that data. 
                   Exports data to local SQLite database.

