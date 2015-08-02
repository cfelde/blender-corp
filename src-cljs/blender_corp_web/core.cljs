; Copyright 2015 Christian Felde
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns blender-corp-web.core
  (:require [schema.core :as s
             :include-macros true                           ;; cljs only
             ]
            [cljs.core.async :as async]
            [goog.dom :as dom]
            [goog.style :as style])
  (:require-macros [cljs.core.async.macros :as m :refer [go]]))

;;; Initial sim parameters + param holder
(def parameters (atom {:hire {:a true :b true :c false :d false :f false}
                       :fire {:a false :b false :c false :d true :f true}
                       :highskill {:a true :b true :c false :d false :f false}
                       :turnover 5
                       :fireturnover 10
                       :levelup 3
                       :leveldown 1
                       :gdb 10
                       :pause false}))

;;; Front end UI stuff
(defn gen-menu-params [p]
  {"Hire A" (-> p :hire :a)
   "Hire B" (-> p :hire :b)
   "Hire C" (-> p :hire :c)
   "Hire D" (-> p :hire :d)
   "Hire F" (-> p :hire :f)

   "Fire A" (-> p :fire :a)
   "Fire B" (-> p :fire :b)
   "Fire C" (-> p :fire :c)
   "Fire D" (-> p :fire :d)
   "Fire F" (-> p :fire :f)

   "Fire turn over (%)" (-> p :fireturnover)

   "A is high skill?" (-> p :highskill :a)
   "B is high skill?" (-> p :highskill :b)
   "C is high skill?" (-> p :highskill :c)
   "D is high skill?" (-> p :highskill :d)
   "F is high skill?" (-> p :highskill :f)

   "Turn over (%)" (-> p :turnover)
   "Level up (%)" (-> p :levelup)
   "Level down (%)" (-> p :leveldown)
   "Group dynamics boost (%)" (-> p :gdb)

   "Pause?" (-> p :pause)})

(defn ^:export init-menu []
  (let [example-one (gen-menu-params {:hire {:a true :b true :c false :d false :f false}
                                      :fire {:a false :b false :c true :d true :f true}
                                      :highskill {:a false :b false :c false :d false :f false}
                                      :turnover 5
                                      :fireturnover 10
                                      :levelup 0
                                      :leveldown 0
                                      :gdb 0
                                      :pause false})
        example-two (gen-menu-params {:hire {:a true :b true :c false :d false :f false}
                                      :fire {:a false :b false :c false :d true :f true}
                                      :highskill {:a false :b false :c false :d false :f false}
                                      :turnover 5
                                      :fireturnover 10
                                      :levelup 0
                                      :leveldown 1
                                      :gdb 10
                                      :pause false})
        example-three (gen-menu-params @parameters)

        menu-params {"preset" "Example 3"
                     "remembered" {"Example 1" { "0" example-one }
                                   "Example 2" { "0" example-two }
                                   "Example 3" { "0" example-three }}
                     "closed" true "folders" {}}]
    (.initMenu js/window (clj->js menu-params) (clj->js example-one))))

