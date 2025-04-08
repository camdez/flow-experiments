(ns flow.onion
  "Onion router implemented with clojure.core.async.flow."
  (:require
   [clojure.core.async.flow :as flow]
   [flow.utils :refer [monitoring prn-proc]]))

(defn- routed-msg [body to]
  {::to to ::body body})

(defn- random-route [node-cnt hops]
  {:pre [(> node-cnt 1)]}
  (into []
        (comp (dedupe) (take hops))
        (repeatedly #(rand-int node-cnt))))

(defn- router-proc
  ([] {:params {:nodes "Number of nodes"
                :hops  "Number of hops each message should be routed before delivery"}
       :ins    {:in "Messages to fully route"}})
  ([args] args)
  ([state _status] state)
  ([{:keys [nodes hops] :as state} _in-name msg]
   (let [{::keys [to] :as r-msg} (reduce routed-msg msg (random-route nodes hops))]
     [state {[to :in] [r-msg]}])))

(defn- node-proc
  ([] {:ins  {:in "Messages to route"}
       :outs {:out "Terminal messages (routing complete)"}})
  ([args] args)
  ([state _status] state)
  ([{::flow/keys [pid] :as state} in-name {::keys [body] :as msg}]
   (prn {:event :message-received :pid pid :from in-name :msg msg})
   (if-let [to (::to body)]
     [state {[to :in] [body]}]
     [state {:out [body]}])))

(defn flow [nodes hops]
  {:procs
   (-> {:router {:proc (flow/process #'router-proc)
                 :args {:nodes nodes
                        :hops  hops}}
        :prn    {:proc (flow/process prn-proc)}}
       (into (for [n (range nodes)]
               [n {:proc (flow/process #'node-proc)}])))
   :conns
   (for [n (range nodes)]
     [[n :out] [:prn :in]])})

(defn send! [g msg]
  (flow/inject g [:router :in] [msg]))

(comment
  (def g (flow/create-flow (flow 5 7)))

  (monitoring (flow/start g))
  (flow/resume g)
  (flow/stop g)

  (send! g "Hello!")
  )
