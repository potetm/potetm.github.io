(ns blog.responsive-design.template
  (:require dommy.template)
  (:require-macros [dommy.macros :refer [deftemplate]]))

(deftemplate li [label]
  [:li label])

(deftemplate ul []
  [:ul])
