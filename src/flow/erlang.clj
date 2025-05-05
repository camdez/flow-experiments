(ns flow.erlang
  (:require
   [clojure.core.async.flow :as flow]
   [flow.utils :refer [monitoring]]))

;;; Quick translation of Erlang `tut15`:
;;; https://www.erlang.org/doc/system/conc_prog.html#message-passing

(defn ping-proc
  ([] {:ins  {:start "Ping requests"
              :pong  "Pong responses"}
       :outs {:out "Pong requests"}})
  ([args] args)
  ([state _status] state)
  ([state in-name num-pings]
   [state
    (case in-name
      :start
      (do
        (println (str "Ping requested: " num-pings " hops"))
        {:out [num-pings]})
      :pong
      (do
        (println (str "Ping received pong: " num-pings " hops"))
        (if (pos? num-pings)
          {:out [(dec num-pings)]}
          (println "Ping finished"))))]))

(def pong-proc
  (-> (fn [num-pings]
        (println (str "Pong received ping: " num-pings " hops"))
        num-pings)
      flow/lift1->step))

(def gdef
  {:procs
   {:ping {:proc (flow/process #'ping-proc)}
    :pong {:proc (flow/process #'pong-proc)}}
   :conns [[[:ping :out] [:pong :in]]
           [[:pong :out] [:ping :pong]]]})

(comment
  (def g (flow/create-flow gdef))

  (monitoring (flow/start g))
  (flow/resume g)
  (flow/stop g)

  (flow/inject g [:ping :start] [3])

  (prn (flow/ping g))
  )
