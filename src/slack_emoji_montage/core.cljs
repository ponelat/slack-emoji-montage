

(ns slack-emoji-montage.core
  (:require [rum.core :as rum]
            [slack-emoji-montage.emoji-data :as data]))


(enable-console-print!)


;; define your app data so that it doesn't get over-written on reload
(def initial-emoji-grid (vec  (map #(vec (repeat 16 :blank)) (repeat 10 nil))))

(defonce app-state (atom 
                    {:current-emoji :ponelat
                     :current-emoji-filter ""
                     :emoji-grid initial-emoji-grid}))

(def *current-emoji (rum/cursor app-state :current-emoji ))
(def *current-emoji-filter (rum/cursor app-state :current-emoji-filter ))

(def *emoji-grid (rum/cursor app-state :emoji-grid ))

;;;; To lazy to write a reducer, keeping all state fns here...
(defn swap-emoji! [ x y emoji-key]
  (swap! *emoji-grid (fn [grid] (assoc-in grid [y x] emoji-key))))

(defn clear-grid! []
  (reset! *emoji-grid initial-emoji-grid))


(defn toggle-emoji-with-current! [x y]
  (swap-emoji! x y (if (= :blank (get-in @app-state [:emoji-grid y x] :blank)) (:current-emoji @app-state) :blank)))
;;;; End of state reducers


(defn resolve-alias
  [m k]
  "Will follow the 'alias:<string>' strings until ( not bounded ) it reaches a non-alias"
  (let [groups (clojure.string/split ((keyword k) m) #"^alias:")]
    (if (> (count groups) 1)
      (resolve-alias m (groups 1))
      (groups 0))))

(defn map-matrix [f coll]
  (map-indexed (fn [i v] (map-indexed #(f %2 [i %1]) v)) coll))

(defn blank? [k] (= :blank k))
                      
(rum/defc emoji-img [k props]
  [:img
   (into props
         {:class (if (blank? k) "is-blank cell" "cell")
          :alt (str k)
          :src (resolve-alias data/emoji k)})])

(defn emoji-cell [k [x y]]
  (emoji-img k {
                :key (str x \, y)
                :on-click (fn [_] (toggle-emoji-with-current! x y))}))

(rum/defc render-emoji-row
  [y row]
  [:div
   (map-indexed (fn [x k] (emoji-cell k [x y])) row)])

(rum/defc render-emoji-grid < rum/reactive []
  [:div.grid
   (map-indexed render-emoji-row (rum/react *emoji-grid))])

(defn filter-matches
  [coll substr]
  (filter (fn [s] (clojure.string/includes? (str  s) substr)) coll))


(rum/defc render-matched-emoji [s match]
  [:span.matched-string
   (emoji-img s {:key s
                 :on-click (fn [_] (reset! *current-emoji s) (reset! *current-emoji-filter ""))})])

(defn filtered-list
  [coll s]
  (map render-matched-emoji (filter-matches coll s)))

(rum/defc render-emoji-selector < rum/reactive []
  [:form 
   [:input {:type "text" :on-change (fn [e] (reset! *current-emoji-filter  (.. e -target -value)))}]
   [:.filtered-list
    (filtered-list (keys data/emoji) (rum/react *current-emoji-filter))]])

(rum/defc render-current-emoji < rum/reactive []
  [:div.current-emoji
   [:span ["Current Emoji: "]]
   (emoji-img (rum/react *current-emoji) {:class "current-emoji"})])


(rum/defc render-toolbar []
  [:div.toolbar
   [:button.clear-grid {
                        :on-click #(clear-grid!)}
    "Wipe"]])


(defn trim-row [coll]
  (reverse (drop-while blank? (reverse coll))))

(defn trim-rows [grid]
  (map trim-row (reverse (drop-while (partial every? blank?) (reverse grid)))))


(defn grid-keywords->slack [grid]
   (clojure.string/join \newline
     (map
       (fn [row]
         (clojure.string/join
                   (map (fn [k] (str k ":")) row)))
       (trim-rows grid))))


(rum/defc render-emoji-text < rum/reactive []
  [:textarea {:value (grid-keywords->slack (rum/react *emoji-grid))
              :on-click #(.select (.. %1 -target))}])
  
(rum/defc app []
  [:.container
   [:header [:h1 ["The emoji maker 3000"]]]
   (render-current-emoji)
   (render-toolbar)
   (render-emoji-grid)
   (render-emoji-selector)
   (render-emoji-text)])

(rum/mount (app)
           (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
