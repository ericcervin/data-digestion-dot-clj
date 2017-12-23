# data-digestion

A collection of Clojure code for digesting data from APIs, downloads, etc. Final output TBD but it'll probably involve SQLite, JSON, and TSV.


destiny.clj - Downloads Star Wars Destiny game data from api. 
              Makes local copy of all JSON data.
              Exports files containing useful (to me) subsets of that data. 
              Exports data to local SQLite database.
              

discogs.clj - Downloads list of releases in my record collection using the Discogs api.
              Exports basic release information into a tsv file.
              Exports data to local SQLite database.
              
philosophy_usa.clj Imports data taken from National Center for Education Statistics.
                   #s of philosophy/religious studies degrees 
                   completed at US Universities in the 2014 - 2015 academic year.