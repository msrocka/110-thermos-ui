;; This file is part of THERMOS, copyright © Centre for Sustainable Energy, 2017-2021
;; Licensed under the Reciprocal Public License v1.5. See LICENSE for licensing details.

(ns build.pkg
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [cljs.build.api :as cljs]
            [less4clj.api :as less]
            [clojure.edn :as edn]
            [badigeon.clean :as clean]
            [badigeon.compile :as compile]
            [badigeon.classpath :as classpath]
            [build.uberjar :as uberjar]
            [clojure.term.colors :refer :all]
            [clojure.string :as string]))

(defn- in-thread-group [f & args]
  (let [exception (atom nil)

        group (proxy [ThreadGroup] ["Build group"]
                (^void uncaughtException [^Thread t ^Throwable e]
                 (println (on-red (white "Build error: " (bold (.getMessage e)))))
                 (reset! exception e)))

        thread (Thread. group #(apply f args))
        ]
    (.start thread)
    (.join thread)
    (when-let [e @exception]
      (throw e))))

(defn build-jar [{debug-optimizations :debug-optimizations}]
  (println (on-cyan (white "clean")))

  (clean/clean "target")

  (def cljs-builds
    (edn/read-string (slurp "cljs-builds.edn")))


  (let [build-less  (future (println (on-cyan (white "compile less to css")))
                            (less/build
                             {:source-paths ["resources"]
                              :target-path "target/resources/"
                              :compression true}))

        build-clojure (future 
                        (println (on-cyan (white "compile clojure")))
                        (in-thread-group
                         #(compile/compile
                           '[thermos-backend.core]
                           {:compile-path "target/classes"
                            :compiler-options {:disable-locals-clearing false
                                               :elide-meta [:doc :file :line :added]
                                               :direct-linking true}
                            :classpath (classpath/make-classpath {:aliases [:server]})})))

        build-cljs (future (doseq [build cljs-builds]
                             (println (on-cyan (white "compile clojurescript for " (bold (:main build)))))
                             
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
                                       :optimizations :advanced)))
                             ))
        ]
    
    @build-less
    @build-clojure
    @build-cljs)
  

  ;; Finally create uberjar. For whatever reason, there is no good
  ;; library for doing this at the moment. Lots of people seem to have
  ;; made 95% of one, including depstar, and I am hacking the final 5%
  ;; here:

  (let [etag (try (string/trim (:out (sh "git" "rev-parse" "HEAD")))
                  (catch Exception e))]
    (println (on-cyan (white "update etag")) etag)
    (.mkdirs (io/file "target/resources"))
    (spit "target/resources/git-rev.txt" etag))
  
  (println (on-cyan (white "create jar")))
  (uberjar/create-uberjar
   "target/thermos.jar"
   :classpath (classpath/make-classpath {:aliases [:server :jar :client]})
   
   :manifest {:Main-Class 'thermos-backend.core
              :Specification-Title "Java Advanced Imaging Image I/O Tools"
              :Specification-Version "1.1"
              :Specification-Vendor "Forward Dynamics"
              :Implementation-Title "com.sun.media.imageio"
              :Implementation-Version "1.1"
              :Implementation-Vendor "Forward Dynamics"
              ;; hikaricp appears to require reflective accesses that
              ;; are forbidden unless you do this
              :Add-Opens "java.base/java.lang"
              }))


