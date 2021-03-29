;; This clojurescript program runs a simple express web server.
;; It reads a Google secret, and responds with "Hello <secret>!".
;; If the Google secret query fails, it responds with "Hello <TARGET>!", 
;; where TARGET is an env variable. Finally,
;; the server responds wiht "Hello world!", if the secret 
;; and env variable are both not set.
;;
;; The ENV variable "GOOGLE_APPLICATION_CREDENTIALS" must be 
;; set for auth to work properly for local testing. If run on GCP,
;; the program should use a dedicated service account with the
;; settings adjusted to allow for Secret Manager access.
;;
;; The env variable PORT determines which web server port, or
;; defaults to 3000. Cloud Run sets PORT to 8080.
;;
;; This program borrows heavily from the following blog: 
;; https://ian-says.com/articles/clojurescript-expressjs-docker-api-server/
;;
;; Nick Brandaleone - March 2021

(ns server.main
  (:require 
    ["@google-cloud/secret-manager" :as Secret]
    ["express" :as express]
    [taoensso.timbre :as log]))
;      :refer-macros [log  trace  debug  info  warn  error  fatal  report
;                     logf tracef debugf infof warnf errorf fatalf reportf
;                     spy get-env]]))
;   [cljs.core.async :refer [go timeout]]))

;  (:require-macros
;   [cljs.core.async.macros :as m :refer [go go-loop alt!]])
;  (:require
;   [cljs.core.async :as async :refer [chan close! timeout put!]]

(enable-console-print!)

;; I should not hard-code this to a particular project. TODO.
(defonce mysecret "projects/659824402950/secrets/TARGET/versions/latest")
(defonce word (atom nil))
(defonce server (atom nil))

(defn -js->clj+ [x]
 "For cases when built-in js->clj doesn't work. Source: https://stackoverfl    ow.com/a/32583549/4839573"
   (into {} (for [k (js-keys x)]
              [k (aget x k)])))

(defn env []
  "Returns current env vars as a Clojure map."
  (-js->clj+ (.-env js/process)))

(defn get-word []
  "determine the TARGET value, to follow 'hello' if secret-manager fails"
  (let [t (get (env) "TARGET" "World")] t))

(defn secret-api []
  "Calls GCP Secret Manager, using a JS promise
   Use an atom to store state. Probably shouldn't, but it make things
   a bit easier to test."
  (let [client (Secret/SecretManagerServiceClient.)]
    (-> (.accessSecretVersion client (clj->js {:name mysecret}))
      (.then (fn [name]
               (let [payload (first name)] 
                 (reset! word (str (.. payload -payload -data)))))
             (fn [] (do
                      (reset! word (get-word))
                      (log/debug "Secret Manager Promise rejected")))))))

(defn server-started-callback
  "Callback triggered when ExpressJS has started"
  [port]
  (log/info "Web server listening on port:" port))

(defn canonicalise-fn
  "Create a function, depenging on input type"
  [item]
  (cond (fn? item) item
        (string? item) (fn [_ _] {:message item})
        :else (fn [_ _] item)))

(defn handler [handler-fn]
  "Function to handle generic route"
  ;; If one wants to redirect https -> https. 
  ;; Not required on GCP, since frontend handles https certs, etc...
;  (if (= "https" (aget (.-headers req) "x-forwarded-proto"))
;    (.redirect res (str "http://" (.get req "Host") (.-url req)))
  (let [canonical-fn (canonicalise-fn handler-fn)]
    (fn [req res]
      (log/info "Request:" (.-method req) (.-url req))
      ; (log/debug "Request:" (.-??? req))
      ;; Debug js objects
      ; (js/console.log "DEBUG:" req)
      (-> res
        (.status 200)
;       (.set "Content-Type" "text/html")
        (.json (clj->js (canonical-fn req res)))))))     

(defn start-server []
  (let [port (or (.-PORT (.-env js/process)) 3000)]
    (doto (new express)
      (.get "/" (handler "Hello Clojuresript"))
      (.get "/secret" (handler (str "Hello, " @word "!")))
      (.get "/foo" (handler {:result "Nothing happens"}))
      (.get "/echo" (handler (fn [req _] {:method (.-method req) :url (.-url req)})))
      (.listen port (server-started-callback port)))))

(defn start! []
  ;; called by main and after reloading code
  (log/info "App loaded!\n")
  (reset! server (start-server)))

(defn stop! []
  ;; called before reloading code
  (.close @server)
  (reset! server nil))

(defn main []
  ;; executed once, on startup, can do one time setup here
  (secret-api) ; Get secret from Google Cloud

  ; The GCP secret is first choice; second is env variable TARGET;
  ; default is "World". This watcher starts the web-server once we have
  ; the variable set.  This is a bit heavy-handed, but is solid.
  (add-watch word :watcher
             (fn [key atom old-state new-state]
                ;; For debugging
;               (prn "-- Atom Changed --")
;               (prn "key" key)
;               (prn "atom" atom)
;               (prn "old-state" old-state)
;               (prn "new-state" new-state)
               (start!))))

; This sets the main function for node running on command line.
; This may not be requred for shadow-cljs
(set! *main-cli-fn* main)

;; Launch app. Default port is 3000
;; PORT=8080 TARGET=galaxy node main.js
