(ns clj-ingress.command.get-rule
  (:require [clj-ingress.command.multi :refer [perform-command]]
            [clj-ingress.utils :as utils]
            [clojure.pprint :refer [print-table]]))

(defn get-rule
  ([file host]
   (if-not (nil? host)
     (list
      (utils/get-ingress-rule
       (utils/load-ingress-yaml file)
       host))
     (-> file
         utils/load-ingress-yaml
         utils/get-ingress-rule))))

(defn rule-map [rule]
  (let [host (:host rule)
        service-name (-> rule :http :paths first :backend :service :name)
        port (-> rule :http :paths first :backend :service :port :number)]
    {:host host
     :service service-name
     :port port}))

(defmethod perform-command :get-rule
  [{:keys [options]}]
  (let [{:keys [file host]} options]
    (print-table
      (-> file
          (get-rule host)
          (->> (map rule-map))))))
