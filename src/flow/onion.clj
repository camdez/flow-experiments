(ns flow.onion
  "Onion router implemented with clojure.core.async.flow."
  (:require
   [clojure.core.async.flow :as flow]
   [flow.utils :refer [monitoring prn-proc]]))

(defn- routed-msg [body to]
  {::to to ::body body})

(defn- random-route [node-pids hops]
  {:pre [(> (bounded-count 2 node-pids) 1)]}
  (into []
        (comp (dedupe) (take hops))
        (repeatedly #(rand-nth node-pids))))

(defn- router-proc
  ([] {:params {:node-pids "Process IDs of nodes"
                :hops      "Number of hops each message should be routed before delivery"}
       :ins    {:in "Messages to fully route"}})
  ([args] args)
  ([state _status] state)
  ([{:keys [node-pids hops] :as state} _in-name msg]
   (let [{::keys [to] :as r-msg} (reduce routed-msg msg (random-route node-pids hops))]
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
  (let [node-pids (mapv #(keyword (str "node-" %)) (range nodes))]
    {:procs
     (-> {:router {:proc (flow/process #'router-proc)
                   :args {:node-pids node-pids
                          :hops      hops}}
          :prn    {:proc (flow/process prn-proc)}}
         (into (for [np node-pids]
                 [np {:proc (flow/process #'node-proc)}])))
     :conns
     (for [np node-pids]
       [[np :out] [:prn :in]])}))

(defn send! [g msg]
  (flow/inject g [:router :in] [msg]))

(comment
  (def g (flow/create-flow (flow 5 7)))

  (monitoring (flow/start g))
  (flow/resume g)
  (flow/stop g)

  (send! g "Hello!")
  )
