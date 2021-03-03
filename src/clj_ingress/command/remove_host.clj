(ns clj-ingress.command.remove-host
  (:require [clj-ingress.command.multi :refer [perform-command]]
            [clj-ingress.utils :as utils]
            [clj-yaml.core :as yaml]))

(defn- remove-host
  "removes the host from both the tls hosts and the ingress rules. generates a backup of the ingress file
  with the name ending in <timestamp>.bkp and overwrites the input file."
  [file host]
  (utils/backup-file file)
  (let [yml (utils/load-ingress-yaml file)
        updated (utils/remove-host yml host)
        output-str (yaml/generate-string updated)]
    (spit file output-str)))

(defmethod perform-command :remove-host
  [{:keys [options]}]
  (let [{:keys [file host] } options]
    (remove-host file host)))
