(ns build.cljs
  (:require [cljs.build.api :as cljs]
            [clojure.edn :as edn]))

(defn build-cljs
  "Build ClojureScript files with optional debug optimizations.

   Options:
   - :debug-optimizations - If true, builds with advanced optimizations but keeps pretty-print and pseudo-names"
  [& {:keys [debug-optimizations]}]
  (let [cljs-builds (edn/read-string (slurp "cljs-builds.edn"))]
    (doseq [build cljs-builds]
      (println "Compiling ClojureScript for" (:main build))

      (cljs/build
        "src"
        (if debug-optimizations
          (assoc build
            :parallel-build true
            :preloads nil
            :infer-externs true
            :optimizations :advanced
            :pretty-print true
            :pseudo-names true)

          (assoc build
            :parallel-build true
            :preloads nil
            :infer-externs true
            :optimizations :advanced))))))

;; To run from the REPL:
;; (build-cljs :debug-optimizations true)
