---
layout: post_page
title: "David Nolen's Responsive Design"
---
In my [last post](http://potetm.github.io/2014/01/07/frp.html), I stated that I was going to use
[bacon.js](https://github.com/baconjs/bacon.js) to work through the CSP posts from [David Nolen's awesome
blog](http://swannodette.github.io/).  My goal is to evaluate the strengths and weaknesses of FRP and CSP.  Well it didn't take long to get
somewhere.

In [this post](#recursive-reference-suckas!), I'm going to walk through my solutions to [Nolen's second CSP
article](http://swannodette.github.io/2013/07/31/extracting-processes/) so that you can see some of the differences between bacon and
core.async.

Before we jump in, I want to have a quick digression into the way I want to evaluate the two paradigms.  In my mind, there are three major
components of a paradigm:

  1. Syntax - The way the code *looks*
  2. Semantics - What the syntax *means*
  3. Function - What actually *happens* on execution

Each component is important in its own way, and the balance between them is what, in part, defines a paradigm, language, library, etc.  For
example, a language may have semantic and functional depth, but often at the expense of syntactic simplicity (think Java).  Or two languages
might be syntactically similar, yet have important semantic differences (think generics in Java vs C#).  Or, as in our case, two libraries
may have very different syntax and semantics, yet are functionally identical.

Keep those components in mind as you go through and compare this post with Nolen's.  At the end of the post I'll try to pull it all
together.

Let's get started.

---

The crux of Nolen's article is the assertion that there are three sets of abstractions which are more fundamental than the traditional Model
View Controller:

  1. Event Stream Processing
  2. Event Stream Coordination
  3. Interface Representation

Thus our journey begins as his did: with an interface definition.

```clojure
(defprotocol IHighlightable
  (-highlight! [list n])
  (-unhighlight! [list n]))
```

And, true to Nolen's design, next comes the definition of an event stream.

```clojure
(defn keystream [$elem]
  (let [mousein (b/map (bjb/mouseenterE $elem) true)
        mouseout (b/map (bjb/mouseleaveE $elem) false)
        has-focus? (bjb/model false)]
    (-> (b/merge mousein mouseout)
        (->> (bjb/add-source has-focus?)))

    (-> ($ js/document)
        bjb/keydownE
        (b/filter has-focus?)
        (b/do-action j/prevent)
        (b/map keycode)
        (b/filter (comp not nil? KEYS))
        (b/map key->keyword))))
```

Lastly, there's the event stream coordination bit.

```clojure
(defn highlighter-filter [v]
  (or (not (nil? (#{:next :previous :clear} v))) (number? v)))

(defn highlighter
  ([in list]
   (highlighter in list (b/constant true)))
  ([in list control?]
   (let [cur (bjb/model "none")
         prev-cur (b/sliding-window cur 2 2)
         out (b/bus)]
     (-> prev-cur
         (b/on-value
           (fn [[prev cur]]
             (when (number? prev)
               (-unhighlight! list prev))
             (when (not= cur :clear)
               (-highlight! list cur))
             (b/push out cur))))

     (-> in
         (b/filter highlighter-filter)
         (b/map (b/combine-with in cur (partial next-val list)))
         (->> (bjb/add-source cur)))

     (-> (b/filter in (comp not highlighter-filter))
         (b/merge out)))))
```

Now, let's put it all to good use.

```clojure
(defn set-char! [s i c]
  (str (.substring s 0 i) c (.substring s (inc i))))

(extend-type array
  IHighlightable
  (-highlight! [list n]
    (aset list n (set-char! (aget list n) 0 ">")))
  (-unhighlight! [list n]
    (aset list n (set-char! (aget list n) 0 " "))))

(let [$div ($ :div#array-highlight)
      $pre ($ :pre#array-highlight-list $div)
      list (array "   Alan Kay"
                  "   J.C.R. Licklider"
                  "   John McCarthy")
      events (keystream $div)
      render #(j/text $pre (.join list "\n"))
      action #(highlighter % list)]
  (create-example events render action))
```
<div id="array-highlight" class="example-select">
  <pre id="array-highlight-list">
  </pre>
</div>

I'd like to take a second here to point out that, in this example, both this solution and Nolen's solution are functionally and semantically
*identical*.

Just like Nolen's initial examples, our rendering surface is a plain old JavaScript array.  Just like in his example, we mutate that array
in place, and those modifications are reflected on the screen.

Now, let's add selection into the mix.  Once again, we start with an interface.

```clojure
(defprotocol ISelectable
  (-select! [list n])
  (-unselect! [list n]))
```

We can reuse our previous event stream process.  So all we need is coordination.

```clojure
(defn selector [in list]
  (let [highlighted (b/to-property (b/filter in number?))
        selected (bjb/model nil)
        prev-cur (b/sliding-window selected 2 2)
        out (b/bus)]
    (-> prev-cur
        (b/on-value
          (fn [[prev cur]]
            (when (number? prev)
              (-unselect! list prev))
            (-select! list cur)
            (b/push out cur))))
    (-> in
        (b/filter (partial = :select))
        (b/map highlighted)
        (->> (bjb/add-source selected)))

    (-> (b/filter in (partial not= :select))
        (b/merge out))))
```

And put it all together:

```clojure
(let [$div ($ :div#array-highlight-select)
      $pre ($ :pre#array-highlight-select-list $div)
      list (array "   Smalltalk"
                  "   Lisp"
                  "   Prolog"
                  "   ML")
      events (keystream $div)
      render #(j/text $pre (.join list "\n"))
      action #(selector (highlighter % list) list)]
  (create-example events render action))
```
<div id="array-highlight-select" class="example-select">
  <pre id="array-highlight-select-list">
  </pre>
</div>

Here, friends, is where Nolen and I diverge.  You'll notice that I have two coordination processes operating on the same mutable list:
`#(selector (highlighter % list) list)`.  The issue with that is the bacon.js library doesn't afford any sort of blocking semantics.  What
this means is, if any part of this application were threaded, we would suddenly be at the mercy of the race condition gods.

However, because JavaScript is a single-threaded host, the application that I've written will work exactly as intended.  Therefore, although
there are *semantic* differences between our solutions, *functionally* they are the same.  More on this at the end.

For now, let's move on to his last example.  First we need to extend our event stream to include mouse events.

```clojure
(defn mouseleave [$ul]
  (-> (bjb/mouseleaveE $ul)
      (b/map (constantly :clear))))

(defn mouseover [$ul]
  (-> $ul
      bjb/mouseoverE
      (b/map #($ (.-target %)))
      (b/filter #(j/is % "li"))
      (b/map #(.index %))))

(defn hover-events [$ul]
  (-> $ul
      bjb/clickE
      (b/map (constantly :select))
      (b/merge-all (mouseleave $ul) (mouseover $ul) (keystream $ul))))
```

And now we're ready to put it to good use.

```clojure
(defn do-to-li [list n update-fn]
  (update-fn ($ (str "li:nth-child(" (inc n) ")") ($ list))))

(extend-type js/HTMLUListElement
  ICounted
  (-count [list]
    (.-length ($ :li ($ list))))

  IHighlightable
  (-highlight! [list n]
    (do-to-li list n #(j/add-class % "highlighted")))
  (-unhighlight! [list n]
    (do-to-li list n #(j/remove-class % "highlighted")))

  ISelectable
  (-select! [list n]
    (do-to-li list n #(j/add-class % "selected")))
  (-unselect! [list n]
    (do-to-li list n #(j/remove-class % "selected"))))

(let [$ul ($ :ul#ul-highlight-select-list)
      ul (aget $ul 0)
      events (hover-events $ul)
      action #(selector (highlighter % ul) ul)]
  (create-example events nil action))
```
<div id="ul-highlight-select" class="example-select">
   <ul id="ul-highlight-select-list">
      <li>Gravity's Rainbow</li>
      <li>Swann's Way</li>
      <li>Absalom, Absalom</li>
      <li>Moby Dick</li>
   </ul>
</div>

Oh so sweet and simple.

---

***WARNING: The following content contains stylistic opinions which are inherently subjective.  YMMV.***

Before we talk about core.async and bacon.js, I want to take a second to evaluate the overall design that Nolen is advocating.  What's clear
is that this design isn't necessarily tied to core.async, which means that he is indeed getting close to some fundamental truths.  I think
his method of defining a single pipeline which he calls his *process protocol* is simple and easily extensible.  I also agree that event
coordination should be separated from event stream creation.  This helps provide clarity to complex processes.

However, I disagree that *interface* representation is the third crucial concern.  The third concern is *UI representation*.  Calling the it
interface representation is effectively prescribing the cure before diagnosing the problem.  In the case of this example, interface
representation also ties you in to mutable structures, which I find unnecessarily complex.

In addition, I would pull out the word *stream* from the design as a whole.  FRP and CSP handle events in streams, but that doesn't mean
that's the *only* way to effectively handle events (though I've yet to set a better alternative).

So, in my opinion, a simpler, more fundamental statement of Nolen's trichotomic design would be:

  1. Event Processing
  2. Event Coordination
  3. UI Representation

Separating these three concerns in your design brings a surprising amount of clarity and structure to your code.  Nolen does a good job of
it, but I think the tools in the bacon toolbox allows us to go further.  In my next post, I'll do just that.

---

Now onto core.async and bacon.js.

What I discovered as I worked through Nolen's second post is that the design Nolen advocates is not only possible using FRP, it's quite
straightforward.  That being said, the differences between the two solutions are much deeper than the syntactic level.  There are
fundamental semantic differences between bacon and core.asyn that become very clear once you enter a parallel environment.  As I mentioned
earlier, the lack of blocking semantics in bacon makes it inherently unsafe to do highlighting *and* selection on a mutable structure in a
threaded environment.  With core.async, this is not only alright, it's modus operandi.

However, that does not necessarily mean that core.async is the better tool for all jobs.  On the contrary, I believe there is a great deal
of cognitive overhead associated with its semantic depth, making it comparatively kludgy to use in the browser where parallel processing is
not a concern.

Take, for example, Nolen's selector:

```clojure
(defn selector [in list data]
  (let [out (chan)]
    (go (loop [highlighted ::none selected ::none]
          (let [e (<! in)]
            (if (= e :select)
              (do
                (when (number? selected)
                  (-unselect! list selected))
                (-select! list highlighted)
                (>! out [:select (nth data highlighted)])
                (recur highlighted highlighted))
              (do
                (>! out e)
                (if (or (= e ::none) (number? e))
                  (recur e selected)
                  (recur highlighted selected)))))))
    out))
```

And my selector:

```clojure
(defn selector [in list]
  (let [highlighted (b/to-property (b/filter in number?))
        selected (bjb/model nil)
        prev-cur (b/sliding-window selected 2 2)
        out (b/bus)]
    (-> prev-cur
        (b/on-value
          (fn [[prev cur]]
            (when (number? prev)
              (-unselect! list prev))
            (-select! list cur)
            (b/push out cur))))
    (-> in
        (b/filter (partial = :select))
        (b/map highlighted)
        (->> (bjb/add-source selected)))

    (-> (b/filter in (partial not= :select))
        (b/merge out))))
```

While the core.async example might be slightly more compact, the bacon example is much more--pardon the overused, overloaded
term--declarative.  No looping, no recursing, no nested conditionals.  In fact, this block follows the aforementioned Nolen trichotomic design:

  * The first thread handles UI representation
  * The second thread handles event coordination

The third thread handles the pass-through stream, which isn't strictly necessary.  Processed events are passed in, so that's irrelevant in
this scope.

Given this clarity and power, I think it would be a mistake to disregard FRP in favor of CSP, especially where parallel processing is not a
concern.  In addition, I would need to evaluate how FRP fairs when blocking semantics are built into the library in order to properly
compare FRP to CSP in threaded environments.  I've never had such a need, and thus have never used such a library, though I know [they
exist](https://github.com/Netflix/RxJava/wiki/Blocking-Observable-Operators).  Given what I've found so far, the results of such a
study would be very interesting to me.

<style>
  ol {
    position: relative;
    left: 200px;
    margin-top: 20px;
    margin-bottom: 20px;
  }
</style>

<script type="text/javascript" src="/js/jquery.min.js"></script>
<script type="text/javascript" src="/js/bacon.js"></script>
<script type="text/javascript" src="/js/bacon-model.js"></script>
<script type="text/javascript" src="/js/bacon-jquery.js"></script>
<script type="text/javascript" src="/js/responsive-design-csp.js"></script>
