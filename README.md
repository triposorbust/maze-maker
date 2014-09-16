# maze-maker

DESCRIPTION: A small program to generate simple ASCII mazes.


## Installation

Download from https://github.com/triposorbust/maze-maker. Recommended installation via leiningen.

```
% lein deps
% lein uberjar
% java -jar target/uberjar/maze-maker-0.1.0-standalone.jar -- --help
```


## Usage

Use command-line arguments to specify size and algorithm for generating maze.

```
$ java -jar maze-maker-0.1.0-standalone.jar [options]
```


## Options

Options on command line specify size and generating algorithm.

```
% java -jar target/uberjar/maze-maker-0.1.0-standalone.jar -- --help
Utility program for generating arena maps.

Usage: maze-maker [options]

Options:
  -x, --width N   21     Width of arena
  -y, --height M  11     Height of arena
  -t, --type STR  :rand  Generating algorithm ("dfs" or "rand")
  -h, --help

Refer to docs for more details.
```


### Bugs

 - Mazes not _guaranteed_ to have a path from `s` to `g`.
 - Could use a nice refactor in a couple of days.
 - ...


## Authors

 - Andy C. (Columbia University)
 - ...


## License

Copyright © 2014 Andy Chiang.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.