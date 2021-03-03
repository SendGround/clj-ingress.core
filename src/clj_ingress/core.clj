(ns clj-ingress.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.set :as sets]
            [clojure.string :as cstr]
            [clj-ingress.command.multi :refer [perform-command]]
            [clj-ingress.command.core])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn- resolve-command [arg]
  (condp = arg
    "get-rule"      :get-rule
    "get-tls-hosts" :get-tls-hosts
    "add-host-rule" :add-host-rule
    "remove-host"   :remove-host
    :unknown-command))

(def cli-opts
  [["-f" "--file PATH" "Input Ingress YAML file path"
    :default nil]
   ["-s" "--service-name NAME" "Service Name"
    :default nil]
   ["-p" "--service-port PORT" "Service port"
    :parse-fn int
    :default nil]
   ["-H" "--host HOSTNAME" "Host Name"
    :default nil]
   
   ["-h" "--help"
    :id :help]])

(def mandatory-options-by-command
  {:get-rule #{:file :host}
   :get-tls-hosts #{:file}
   :add-host-rule #{:file :host :service-name :service-port}
   :remove-host #{:file :host}})

(defn validate-command-mandatory-opts
  [command mandatory-opts]
  (fn [cmd opt-map]
    (if  (not (sets/subset?
               (set mandatory-opts)
               (into #{}
                     (for [[k v] opt-map
                           :when (some? v)]
                       k))))
      (let [opt-diff (sets/difference
                      (set mandatory-opts)
                      (into #{} (for  [[k v] opt-map :when (some? v)] k)))
            kw-str   #(apply str (rest %))]
        {:status  :error
         :message (apply str "Include following options: " (cstr/join ", " (map (comp kw-str str ) (vec opt-diff))))})
      {:status :ok})))

(def validator-by-command
  (->>
   (doall
    (for [[cmd mandatories] mandatory-options-by-command
          :when (some? mandatories)]
      [cmd (validate-command-mandatory-opts cmd mandatories)]))
   (into {})))


(defn- ok [& args]
  {:status :ok})

(defn- validate-command
  [command opts]
  (let [v (get validator-by-command command ok)]
    (v command opts)))

(defn handle-parse-result [res]
  (let [{:keys [arguments
                errors
                options]} res
        command           (resolve-command (first arguments))
        status            (if (or (= command :unknown-command)
                                  (some? errors))
                            :error
                            :ok)
        wrap-result       (fn [r]
                            (if (= (:status r) :error)
                              r
                              (merge
                               r
                               (validate-command command options))))]
    (-> res
        (assoc :command command
               :status status)
        (wrap-result))))

(defn- handle-errors [errs]
  (println "Error:")
  (doseq [e errs]
    (println e)))

(def usage-str
  [""
   "Usage: clj-ingress [OPTIONS] command"
   "command may be one of:"
   ""
   "- get-rule "
   "- get-tls-hosts "
   "- add-host-rule "
   "- remove-host"
   ""])


(defn greet
  "Callable entry point to the application."
  [data]
  (println (str "Hello, " (or (:name data) "World") "!")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; (greet {:name (first args)})
  (let [{:keys [options arguments summary errors] :as res}
        (-> args
            (parse-opts cli-opts)
            handle-parse-result)]
    (println res)
    (cond
      (some? errors)               (do
                                     (handle-errors errors)
                                     1)
      (some-> options :help some?) (do
                                     (println (cstr/join \newline usage-str))
                                     (println summary)
                                     0)
      (= :ok (-> res :status))     (do
                                     (perform-command res)
                                     0)
      (= :error (-> res :status)) (do
                                    (println "Error:")
                                    (println (-> res :message)))
      :else                        (do
                                     (println "failed.")
                                     1))))