(defn build-cli-tool []
  (println (on-cyan (white "clean")))

  (clean/clean "target")

  (println (on-cyan (white "compile clojure")))
  (in-thread-group
   #(compile/compile
     '[thermos-cli.core]
     {:compile-path "target/classes"
      :compiler-options {:disable-locals-clearing false
                         :elide-meta [:doc :file :line :added]
                         :direct-linking true}
      :classpath (classpath/make-classpath {:aliases [:server]})}))

  ;; Finally create uberjar. For whatever reason, there is no good
  ;; library for doing this at the moment. Lots of people seem to have
  ;; made 95% of one, including depstar, and I am hacking the final 5%
  ;; here:
  (println (on-cyan (white "create jar")))
  (uberjar/create-uberjar
   "target/thermos-cli.jar"
   :classpath (classpath/make-classpath {:aliases [:server :jar]})
   :manifest {:Main-Class 'thermos-cli.core
              :Specification-Title "Java Advanced Imaging Image I/O Tools"
              :Specification-Version "1.1"
              :Specification-Vendor "Forward Dynamics"
              :Implementation-Title "com.sun.media.imageio"
              :Implementation-Version "1.1"
              :Implementation-Vendor "Forward Dynamics"}))


(defn build-zoning-tool []
  (println (on-cyan (white "clean")))

  (clean/clean "target")

  (println (on-cyan (white "compile clojure")))
  (in-thread-group
   #(compile/compile
     '[thermos-cli.zone-cli]
     {:compile-path "target/classes"
      :compiler-options {:disable-locals-clearing false
                         :elide-meta [:doc :file :line :added]
                         :direct-linking true}
      :classpath (classpath/make-classpath {:aliases [:server]})}))

  (println (on-cyan (white "create jar")))
  (uberjar/create-uberjar
   "target/zone-cli.jar"
   :classpath (classpath/make-classpath {:aliases [:server :jar]})
   :manifest {:Main-Class 'thermos-cli.zone-cli
              :Specification-Title "Java Advanced Imaging Image I/O Tools"
              :Specification-Version "1.1"
              :Specification-Vendor "Forward Dynamics"
              :Implementation-Title "com.sun.media.imageio"
              :Implementation-Version "1.1"
              :Implementation-Vendor "Forward Dynamics"}))


(defn build-heatmap-only-tool []
  (println (on-cyan (white "clean")))

  (clean/clean "target")

  (println (on-cyan (white "compile clojure")))
  (in-thread-group
   #(compile/compile
     '[thermos-cli.heatmap]
     {:compile-path "target/classes"
      :compiler-options {:disable-locals-clearing false
                         :elide-meta [:doc :file :line :added]
                         :direct-linking true}
      :classpath (classpath/make-classpath
                  {:aliases [:heatmap-tool]})}))

  (println (on-cyan (white "create jar")))
  (uberjar/create-uberjar
   "target/thermos-heat-model.jar"
   :classpath (classpath/make-classpath {:aliases [:server :jar]})
   :manifest {:Main-Class 'thermos-cli.heatmap
              :Specification-Title "Java Advanced Imaging Image I/O Tools"
              :Specification-Version "1.1"
              :Specification-Vendor "Forward Dynamics"
              :Implementation-Title "com.sun.media.imageio"
              :Implementation-Version "1.1"
              :Implementation-Vendor "Forward Dynamics"})
  )


(defn build-noder []
  (println (on-cyan (white "clean")))

  (clean/clean "target")

  (println (on-cyan (white "compile clojure")))
  (in-thread-group
   #(compile/compile
     '[thermos-cli.noder]
     {:compile-path "target/classes"
      :compiler-options {:disable-locals-clearing false
                         :elide-meta [:doc :file :line :added]
                         :direct-linking true}
      :classpath (classpath/make-classpath
                  {:aliases [:noder]})}))

  (println (on-cyan (white "create jar")))
  (uberjar/create-uberjar
   "target/thermos-noder.jar"
   :classpath (classpath/make-classpath {:aliases [:server :jar]})
   :manifest {:Main-Class 'thermos-cli.noder
              :Specification-Title "Java Advanced Imaging Image I/O Tools"
              :Specification-Version "1.1"
              :Specification-Vendor "Forward Dynamics"
              :Implementation-Title "com.sun.media.imageio"
              :Implementation-Version "1.1"
              :Implementation-Vendor "Forward Dynamics"}))
