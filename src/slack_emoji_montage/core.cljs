(ns slack-emoji-montage.core
  (:require [rum.core :as rum]
            [slack-emoji-montage.emoji-data :as data]))


(enable-console-print!)

(println "This text is printed from src/slack-emoji-montage/core.cljs. Go ahead and edit it and see reloading in action.")

;; define your app data so that it doesn't get over-written on reload

(def initial-emoji-grid (vec  (map #(vec (repeat 16 :blank)) (repeat 10 nil))))
                         
(defonce app-state (atom 
                     {:current-emoji :ponelat
                      :emoji-grid initial-emoji-grid}))

;;;; To lazy to write a reducer, keeping all state fns here...
(defn swap-emoji! [x y emoji-key]
  (swap! app-state (fn [v] (assoc-in v [:emoji-grid x y] emoji-key))))
(defn toggle-emoji-with-current! [x y]
  (swap-emoji! x y (if (= :blank (get-in @app-state [:emoji-grid x y] :blank)) (:current-emoji @app-state) :blank)))
;;;; End of state reducers


(defn resolve-alias
  [coll k]
  "Will follow the 'alias:<string>' strings until ( not bounded ) it reaches a non-alias"
  (let [groups (clojure.string/split (k coll) #"^alias:")]
    (if (> (count groups) 1)
      (resolve-alias coll (keyword (groups 1)))
      (groups 0))))

(defn map-matrix [f coll]
  (map-indexed (fn [i v] (map-indexed #(f %2 [i %1]) v)) coll))

(defn blank? [k] (= :blank k))
                      
(rum/defc emoji-img [k [x y]]
  [:img.cell
   {:key (str x "," y)
    :class (if (blank? k) "is-blank" nil)
    :src (resolve-alias data/emoji k)
    :on-click (fn [_] (toggle-emoji-with-current! x y))}])
  
                      
(rum/defc render-emoji-grid < rum/reactive []
  [:div.grid
   (map-matrix emoji-img (:emoji-grid (rum/react app-state)))])
  
  
(rum/defc app []
  [:.container
   [:header [:h1 ["The emoji maker 3000"]]]
   [ (render-emoji-grid)]])

(rum/mount (app)
           (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
