(defproject blender-corp-web "0.1.0-SNAPSHOT"
  :description "Blender Corp HR simulator"
  :url "http://cfelde.com/misc/blender-corp"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [prismatic/schema "0.4.3"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :plugins [[lein-cljsbuild "1.0.6"]]
  :cljsbuild {
    :builds [{
        ; The path to the top-level ClojureScript source directory:
        :source-paths ["src-cljs"]
        ; The standard ClojureScript compiler options:
        ; (See the ClojureScript compiler documentation for details.)
        :compiler {
          :output-to "web-target/cljs.js"  ; default: target/cljsbuild-main.js
          :optimizations :simple
          :pretty-print true}}]})
