(defproject
  blog-code "0.1.0-SNAPSHOT"
  :description "Some codez for tha blag"
  :url "http://potetm.github.io"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.facebook/react "0.11.2"]
                 [cljs-uuid "0.0.4"]
                 [datascript "0.5.1"]
                 [jayq "2.5.2"]
                 [org.clojure/clojure "1.7.0-alpha3"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.match "0.2.0-rc5"]
                 [prismatic/dommy "0.1.3"]
                 [quiescent "0.1.4"]
                 [reagent "0.4.3"]
                 [yolk "0.10.0"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {:builds
              [{:id           "frp-dev"
                :source-paths ["src/blog/frp"]
                :compiler     {:optimizations :whitespace
                               :pretty-print  true
                               :output-to     "../js/frp.js"}}
               {:id           "frp"
                :source-paths ["src/blog/frp"]
                :compiler     {:optimizations :advanced
                               :pretty-print  false
                               :output-to     "../js/frp.js"
                               :externs       ["externs/bacon.js"
                                               "externs/bacon-jquery.js"
                                               "externs/jquery-1.8.js"]}}
               {:id           "responsive-design-csp-dev"
                :source-paths ["src/blog/responsive_design_csp"]
                :compiler     {:optimizations :whitespace
                               :pretty-print  true
                               :output-to     "../js/responsive-design-csp.js"}}
               {:id           "responsive-design-csp"
                :source-paths ["src/blog/responsive_design_csp"]
                :compiler     {:optimizations :advanced
                               :pretty-print  false
                               :output-to     "../js/responsive-design-csp.js"
                               :externs       ["externs/bacon.js"
                                               "externs/bacon-jquery.js"
                                               "externs/jquery-1.8.js"]}}
               {:id           "responsive-design-frp-dev"
                :source-paths ["src/blog/responsive_design_frp"]
                :compiler     {:optimizations :whitespace
                               :pretty-print  true
                               :warnings      false ;; dommy complains, but it's fine
                               :output-to     "../js/responsive-design-frp.js"}}
               {:id           "responsive-design-frp"
                :source-paths ["src/blog/responsive_design_frp"]
                :compiler     {:optimizations :advanced
                               :pretty-print  false
                               :output-to     "../js/responsive-design-frp.js"
                               :warnings      false ;; dommy complains, but it's fine
                               :externs       ["externs/bacon.js"
                                               "externs/bacon-jquery.js"
                                               "externs/jquery-1.8.js"]}}
               {:id           "reactive-design-dev"
                :source-paths ["src/blog/reactive_design"]
                :compiler     {:optimizations :whitespace
                               :pretty-print  true
                               :preamble      ["react/react.js"]
                               :output-to     "../js/reactive-design.js"}}]})
