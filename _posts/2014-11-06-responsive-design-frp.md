---
layout: post_page
title: "Responsive Reactive"
---
When I [left off](/2014/01/27/responsive-design-csp.html) so long ago,
I was discussing David Nolen's post
[*CSP is Responsive Design*](http://swannodette.github.io/2013/07/31/extracting-processes/).
In it, Nolen suggests the following concerns as a replacement to the standard MVC:

  1. Event Stream Processing
  2. Event Stream Coordination
  3. Interface Representation

While these are reasonable concerns, they are not at all fundamental
to responsive applications. Much more fundamental in my mind are:

  1. Data Modeling
  2. Data Updates (Stream Processing and Coordination)
  3. Data Rendering

I happen to agree that an event stream is one of the best abstractions
we have for making stable updates to a model, but we should be clear about their place.
Event streams and interfaces are both ancillary concerns. They are a means to an end.
The end is always data.

Unfortunately, those concerns could easily be mapped to Model, View, and Controller.
Indeed, the (deeply) underlying principles of MVC are sound. It's in the execution
that every MVC library I've ever seen goes astray. MVC apps tend to devolve into
messes of two-way binding, data hiding, and unstable mutation. So an important
zeroth concern, the thing the predicates everything else, is that we should always
deal with [*stable*](http://clojure.org/state#toc3) data.

Let's take last post's example of a submenu to see how this looks in Bacon.js.

---

#### Data Modeling

We'll model our submenu as a map of

  * `:items`: The list of items to display
  * `:highlighted`: The currently highlighted item index
  * `:selected`: The currently selected item index

```clojure
(defn init-model [items]
  (let [model (bjb/combine-model
                {:items       items
                 :highlighted "none"
                 :selected    "none"})]
    (-> model
        ->clj
        (b/filter (comp empty? :items))
        (b/skip-duplicates =)
        (b/map #(assoc % :highlighted "none" :selected "none"))
        ->js
        (->> (bjb/add-source model)))
    model))
```

We also use the initializer to set up a rule that resets `:highlighted` and `:selected`
whenever the `:items` are empty.

---

#### Data Updates

First, we define a function that can be used to dispatch events to functions
that will update specific attributes of our model.

```clojure
(defn update-model! [model events event-key update-key next-fn]
  (-> (b/filter events (comp not nil? #{event-key} first))
      (b/map (b/combine-as-array model events))
      ->clj
      (b/map (fn [[m e :as v]] (assoc m update-key (next-fn v))))
      ->js
      (->> (bjb/add-source model))))
```

Events will come in as tuples of `[event-type event-data]`.

We then set up our update handlers.

```clojure

(defn init-events! [model events]
  (update-model! model events :next :highlighted
                 (fn [[{:keys [items highlighted]} _e]]
                   (if (= "none" highlighted)
                     0
                     (mod (inc highlighted) (count items)))))

  (update-model! model events :prev :highlighted
                 (fn [[{:keys [items highlighted]} _e]]
                   (if (= "none" highlighted)
                     (count items)
                     (mod (dec highlighted) (count items)))))

  (update-model! model events :clear-highlight :highlighted
                 (constantly "none"))

  (update-model! model events :highlight :highlighted
                 (fn [[{:keys [highlighted]} [_k index]]]
                   (if (or (number? index) (= "none" index))
                     index
                     highlighted)))

  (update-model! model events :select :selected
                 (fn [[{:keys [highlighted]} _e]]
                   highlighted))

  (update-model! model events :append :items
                 (fn [[{:keys [items]} [_k item]]]
                   (if (empty? item)
                     []
                     (conj items item)))))
```

---

#### Data Rendering

We can now create a menu constructor that takes an initial item list,
a stream of events to respond to, and a render function.

```clojure
(defn menu [items events render]
  (let [model (init-model items)]
    (init-events! model events)
    (-> model
        ->clj
        (b/skip-duplicates =)
        (b/on-value
          (fn [{:keys [items highlighted selected]}]
            (render items highlighted selected))))
    model))
```

And now the fruits of our labor:

```clojure
(let [$div ($ :div#array-highlight)
      $pre ($ :pre#array-highlight-list $div)]
  (menu
    ["so" "it" "goes"]
    (key-events $div)
    (fn [items highlighted selected]
      (->> (map
             (fn [i item]
               (str
                 (if (= i highlighted) ">" " ")
                 (if (= i selected) "*" " ")
                 "  "
                 item))
             (iterate inc 0)
             items)
           (str/join "\n")
           (j/text $pre)))))
```
<div id="array-highlight" class="example-select">
  <pre id="array-highlight-list">
  </pre>
</div>

Notice how we've completely decoupled underlying data, event processing,
and rendering from the top to the bottom. Due to our architecture, we are
able to slip in a completely different rendering function along with mouse
events with ease.

```clojure
(defn leavestream [$ul]
  (-> $ul
      bjb/mouseleaveE
      (b/map (constantly [:clear-highlight]))))

(defn overstream [$ul]
  (-> $ul
      bjb/mouseoverE
      (b/map #($ (.-target %)))
      (b/filter #(j/is % "li"))
      (b/map #(vector :highlight (.index %)))))

(defn hover-events [$ul]
  (-> $ul
      bjb/clickE
      (b/map (constantly [:select]))
      (b/merge-all (leavestream $ul) (overstream $ul) (key-events $ul))))

(let [$elem ($ :ul#ul-highlight-select-list)]
  (menu
    ["So"
     "say"
     "we"
     "all"]
    (hover-events $elem)
    (fn [items highlighted selected]
      (j/html $elem "")
      (doseq [[item i] (map vector items (iterate inc 0))
              :let [$item ($ (t/li item (= i highlighted) (= i selected)))]]
        (j/append $elem $item)))))
```
<div id="ul-highlight-select" class="example-select">
   <ul id="ul-highlight-select-list">
   </ul>
</div>

---

#### Incremental Drawing

So far, we've just completely redrawn our element on every change. While that's
the most straightforward solution, many times it is too slow. So let's change it
to do incremental updates to the DOM.

First we change our constructor to return a series of streams that contain
the individual changes to our model.

```clojure
(defn menu-incremental [items events]
  (let [model (init-model items)
        items (bjb/lens model "items")
        highlight (-> (bjb/lens model "highlighted") (b/sliding-window 2))
        select (-> (bjb/lens model "selected") (b/sliding-window 2))]
    (init-events! model events)

    {:items       items
     :highlight   (b/map highlight second)
     :unhighlight (b/map highlight first)
     :select      (b/map select second)
     :unselect    (b/map select first)}))
```

Notice that we're not changing the underlying model. We're simply changing
how we look at that model. Due to the fact that we have a stream of stable values,
we can easily view the history of an item with `b/sliding-window`.

Then we can simply bind simple DOM updates to those streams.

```clojure
(defn bind-li-changes! [$elem stream update-fn]
  (-> stream
      (b/filter (partial not= "none"))
      (b/filter (comp not nil?))
      (b/map #($ (str "li:nth-child(" (inc %) ")") $elem))
      (b/on-value update-fn)))

(let [$elem ($ :ul#incremental-list)]
  (let [updates (menu-incremental
                  ["You're"
                   "a"
                   "Hufflepuff"
                   "Harry"]
                  (hover-events $elem))]
    (-> (:items updates)
        ->clj
        (b/skip-duplicates =)
        (b/on-value
          (fn [items]
            (j/html $elem "")
            (doseq [item items]
              (j/append $elem ($ (t/li item false false)))))))

    (bind-li-changes! $elem (:highlight updates) #(j/add-class % "highlighted"))
    (bind-li-changes! $elem (:unhighlight updates) #(j/remove-class % "highlighted"))
    (bind-li-changes! $elem (:select updates) #(j/add-class % "selected"))
    (bind-li-changes! $elem (:unselect updates) #(j/remove-class % "selected"))))
```
<div id="incremental" class="example-select">
   <ul id="incremental-list">
   </ul>
</div>

This also allows us to watch for changes to `:items` and add to the list
dynamically.

```clojure
(defn item-events [& items]
  (-> (b/repeatedly 1000 (clj->js items))
      (b/map (partial vector :append))))

(let [$elem ($ :ul#incremental-changes-list)]
  (let [events (hover-events $elem)
        item-events (item-events "Time" "to" "toss" "the" "dice" [])
        updates (menu-incremental [] (b/merge events item-events))]
    (-> (:items updates)
        (b/map (b/combine-as-array (:items updates) (:highlight updates) (:select updates)))
        ->clj
        (b/skip-duplicates =)
        (b/on-value
          (fn [[items highlighted selected]]
            (j/html $elem "")
            (doseq [[item i] (map vector items (iterate inc 0))
                    :let [$item ($ (t/li item (= i highlighted) (= i selected)))]]
              (j/append $elem $item)))))

    (bind-li-changes! $elem (:highlight updates) #(j/add-class % "highlighted"))
    (bind-li-changes! $elem (:unhighlight updates) #(j/remove-class % "highlighted"))
    (bind-li-changes! $elem (:select updates) #(j/add-class % "selected"))
    (bind-li-changes! $elem (:unselect updates) #(j/remove-class % "selected"))))
```
<div id="incremental-changes" class="example-select">
   <ul id="incremental-changes-list">
   </ul>
</div>

I just want to reiterate that we were able to make these major changes to the way
our element is drawn without touching the underlying data model or the underlying event
system. All we had to do was get another view of our data using lenses and sliding windows.
Everything else was DOM work.

I could certainly see an argument for making an interface so you don't
force users to individually bind changes. However, for us that is a completely
trivial API decision compared to the overall architecture.

---

### Why does this even matter?

A lot has changed since I wrote the first part of this post, especially with the rise of
[React.js](http://facebook.github.io/react/) and the [myriad](https://github.com/swannodette/om)
[of](https://github.com/holmsand/reagent) [clojurescript](https://github.com/levand/quiescent)
wrappers that have popped up for it over the past year.

Nobody, including myself, is advocating that we go back to the days of selectors and
explicit DOM manipulation. However, the underlying principles of this architecture is
still valid. The real power of React.js is that it takes care of rendering your data.
We now have the power to say, "Here's my data. Draw it. And do it fast."

React has a lot to say about state management as well, but their system complects
modeling, updates, and rendering. Those are each separate concerns, and there is
a lot of power to be leveraged in separating them.

OM cursors help alleviate the problem of modeling. However, they still complect
model updates and rendering. Even in
[OM's Advanced Tutorial](https://github.com/swannodette/om/wiki/Advanced-Tutorial),
you see examples of state updates in the middle of the render method. The problem
with this approach is it becomes difficult to determine which component should
be responsible for handling state updates. Is it okay that the lowest component
updates state? What if somebody else is looking at that state? If you move it up,
will you be able to find it in the future?

Even in [Nolen's example from June 2013](https://github.com/swannodette/swannodette.github.com/blob/master/code/blog/src/blog/responsive/core.cljs#L207-L214),
he was advocating for braiding rendering and updating.

As we saw in this example, it is possible to completely separate the concerns of
data rendering and data updating. None of our rendering code was dependant on
how and when we choose to update our model. We should aspire for the same
clean separation using today's tools.

<script type="text/javascript" src="/js/jquery.min.js"></script>
<script type="text/javascript" src="/js/bacon.js"></script>
<script type="text/javascript" src="/js/bacon-model.js"></script>
<script type="text/javascript" src="/js/bacon-jquery.js"></script>
<script type="text/javascript" src="/js/responsive-design-frp.js"></script>
