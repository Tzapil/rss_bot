(defproject rss-get "0.1.0-SNAPSHOT"
  :description "Feed bot"
  :url "https://telegram.me/fdr_bot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojars.scsibug/feedparser-clj "0.4.0"]
                 [tblibrary "0.1.2-SNAPSHOT"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot rss-get.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
