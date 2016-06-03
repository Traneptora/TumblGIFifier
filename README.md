# TumblGIFifier
Automatically Creates GIFs from Videos sized to Tumblr's liking.
Tumblr has a maximum GIF size of 2.0 MB, and TumblGIFifier will automatically resize it as close as it can to that threshold.

Warning:
TumblGIFifier is still in its early stages of development. The UI is a bit clunky and there WILL be undiscovered bugs.

## For Ordinary Users

If you don't want to compile it from source, click the "Releases" button to the upper-right, and download the most recent version.

## Compilation

The first time you clone this repository, you need to grab `xz-java`. To do this, run:

	$ git submodule init && git submodule update

This will pull in the XZ Java submodule. Then, to compile, run:

	$ ant

To clean the build, run:

	$ ant clean


