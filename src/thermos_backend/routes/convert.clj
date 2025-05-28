;; This file is part of THERMOS, copyright © Centre for Sustainable Energy, 2017-2021
;; Licensed under the Reciprocal Public License v1.5. See LICENSE for licensing details.

(ns thermos-backend.routes.convert
  (:require [thermos-backend.spreadsheet.core :as xl]
            [thermos-backend.spreadsheet.common :as xlc]
            [ring.util.response :as response]
            [ring.util.io :as rio]
            [thermos-backend.pages.cache-control :as cache-control]
            [thermos-util.converter :as geojson-converter]
            [cheshire.core :as json]
            [thermos-backend.db :as db]
            ))

(def xl-content-type "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")

(defn problem-to-excel [{{state :state} :body-params
                         {{file :tempfile} :file} :params
                         :as request}]
  (if file
    ;; inverse
    (-> (xl/from-spreadsheet file)
        (response/response)
        (cache-control/no-store))
    
    (-> (rio/piped-input-stream
         (fn [out]
           (try 
             (-> (xl/to-spreadsheet state)
                 (xlc/write-to-stream out))
             (catch Exception e
               (println "While outputting spreadsheet!")
               (clojure.stacktrace/print-throwable e)
               (.printStackTrace e)
               ))
           
           (.flush out)
           ))
        (response/response)
        (response/content-type xl-content-type)
        (cache-control/no-store))))

(defn problem-to-json [{{state :state} :body-params}]
  (-> (geojson-converter/network-problem->geojson state)
      (json/encode)
      (response/response)
      (response/content-type "application/json")
      (cache-control/no-store)))

(defn problem-to-json-2 [{{state :state} :body-params}]
  (-> (geojson-converter/network-problem->geojson-2 state)
      (json/encode)
      (response/response)
      (response/content-type "application/json")
      (cache-control/no-store)))

(defn problem-to-sophena [{{state :state path :path} :body-params}]
  (let [bytes (.getBytes (pr-str state))
        segments (clojure.string/split path #"/")
        net-id (Integer/parseInt (last segments))]
    (db/with-connection
      [con]
      (do
        (println net-id)
        (-> (response/response (java.io.ByteArrayInputStream. bytes))
            (response/content-type "application/octet-stream")
            (response/header "Content-Disposition" "attachment; filename=\"example.sophena\"")
            (cache-control/no-store))))))

(def converter-routes
  "Routes for converting a problem to other types.

  These require no access control because the application POSTS up the
  problem data, and the response is the spreadsheet/json/etc, so if
  someone hits the route they have to have the data already in some
  form.
  "
  ["/convert"
   [["/excel" #'problem-to-excel]
    ["/sophena" #'problem-to-sophena]
    ["/json" problem-to-json]
    ["/json-2" problem-to-json-2]]])

