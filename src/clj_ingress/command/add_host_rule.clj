(ns clj-ingress.command.add-host-rule
  (:require [clj-ingress.command.multi :refer [perform-command]]
            [clj-ingress.utils :as utils]
            [clj-yaml.core :as yaml]))
(defn add-host-rule [file host service-name service-port]
  (utils/backup-file file)
  (let [yml (utils/load-ingress-yaml file)
        updated (utils/add-host yml host service-name service-port)
        output-str (yaml/generate-string updated)]
    (spit file output-str)))



(defmethod perform-command :add-host-rule
  [{:keys [options]}]
  (let [{:keys [file host service-name service-port]} options]
    (add-host-rule file host service-name service-port)))
