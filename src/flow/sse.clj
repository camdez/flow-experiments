(ns flow.sse
  (:require
   [clojure.core.async.flow :as flow]
   [flow.utils :as utils]
   [org.httpkit.server :as server]))

;; Work in progress.

;; Extend to IRC-like example?
;; <connect>
;; /join #room
;; /send #room "message"
;; /part #room
;; <disconnect>

;; Should these be separate ins or commands in the messages?
;;
;; :joined / :left need to be unified so :left is always after :join.
;;
;; Seems like it should not be "broadcast", but a channel lookup (who
;; is connected?)  Maybe?
(defn- presence-proc
  ([] {:ins  {:joined    ""
              :left      ""
              :broadcast ""}
       :outs {:send "Messages to send"}})
  ([args]
   (println "Initializing presence data")
   (assoc args :connected #{}))
  ([state status]
   (prn {::event :transition
         :status status
         :state  state})
   state)
  ([{:keys [connected] :as state} in-name msg]
   (case in-name
     :joined
     (let [{:keys [resp-ch]} msg
           new-state         (update state :connected conj resp-ch)]
       (prn {:new-state new-state})
       (println (str resp-ch " joined"))
       [new-state])
     :left
     (let [{:keys [resp-ch]} msg]
       (println (str resp-ch " left"))
       [(update state :connected disj resp-ch)])
     :broadcast
     (let [msg (str "[" (count connected) "]: " msg)]
       [state {:send (map (juxt identity (constantly msg)) connected)}]))))

(defn app [{:keys [flow] :as system}]
  (fn handler [req]
    (server/as-channel
     req
     {:on-open (fn [ch]
                 (server/send! ch {:status 200 :headers {"Content-Type" "text/plain"}} false)
                 (flow/inject flow [:presence :joined] [{:resp-ch ch}])
                 false)
      :on-close (fn [ch status]
                  (println ch "closed" status)
                  (flow/inject flow [:presence :left] [{:resp-ch ch :status status}]))})))

(def gdef
  {:procs {:presence {:proc (flow/process #'presence-proc)}
           :sender   {:proc (-> {:describe
                                 (fn []
                                   {:ins {:in "Messages to send"}})
                                 :transform
                                 (fn [state _in-name [resp-ch msg]]
                                   (server/send! resp-ch msg false)
                                   [state])}
                                flow/process)}}
   :conns [[[:presence :send] [:sender :in]]]})

(defn build-system []
  {:server nil
   :flow   (flow/create-flow gdef)})

(defn start-system [s]
  (utils/monitoring (flow/start (:flow s)))
  (flow/resume (:flow s))
  (assoc s :server (server/run-server (app s) {:port 8080})))

(defn stop-system [s]
  ((:server s))
  (Thread/sleep 200) ; <---- flow could still be processing
  (flow/stop (:flow s))
  s)

(comment
  (def system (atom (build-system)))
  (swap! system start-system)
  (swap! system stop-system)

  (flow/pause (:flow @system))

  (-> (flow/ping-proc (:flow @system) :presence)
      ::flow/state)

  (flow/inject (:flow @system) [:presence :broadcast] [(str "Hello " (rand-int 1000) "!!\n\n")])
  )
