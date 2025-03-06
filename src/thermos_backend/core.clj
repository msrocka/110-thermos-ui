;; This file is part of THERMOS, copyright © Centre for Sustainable Energy, 2017-2021
;; Licensed under the Reciprocal Public License v1.5. See LICENSE for licensing details.

(ns thermos-backend.core
  (:require [org.httpkit.server :as httpkit]
            [thermos-backend.db.users :as users]
            [thermos-backend.hacks]
            [thermos-backend.config :refer [config]]
            [thermos-backend.handler :as handler]
            [thermos-backend.queue :as queue]
            [thermos-backend.solver.core :as solver]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate]]
            [mount.core :as mount])
  (:gen-class))

(defstate
  server
  :start
  (let [server-config
        {:max-body (* 1024 1024 (config :web-server-max-body))
         :no-cache (config :web-server-disable-cache)
         :port (config :web-server-port)}

        enabled (config :web-server-enabled)
        ]
    (when enabled (httpkit/run-server handler/all server-config)))
  :stop
  (and server (server :timeout 100)))

(defstate
  init-users
  :start (users/load-predefined-users!))

(defn -main [& args]
  (log/info "Starting THERMOS application")
  (mount/start)

  (when (= "true" (config :restart-running-jobs))
    (log/info "Restarting all previously running jobs")
    (queue/restart-all-running!))
  
  ;; wait for stop.
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(do (mount/stop)
                                  (shutdown-agents)))))


