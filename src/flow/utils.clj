(ns flow.utils
  (:require
   [clojure.core.async :as async]
   [clojure.pprint :as pp]))

(defn monitoring [{:keys [report-chan error-chan]}]
  (prn "========= monitoring start")
  (async/thread
    (loop []
      (let [[val port] (async/alts!! [report-chan error-chan])]
        (if (nil? val)
          (prn "========= monitoring shutdown")
          (do
            (prn (str "======== message from " (if (= port error-chan) :error-chan :report-chan)))
            (pp/pprint val)
            (recur))))))
  nil)

(def prn-proc
  {:describe  (fn [] {:ins {:in "Values to print"}})
   :transform (fn [_ _ msg] (prn msg))})
