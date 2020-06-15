(ns fuelsurcharges.views
  (:require [oz.core :as oz]
            [reagent.dom :as rdom]))


(defn group-data [& names]
  (apply concat (for [n names]
                  (map-indexed (fn [i y] {:x i :y y :col n}) (take 20
                                                                   (repeatedly #(rand-int 100)))))))
(group-data "monkey" "slipper" "broom")

(def line-plot
  {:data     {:values (group-data "monkey" "slipper" "broom")}
   :encoding {:x     {:field "x"}
              :y     {:field "y"}
              :color {:field "col" :type "nominal"}}
   :mark     "line"})

(defn header
  []
  [:div
   [:h1 "A template for oz apps"]])

(def app ;; needs an argument to be rendered
  [:div
   [header]
   [oz/vega-lite line-plot]
   ])

(comment (rdom/render [#'app] (.getElementById js/document "app")))
