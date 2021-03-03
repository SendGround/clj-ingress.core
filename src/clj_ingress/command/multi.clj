(ns clj-ingress.command.multi)

(defmulti  perform-command  :command)
