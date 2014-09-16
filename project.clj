(defproject maze-maker "0.1.0"
  :description "maze-maker: generates simple ASCII mazes"
  :url "https://github.com/triposorbust/maze-maker"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]]
  :main ^:skip-aot maze-maker.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
