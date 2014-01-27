(ns blog.responsive-design-frp.template
  (:require dommy.template)
  (:require-macros [dommy.macros :refer [deftemplate]]))

(deftemplate li [label highlighted? selected?]
  [:li {:class (str (when highlighted? "highlighted ")
                    (when selected? "selected "))}
   label])
