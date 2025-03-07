
(ns build.jar2
  (:require [clojure.tools.build.api :as b]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn create-uberjar
  "Create an uberjar with tools.build.
   
   Options:
   - :classpath - Classpath to use (defaults to java.class.path)
   - :manifest - Map of manifest entries
   - :skip-files - Patterns of files to skip (regex patterns)
   - :skip-jars - Patterns of jars to skip (regex patterns)"
  [target-path & {:keys [classpath manifest skip-files skip-jars]
                  :or {classpath (System/getProperty "java.class.path")
                       skip-files #{"^project\\.clj$"
                                    "^LICENSE$"
                                    "^COPYRIGHT$"
                                    "(?i)META-INF/.*\\.(?:MF|SF|RSA|DSA)"
                                    "(?i)META-INF/(?:INDEX\\.LIST|DEPENDENCIES|NOTICE|LICENSE)(?:\\.txt)?"}
                       skip-jars #{"depstar"}}}]

  (println "Creating uberjar" target-path)

  ;; Create a basis from the provided classpath
  (let [classpath-vec (if (string? classpath)
                        (str/split classpath (System/getProperty "path.separator"))
                        classpath)

        ;; Create a temporary directory for the build
        tmp-dir "target/build-tmp"
        class-dir (str tmp-dir "/classes")

        ;; Convert regex patterns to string patterns for tools.build
        skip-file-patterns (map #(re-pattern %) skip-files)
        skip-jar-patterns (map #(re-pattern %) skip-jars)

        ;; Create a custom basis to handle the specific classpath
        basis {:libs {}
               :paths []
               :classpath-args {:extra-paths classpath-vec}}]

    ;; Clean the temporary directory
    (b/delete {:path tmp-dir})
    (b/create-dirs {:paths [class-dir]})

    ;; Copy resources to class directory, filtering out skipped files
    (println "Copying resources to build directory")
    (doseq [path classpath-vec]
      (let [path-obj (io/file path)]
        (when (.exists path-obj)
          (if (.isDirectory path-obj)
            ;; Handle directory in classpath
            (let [skip? (fn [file]
                          (let [rel-path (str (b/relativize path (.getPath file)))]
                            (some #(re-find % rel-path) skip-file-patterns)))]
              (when-not (some #(re-find % path) skip-jar-patterns)
                (println "+ dir" path)
                (b/copy-dir {:src-dirs [path]
                             :target-dir class-dir
                             :include (fn [_] true)
                             :exclude skip?})))

            ;; Handle JAR file in classpath
            (when-not (some #(re-find % path) skip-jar-patterns)
              (println "+ jar" path))))))

    ;; Create the uber JAR with merged manifests
    (println "Building uberjar:" target-path)
    (b/uber {:class-dir class-dir
             :uber-file target-path
             :basis basis
             :manifest (into {"Created-By" "tools.build"
                              "Manifest-Version" "1.0.0"}
                             (for [[k v] manifest]
                               [(if (keyword? k) (name k) (str k))
                                (if (and (= k :Main-Class) (symbol? v))
                                  (-> (name v) (.replaceAll "-" "_"))
                                  (str v))]))

             ;; Merging strategies
             :conflict-handlers
             {"META-INF/services/" :concat-lines
              "data_readers.clj" :merge-edn
              "META-INF/registryFile.jaiext" :concat-lines
              "META-INF/registryFile.jai" :concat-lines}})

    ;; Clean up temp directory
    (println "Removing temporary files")
    (b/delete {:path tmp-dir})

    (println "Successfully created" target-path)
    :ok))