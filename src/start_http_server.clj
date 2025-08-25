#!/usr/bin/env bb

(println "🌐 Loading HTTP server dependencies...")
(require '[org.httpkit.server :as http]
         '[clojure.java.io :as io])

(defn static-file-handler
  "Simple static file handler for public directory"
  [request]
  (let [uri (:uri request)
        path (if (= uri "/") "/index.html" uri)
        file-path (str "public" path)
        file (io/file file-path)]
    (if (and (.exists file) (.isFile file))
      {:status 200
       :headers {"Content-Type" (cond
                                  (.endsWith path ".html") "text/html"
                                  (.endsWith path ".css") "text/css"
                                  (.endsWith path ".js") "application/javascript"
                                  (.endsWith path ".json") "application/json"
                                  (.endsWith path ".cljs") "application/javascript"
                                  :else "text/plain")}
       :body (slurp file)}
      {:status 404
       :headers {"Content-Type" "text/html"}
       :body "<!DOCTYPE html><html><body><h1>404 - File Not Found</h1></body></html>"})))

(println "🚀 Starting HTTP server with FIXED configuration...")
(println "   Port: 37373")
(println "   Directory: public")

(http/run-server static-file-handler {:port 37373})

(println "✅ HTTP server started at http://localhost:37373/")