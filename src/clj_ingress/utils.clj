(ns clj-ingress.utils
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io])
  (:import [java.io File]
           [java.time Instant]))




(defn timestamp []
  (.getEpochSecond (Instant/now)))

(defn backup-file [file-name]
  (let [f (io/file file-name)
        dir (.getParent f)
        bkp-name (str (.getName f) "." (timestamp) ".bkp")
        bkp-path (str  dir File/separator bkp-name)]
    (io/copy f (io/file bkp-path))))


(defn load-ingress-yaml [file-name]
  (-> (slurp file-name)
      yaml/parse-string))

(defn rules [yml]
  (-> yml :spec :rules))
(defn rules-hosts [yml]
  (-> yml
      rules
      (->> (map :host))))

(defn tls-hosts [yml]
  (-> yml
      :spec
      :tls
      first
      :hosts
      set))

(defn remove-tls-host [yml host]
  (let [update-tls (fn [tls-list]
                     (let [tls (first tls-list)
                           updated (update tls :hosts #(filter (fn [h] (not= h host)) %))]
                       (list updated)))]
    (update-in yml [:spec :tls] update-tls)))

(defn remove-host-rule [yml host]
  (-> yml
      (update-in [:spec :rules]
                 (fn [rules]
                   (filter (fn [rule]
                             (not= host (:host rule)))
                           rules)))))

(defn remove-host [yml host]
  (-> yml
      (remove-host-rule host)
      (remove-tls-host host)))

(defn add-tls-host [yml host]
  (if-not ((tls-hosts yml) host)
    (let [update-tls (fn [tls-list]
                      (let [tls (first tls-list)
                            updated (update tls :hosts concat [host])]
                        (list updated)))]
      (update-in yml [:spec :tls] update-tls))
    yml))

    

(defn get-ingress-rule
  ([yml host]
   (-> yml
       rules
       (->> (filter #(= host (:host %))))
       first))
  ([yml]
   (-> yml
       rules)))

(defn make-simple-ingress-rule [& {:keys [host service-name port-number pathType path]
                                   :or {pathType "Prefix" path "/(.*)"}}]
  {:host host
   :http {:paths (list
                  {:backend {:service {:name service-name
                                       :port {:number port-number}}}
                   :pathType pathType
                   :path path})}})



(defn add-host-rule [yml host service-name service-port]
  (if-not (get-ingress-rule yml host)
    (let [rule (make-simple-ingress-rule :host host :service-name service-name :port-number service-port)]
      (-> yml
          (update-in [:spec :rules] conj rule)))
    yml))


(defn add-host [yml host service-name service-port]
  (-> yml
      (add-tls-host host)
      (add-host-rule host service-name service-port)))

(defn default-service [yml]
  (-> yml
      :spec
      :defaultBackend
      :service))
