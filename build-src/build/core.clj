;; This file is part of THERMOS, copyright © Centre for Sustainable Energy, 2017-2021
;; Licensed under the Reciprocal Public License v1.5. See LICENSE for licensing details.

(ns build.core
  (:require [clojure.tools.cli :refer [parse-opts]]))

(def opts-for
  (let [debug ["-d" "--debug" "Produce part-mangled js to help debug what's wrong when using advanced optimisations"]]
    {:dev  [["-e" "--emacs" "Use cider middleware for emacs"]
            debug]
     :pkg  [debug]
     :cljs [debug]}))

(defn- command-of [args]
  (cond
    (empty? args) :dev,
    (and
      (> (count args) 1)
      (= (first args) "-m"))
    (command-of (rest (rest args))),
    :else (keyword (first args))))


(defn -main [& args]
  (let [command (command-of args)
        opts (parse-opts args (opts-for command))
        debug-optimizations (get-in opts [:options :debug])]

    (case command
      :pkg (do (require 'build.pkg)
               ((resolve 'build.pkg/build-jar)
                {:debug-optimizations debug-optimizations}))

      :cljs (do (require 'build.cljs)
                ((resolve 'build.cljs/build-cljs)
                 {:debug-optimizations debug-optimizations}))

      :cli-pkg (do (require 'build.pkg)
                   ((resolve 'build.pkg/build-cli-tool)))

      :zone-pkg (do (require 'build.pkg)
                    ((resolve 'build.pkg/build-zoning-tool)))

      :heat-pkg (do (require 'build.pkg)
                    ((resolve 'build.pkg/build-heatmap-only-tool)))

      :noder-pkg (do (require 'build.pkg)
                     ((resolve 'build.pkg/build-noder)))

      :dev (do
             (require 'build.dev)
             ((resolve 'build.dev/start-dev)
              {:nrepl-handler
               (if (:emacs (:options opts))
                 'build.emacs/emacs-middleware
                 'nrepl.server/default-handler)

               :debug-optimizations
               (:debug (:options opts))}))

      (do (println "Known tasks are pkg and dev")
          (println "e.g. clj -Aclient:server:dev dev")))))

