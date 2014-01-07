(defproject blog-code "0.1.0-SNAPSHOT"
  :description "Some codez for tha blag"
  :url "http://potetm.github.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [jayq "2.5.0"]
                 [yolk "0.10.0"]]
  :plugins [[lein-cljsbuild  "1.0.1"]]
  :cljsbuild {:builds
              [{:id "frp-dev"
                :source-paths ["src/blog/frp"]
                :compiler {:optimizations :whitespace
                           :pretty-print true
                           :output-to "../js/frp.js"}}
               {:id "frp"
                :source-paths ["src/blog/frp"]
                :compiler {:optimizations :advanced
                           :pretty-print false
                           :output-to "../js/frp.js"
                           :externs ["externs/bacon.js"
                                     "externs/bacon-jquery.js"
                                     "externs/jquery-1.8.js"]}}]})
