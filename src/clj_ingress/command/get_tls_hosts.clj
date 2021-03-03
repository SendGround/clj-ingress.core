(ns clj-ingress.command.get-tls-hosts
  (:require [clj-ingress.command.multi :refer [perform-command]]
            [clj-ingress.utils :as utils]
            [clojure.string :as cstr]))

(defn- get-tls-hosts [file]
  (let [yml (utils/load-ingress-yaml file)
        hosts (utils/tls-hosts yml)]
    (println "TLS hosts: ")
    (println (cstr/join \newline hosts))))

(defmethod perform-command :get-tls-hosts
  [{:keys [options]}]
  (let [{:keys [file]} options]
    (get-tls-hosts file)))