(defn ^:export config-update [js-update]
  (let [update (js->clj js-update :keywordize-keys true)]
    (condp = (first (keys update))
      :hire (reset! parameters (update-in @parameters [:hire (-> update :hire keys first)] #(identity %2) (-> update :hire vals first)))
      :fire (reset! parameters (update-in @parameters [:fire (-> update :fire keys first)] #(identity %2) (-> update :fire vals first)))
      :highskill (reset! parameters (update-in @parameters [:highskill (-> update :highskill keys first)] #(identity %2) (-> update :highskill vals first)))
      (reset! parameters (assoc @parameters (-> update ffirst) (-> update first second))))
    )
  )

(defn ^:export update-chart [js-data]
  (.drawChart js/window js-data))

(defn ^:export update-pie [js-data]
  (.drawPie js/window js-data))

(defn create-worker [id class color direction]
  (let [img (cond
              (and (= color :green) (= direction :left)) "green-man-left.gif"
              (and (= color :green) (= direction :right)) "green-man-right.gif"

              (and (= color :yellow) (= direction :left)) "yellow-man-left.gif"
              (and (= color :yellow) (= direction :right)) "yellow-man-right.gif"

              (and (= color :red) (= direction :left)) "red-man-left.gif"
              (and (= color :red) (= direction :right)) "red-man-right.gif"

              (and (= color :blue) (= direction :left)) "blue-man-left.gif"
              (and (= color :blue) (= direction :right)) "blue-man-right.gif"

              (and (= color :grey) (= direction :left)) "grey-man-left.gif"
              (and (= color :grey) (= direction :right)) "grey-man-right.gif")]
    (dom/createDom "img" (js-obj "id" id "class" class "src" img))))

(defn add-worker! [worker]
  (dom/appendChild (dom/getElement "building") worker))

(defn get-worker [id]
  (dom/getElement id))

(defn remove-worker! [id]
  (dom/removeNode (get-worker id)))

(defn set-worker-position! [id pos]
  (style/setStyle (get-worker id) "left" (str pos "px")))

(def worker-ids (atom 0))
(defn get-worker-id []
  (str "wid-" (swap! worker-ids inc)))

(defn walker-right [left-most right-most class color-chan]
  (let [workers (atom {})
        next-color (atom [])
        sleep 100]
    (go (while true (do
                      (let [color (async/<! color-chan)]
                        (compare-and-set! next-color [] (vec [color])))
                      (async/<! (async/timeout sleep)))))
    (go (while true (do
                      (async/<! (async/timeout sleep))
                      (if (not (:pause @parameters))
                        (do
                          (doall (map #(-> %1 (create-worker class %2 :right) add-worker!) (filter #(-> % get-worker nil?) (keys @workers)) @next-color))
                          (reset! next-color [])
                          (doall (map #(set-worker-position! (first %) (second %)) (remove #(-> % first get-worker nil?) @workers)))
                          (reset! workers (into {} (map #(vec [(first %) (-> % second inc)]) (remove #(-> % first get-worker nil?) @workers))))
                          (doall (map #(-> % first remove-worker!) (filter #(-> % second (> right-most)) @workers)))
                          (reset! workers (into {} (map #(vec [(first %) (second %)]) (filter #(-> % second (<= right-most)) @workers))))
                          (if (or (empty? @workers) (> (- (reduce min (vals @workers)) left-most) 20))
                            (swap! workers assoc (get-worker-id) left-most)))))))))

(defn walker-left [left-most right-most class color-chan]
  (let [workers (atom {})
        next-color (atom [])
        sleep 100]
    (go (while true (do
                      (let [color (async/<! color-chan)]
                        (compare-and-set! next-color [] (vec [color])))
                      (async/<! (async/timeout sleep)))))
    (go (while true (do
                      (async/<! (async/timeout sleep))
                      (if (not (:pause @parameters))
                        (do
                          (doall (map #(-> %1 (create-worker class %2 :left) add-worker!) (filter #(-> % get-worker nil?) (keys @workers)) @next-color))
                          (reset! next-color [])
                          (doall (map #(set-worker-position! (first %) (second %)) (remove #(-> % first get-worker nil?) @workers)))
                          (reset! workers (into {} (map #(vec [(first %) (-> % second dec)]) (remove #(-> % first get-worker nil?) @workers))))
                          (doall (map #(-> % first remove-worker!) (filter #(-> % second (< left-most)) @workers)))
                          (reset! workers (into {} (map #(vec [(first %) (second %)]) (filter #(-> % second (>= left-most)) @workers))))
                          (if (or (empty? @workers) (> (- right-most (reduce max (vals @workers))) 20))
                            (swap! workers assoc (get-worker-id) right-most)))))))))

;;; Blender corp sim stuff
(def EmployeeState (s/enum :stay :leave))

(def TransitionProbability {:stay  {:stay  double,
                                    :leave double},
                            :leave {:stay  double,
                                    :leave double}})

(def SkillLevel (s/enum :a :b :c :d :f))

(def Employee {:id         s/Int,
               :level      SkillLevel
               :state      EmployeeState,
               :transition TransitionProbability})

(def Staff [Employee])

(def StatEntry {:it s/Int,
                :turnover-count s/Int,
                :level-count {SkillLevel s/Int}})

(def transition-for-level {:a {:stay {:stay 0.6, :leave 0.4}, :leave {:stay 0.1, :leave 0.9}},
                           :b {:stay {:stay 0.7, :leave 0.3}, :leave {:stay 0.2, :leave 0.8}},
                           :c {:stay {:stay 0.8, :leave 0.2}, :leave {:stay 0.3, :leave 0.7}},
                           :d {:stay {:stay 0.9, :leave 0.1}, :leave {:stay 0.4, :leave 0.6}},
                           :f {:stay {:stay 0.95, :leave 0.05}, :leave {:stay 0.5, :leave 0.5}}})

(s/defn one-of :- s/Any
  [values :- [s/Any]]
  (let [v (if (vector? values) values (vec values))
        c (count v)
        r (rand-int c)]
    (get v r)))

(def emp-id-gen (atom 0))
(s/defn jobseeker-gen :- Employee
  "Generate random job seekers of varying quality"
  [hire-level :- [SkillLevel]]
  (let [level (one-of hire-level)]
    {:id         (swap! emp-id-gen inc),
     :level      level,
     :state      :stay,
     :transition (get transition-for-level level)}))

(s/defn boost-transition :- TransitionProbability
  [transitions :- TransitionProbability
   boost-stay :- s/Bool
   boost-level :- double]
  (let [stay-stay (-> transitions :stay :stay)
        stay-leave (-> transitions :stay :leave)
        leave-stay (-> transitions :leave :stay)
        leave-leave (-> transitions :leave :leave)
        stay-sum (+ stay-stay stay-leave)
        leave-sum (+ leave-stay leave-leave)]
    (if boost-stay
      ; Boost stay
      (let [stay-stay (* stay-stay boost-level)
            stay-leave (- stay-sum stay-stay)
            leave-stay (* leave-stay boost-level)
            leave-leave (- leave-sum leave-stay)]
        {:stay {:stay stay-stay, :leave stay-leave}, :leave {:stay leave-stay, :leave leave-leave}})
      ; Boost leave
      (let [stay-leave (* stay-leave boost-level)
            stay-stay (- stay-sum stay-leave)
            leave-leave (* leave-leave boost-level)
            leave-stay (- leave-sum leave-leave)]
        {:stay {:stay stay-stay, :leave stay-leave}, :leave {:stay leave-stay, :leave leave-leave}}))))

(s/defn maybe-transition :- Employee
  "Take an employee and possibly change the state given the transitions"
  [employee :- Employee
   stay-leave-boost :- double
   staff-stats :- StatEntry]
  (let [level-count (:level-count staff-stats)
        level (:level employee)
        at-or-above-level (apply + (map second (filter #(<= (compare (first %) level) 0) level-count)))
        below-level (apply + (map second (filter #(> (compare (first %) level) 0) level-count)))
        more-good-than-bad (> at-or-above-level below-level)
        state (-> employee :state)
        transition (-> employee :transition (boost-transition more-good-than-bad stay-leave-boost) (get state))
        prob-sum (reduce + (vals transition))
        new-state (if (< (rand prob-sum) (:stay transition)) :stay :leave)]
    (assoc employee :state new-state)))

(s/defn level-up :- Employee
  "Return employee with one level up, unless already at top"
  [employee :- Employee]
  (let [current-level (:level employee)
        sorted-levels (into (sorted-set) (:vs SkillLevel))
        one-better (or (last (subseq sorted-levels < current-level)) (first sorted-levels))]
    (assoc employee :level one-better)))

(s/defn level-down :- Employee
  "Return employee with one level down, unless already at bottom"
  [employee :- Employee]
  (let [current-level (:level employee)
        sorted-levels (into (sorted-set) (:vs SkillLevel))
        one-worse (or (first (subseq sorted-levels > current-level)) (last sorted-levels))]
    (assoc employee :level one-worse)))

(def company (atom {}))
(s/defn add-employee! :- s/Bool
  "Add employee to company, returning true if employee among staff after update"
  [max-employees :- s/Int
   employee :- Employee]
  (let [id (-> employee :id)]
    (-> (swap! company
               ; Only add new employee if there's a free spot, or just an update to existing employee
               (fn [m k v] (if (or (< (count m) max-employees) (not (nil? (get m k)))) (assoc m k v) m))
               id employee) (get id) nil? not)))

(s/defn remove-employee! :- Staff
  "Remove employee from company, return complete set of staff"
  [employee :- Employee]
  (let [id (-> employee :id)]
    (vals (swap! company dissoc id))))

(s/defn get-employees :- Staff
  "Get all current employees"
  []
  (vals @company))

(s/defn create-stat-entry :- StatEntry
  "Take quitters and staff and create a stat entry"
  [iteration :- s/Int,
   quitters :- [Staff],
   workers :- [Staff]]
  (let [quit-count (count quitters)
        worker-level-groups (group-by :level workers)
        worker-level-counts (into {} (map #(vec [(first %) (count (second %))]) worker-level-groups))]
    {:it iteration,
     :turnover-count quit-count,
     :level-count worker-level-counts}))

(defn init-org-sim [param-chan output-chan]
  (let [new-hires (async/chan (async/dropping-buffer 10))
        employee-count 1000
        transition-stats-cache (atom nil)
        it (atom 0)]
    (go (while true (do (let [params (async/<! param-chan)]
                          (reset! parameters params)))))

    (go (do
          ; Fill company first
          (let [hire-levels (map first (filter second (:hire @parameters)))]
            (if (-> hire-levels empty? not)
              (while (add-employee! employee-count (jobseeker-gen hire-levels)))))
          ; Send early stats
          (let [stats (create-stat-entry (swap! it inc) [] (get-employees))]
            (async/>! output-chan {:stats stats
                                   :quitters []}))
          ; Then use channel for normal ops..
          (while true (do
                        (let [hire-levels (map first (filter second (:hire @parameters)))]
                          (if (and (not (:pause @parameters)) (-> hire-levels empty? not))
                            (async/>! new-hires (jobseeker-gen hire-levels))))
                        (async/<! (async/timeout 1))))
      )
    )

    (letfn [(set-stats [s] (reset! transition-stats-cache s))
            (get-stats [] (or @transition-stats-cache (create-stat-entry 0 [] (get-employees))))
            (transition-employees! [e] (add-employee! employee-count (maybe-transition e (+ 1 (/ (:gdb @parameters) 100)) (get-stats))))
            (transition-level-up! [e] (add-employee! employee-count (level-up e)))
            (transition-level-down! [e] (add-employee! employee-count (level-down e)))
            (fire-employee! [e] (add-employee! employee-count (assoc e :state :leave)))
            (is-leaving? [e] (= :leave (get e :state)))
            (is-high-skill? [e] (some (set (list (-> e :level))) (map first (filter second (:highskill @parameters)))))
            (is-low-skill? [e] (some (set (list (-> e :level))) (map first (filter second (:fire @parameters)))))]
      (go (async/<! (async/timeout 100))                    ; We pause here to let initial stats above go out earlier..
          (while true
            (doall (map transition-employees! (take (int (* employee-count (/ (:turnover @parameters) 100))) (shuffle (or (get-employees) '())))))
            (doall (map transition-level-up! (take (int (* employee-count (/ (:levelup @parameters) 100))) (shuffle (or (get-employees) '())))))
            (doall (map transition-level-down! (take (int (* employee-count (/ (:leveldown @parameters) 100))) (shuffle (or (get-employees) '())))))
            (doall (map fire-employee! (take (int (* employee-count (/ (:fireturnover @parameters) 100))) (filter is-low-skill? (or (get-employees) '())))))
            (doall (map transition-employees! (filter #(and (is-leaving? %) (is-high-skill? %)) (get-employees))))
            (let [have-quit (filter is-leaving? (get-employees))]
              (doall (map remove-employee! have-quit))
              (while (add-employee! employee-count (async/<! new-hires)))
              (let [stats (create-stat-entry (swap! it inc) have-quit (remove is-leaving? (get-employees)))]
                (set-stats stats)
                (async/>! output-chan {:stats stats
                                       :quitters have-quit})))))
    )
  )
)

;;; Web worker setup stuff
(defn create-web-worker [worker-script]
  (let [worker (js/Worker. worker-script)
        input-chan (async/chan)
        output-chan (async/chan (async/sliding-buffer 50))]
    (letfn [(worker-listener [event] (go (async/>! output-chan (js->clj (.-data event) :keywordize-keys true))))]
      (.addEventListener worker "message" worker-listener)
      (go (while true (.postMessage worker (clj->js (async/<! input-chan))))))
    [worker input-chan output-chan]))

(defn init-web-worker [input-chan output-chan]
  (letfn [(input-listener [event] (go (async/>! input-chan (js->clj (.-data event) :keywordize-keys true))))]
    (set! (.-onmessage js/self) input-listener)
    (go (while true (.postMessage js/self (clj->js (async/<! output-chan)))))))

(defn wire-web-worker-task []
  (let [input-chan (async/chan)
        output-chan (async/chan)]
    (enable-console-print!)
    (init-web-worker input-chan output-chan)
    (init-org-sim input-chan output-chan)))

;;; Browser window on-load stuff
(defn window-load []
  (enable-console-print!)
  (let [wwc (create-web-worker "cljs.js")
        worker-input-chan (nth wwc 1)
        worker-output-chan (nth wwc 2)
        joiner-color-chan (async/chan (async/sliding-buffer 5))
        employee-color-chan (async/chan (async/sliding-buffer 5))
        quitters-color-chan (async/chan (async/sliding-buffer 5))
        stat-entry-limit 50
        stat-entries (atom [])
        last-quitters (atom [])]

      (letfn [(stat-entry->historical-data [stat-entry]
              (let [level-count (:level-count stat-entry)]
                (vec [(str "Iteration " (get stat-entry :it 0))
                       (get level-count :f 0)
                       (get level-count :d 0)
                       (get level-count :c 0)
                       (get level-count :b 0)
                       (get level-count :a 0)])))
              (quitters->color [employee]
                (let [level (:level employee)]
                  (condp = level
                    "a" :blue
                    "b" :green
                    "c" :yellow
                    "d" :red
                    "f" :grey)))
              (level->color [level]
                (condp = level
                  :a :blue
                  :b :green
                  :c :yellow
                  :d :red
                  :f :grey))
              (param-hire-colors []
                (let [hire (:hire @parameters)
                      hire-levels (map first (filter second hire))]
                  (map level->color hire-levels)))
              (level-count->colors [[level count]]
                (condp = level
                  :a (repeat (/ count 10) :blue)
                  :b (repeat (/ count 10) :green)
                  :c (repeat (/ count 10) :yellow)
                  :d (repeat (/ count 10) :red)
                  :f (repeat (/ count 10) :grey)))]

        (init-menu)

        ; Road in
        (walker-right 0 78 "worker-left-road" joiner-color-chan)

        (go
          ; Delay the building workers, for visual effect..
          (async/<! (async/timeout 8000))

          ; Floors, walking right
          (walker-right 85 237 "worker-1" employee-color-chan)
          (walker-right 85 237 "worker-2" employee-color-chan)
          (walker-right 85 237 "worker-3" employee-color-chan)
          (walker-right 85 237 "worker-4" employee-color-chan)

          ; Floors, walking left
          (walker-left 85 237 "worker-1" employee-color-chan)
          (walker-left 85 237 "worker-2" employee-color-chan)
          (walker-left 85 237 "worker-3" employee-color-chan)
          (walker-left 85 237 "worker-4" employee-color-chan)

          ; Road out
          (walker-right 243 321 "worker-right-road" quitters-color-chan))

        (add-watch parameters :param-to-worker (fn [_ _ _ params]
                                                 (go (async/>! worker-input-chan params))))

        (go (while true (do
                          (let [colors (param-hire-colors)]
                            (if (and (not (:pause @parameters)) (-> colors empty? not))
                              (async/>! joiner-color-chan (first (shuffle colors)))))
                          (async/<! (async/timeout 10)))))

        (go (while true (let [from-worker (async/<! worker-output-chan)
                              stats (:stats from-worker)
                              quitters (:quitters from-worker)]
                          (reset! stat-entries (vec (take-last stat-entry-limit (conj @stat-entries stats))))
                          (reset! last-quitters quitters)
                          (async/<! (async/timeout 1)))))

        (go (while true (do
                          (if (and (not (:pause @parameters)) (-> @last-quitters empty? not))
                            (let [quitters-color (map quitters->color @last-quitters)]
                              (async/>! quitters-color-chan (first (shuffle quitters-color)))))
                          (async/<! (async/timeout 100)))))

        (go (while true (do
                          (if (and (not (:pause @parameters)) (-> @stat-entries empty? not))
                            (let [last-stat-entry (last @stat-entries)
                                  level-count (:level-count last-stat-entry)
                                  shuffled-colors (shuffle (mapcat level-count->colors level-count))]
                              (async/onto-chan employee-color-chan (take 10 shuffled-colors) false)))
                          (async/<! (async/timeout 100)))))

        (go (while true (do
                          (if (and (not (:pause @parameters)) (-> @stat-entries empty? not))
                            (let [data (reduce conj [["Style", "F player", "D player", "C player", "B player", "A player"]]
                                               (map stat-entry->historical-data @stat-entries))]
                              (update-chart (clj->js data))))
                          (async/<! (async/timeout 50)))))

        (go (while true (do
                          (if (and (not (:pause @parameters)) (-> @stat-entries empty? not))
                            (let [last-level-count (:level-count (last @stat-entries))
                                  data (reduce conj [["Skill level", "Count"]]
                                               (map #(vec [%1 (get last-level-count %2)]) ["A player" "B player" "C player" "D player" "F player"] [:a :b :c :d :f]))]
                              (update-pie (clj->js data))))
                          (async/<! (async/timeout 50)))))
      )
    )
  )

;;; Init web worker or main thread
(if (undefined? (.-document js/self))
  (wire-web-worker-task)
  (set! (.-onload js/window) window-load))
