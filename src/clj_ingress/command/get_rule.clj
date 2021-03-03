(ns clj-ingress.command.get-rule
  (:require [clj-ingress.command.multi :refer [perform-command]]
            [clj-ingress.utils :as utils]
            [clojure.pprint :refer [pprint]]))

(defn get-rule [file host]
  (utils/get-ingress-rule
   (utils/load-ingress-yaml file)
   host))

(defmethod perform-command :get-rule
  [{:keys [options]}]
  (let [{:keys [file host]} options]
    (pprint (get-rule file host))))
