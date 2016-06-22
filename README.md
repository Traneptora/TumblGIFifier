# TumblGIFifier
Automatically Creates GIFs from Videos sized to Tumblr's liking.
Tumblr has a maximum GIF size of 2.0 MB, and TumblGIFifier will automatically resize it as close as it can to that threshold.

**Warning:**
TumblGIFifier is still in its early stages of development. The UI is a bit clunky and there *WILL* be undiscovered bugs.

## Instructions

To download the most recent version of the program, go to the [releases page](https://github.com/thebombzen/TumblGIFifier/releases), which is the tag symbol in the upper-right. Download the most recent one, which should be at the top.

You need to have at least Java 1.7 to run TumblGIFifier. If you don't have java installed, [download it here](http://www.java.com/) and install it.

TumblGIFifier comes as an executable JAR file.

- To execute the JAR file on Windows, doubleclicking it should work as long as you have Java installed. However, some archive programs might try to open it as an archive. If so, open it with the Java Platform (and you should probably set the Java Platform as the default for opening JAR files anyway).
- To execute the JAR file on a Mac, you need to open it with JAR Launcher. If you try to open it using the default way you might accidentally unzip it and barf a bunch of files all over your desktop. Try not to do that.
- To execute the JAR file on Linux, you really don't need me to finish this sentence.

## Known Problems

- The user interface is somewhat unintuitive, with the text explanation right next to the controls and weird word wrap.
- There's no "how to use" instructions. I need to add a help window explaining the features.
- Sometimes it doesn't work the first time you load up TumblGIFifier. Closing and reopening the program usually fixes this.
- There's no way to update FFmpeg if it's already downloaded, even if they fix a bug and I upload a new version to the dropbox repository.
- If you nudge the sliders very quickly sometimes the screenshots will come up in the wrong order.

## Compilation From Source

The first time you clone this repository, you need to grab `xz-java`. To do this, run:

	$ git submodule init && git submodule update

This will pull in the XZ Java submodule. Then, to compile, run:

	$ ant

To clean the build, run:

	$ ant clean

Note that you have to clean the build if you make a change and then want to recompile.

## Contributing

Feel free! I'll review pull requests you send here.
