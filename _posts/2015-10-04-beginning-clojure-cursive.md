---
layout: post_page
title: "Beginning Clojure: Cursive"
---
One thing I often hear from people getting started with clojure is

> I'm learning clojure, and, since it seems to be the defacto standard for Lisp, I'm also learning Emacs.

Now, I'll just come out and say it: I'm a Vim guy.

When I started learning clojure in 2012, the options were pretty much: use emacs, or use
[vim-fireplace](https://github.com/tpope/vim-fireplace). vim-fireplace is great, but it doesn't provide you
near the power and flexibility of a true REPL. Additionally, I don't find
[paredit emulators in vim](https://github.com/guns/vim-sexp) to be a fluid editing experience.

So, like many, I tried to learn clojure and Emacs at the same time.

I found that experience to be extremely frustrating. Not only was I having to learn and apply new language
concepts, but I constantly felt hampered by my inability to edit text efficiently. Not to mention getting
set up with *any* of the aforementioned tools is non-trivial for a newcomer. To this day, I remember the difficulty
trying to "just fire up and connect to nREPL".

Nowadays most of that pain has been alleviated by [Cursive](https://cursiveclojure.com/). That's especially true
if you're accustomed to using an IDE. You get "simple" things like autocompletion, documentation, suggestions,
and refactoring, all without having to "fire up a nREPL". You get a full-fledged, interactive REPL. And you get
to *choose* what editing mode you prefer. Emacs, Vim, point-and-click, whatever. Cursive's got you covered.

So, now when people tell me "I'm learning clojure," I usually respond:

> Use Cursive, unless you already know Emacs

The problem with saying that is there's still a pretty lengthy install process for an absolute beginner.

Therefore, I decided to put together a little tutorial for clojure newcomers that I can point to when the
question comes up.

#### Install Java
1. Go to [the Java Download Page](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
2. Accept the license agreement
3. Select the appropriate install
4. Note the install location. Example install locations are:
  * Mac: `/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home`
  * Windows: `C:\Program Files\Java\jdk1.8.0_60`
  * Linux: I have no idea. Figure it out. You asked for this :)

#### Install Leiningen
[Leiningen](http://leiningen.org/) is currently the defacto standard for clojure build tools. It's
used for lots of things, but in this example we're only going to use it to generate our project.

If you're on Windows, you should be able to use the [windows installer](http://leiningen-win-installer.djpowell.net)

If you're on Mac/Linux, open a terminal and run:

```sh
echo 'export $PATH=$HOME/bin:$PATH' >> ~/.bashrc
mkdir ~/bin
cd ~/bin
wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein
chmod 755 lein
lein
```

At this point, you can start a repl with `lein repl`. Yay!

#### Create a new clojure project
You should now be able to create your first clojure project. Simply open a terminal window, navigate
to your projects directory, and run:

```sh
lein new app hello-clojure
```

This creates a new directory called `hello-clojure` which contains the scaffolding for an "app"
project. If you want to create a different kind of application, simply replace `app` with the lein
template of your choice. For example, if you want to create a webapp, you might choose
[`chestnut`](https://github.com/plexus/chestnut).

#### Install Cursive
Download and install [IntelliJ](https://www.jetbrains.com/idea/download/). The Free Community
Edition will suffice.

Follow the [Getting Started tutorial](https://cursiveclojure.com/userguide/) to install the Cursive plugin.

After you've installed the plugin, you're going to want to
[set up keybindings for Cursive](https://cursiveclojure.com/userguide/keybindings.html). Keep in mind that
IntelliJ does not allow you to overwrite the template keybindings, so you must first make a copy of
your preferred keymap.

Now that you have Cursive all set up, you can
[import the project you just created](https://cursiveclojure.com/userguide/leiningen.html) and
[set up a REPL](https://cursiveclojure.com/userguide/repl.html) in your project.

**NOTE**: During the project import process, you will be prompted to choose an SDK.

  1. Click the + button to create a new SDK
  2. Select *JDK* from the *Add New SDK* drop down
  3. Navigate to the Java install location you noted during step 1.

You can later access and modify the SDKs you have installed by going to *File → Project Structure → SDKs*.
You can change the SDK for a particular project by going to *File → Project Structure → Project* and
selecting a new *Project SDK*.

#### (Optional) Install IdeaVim
Lastly, we'll mix in all the goodness of vim with the [IdeaVim plugin](https://github.com/JetBrains/ideavim).
Go to *Settings → Plugins → Install JetBrains plugin → IdeaVim*. Click *Install Plugin* and restart.

There are a few gotchas with this plugin, but the one that gets me the most is the ability to use <kbd>Esc</kbd>
from the REPL. One of the IntelliJ defaults is <kbd>Esc</kbd> returns you to the editor. This means that if you're
in the REPL, you have to hit <kbd>Ctrl-c</kbd> if you want to enter navigation mode. You can remap <kbd>Esc</kbd> but
then you lose the ability to jump back to the editor altogether.

#### Profit
And that's it! You now have the JVM and Leiningen installed, know how to set up a new project, and have a powerful
editor.

Feel free to [hit me up on twitter](https://twitter.com/potetm) if there are any corrections or clarifications that
need to be made to this tutorial.
