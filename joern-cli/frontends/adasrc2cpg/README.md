# adasrc2cpg

A libadalang-based parser for Ada source code that creates code property graphs according to the specification at https://github.com/ShiftLeftSecurity/codepropertygraph.

## Building the code

The build process requires:

* JDK 11 or higher
* Scala build tool (sbt)
* Python 3 with libadalang installed

To build adasrc2cpg, issue the command `sbt stage` from the Joern root directory.

## libadalang Integration

This frontend uses [libadalang](https://github.com/AdaCore/libadalang), AdaCore's Ada parser library, to parse Ada source code and generate ASTs.

### Installing libadalang

```bash
pip install libadalang
```

Or follow the installation instructions at: https://github.com/AdaCore/libadalang

## Running

To produce a code property graph, issue the command:

```bash
./adasrc2cpg.sh <path/to/sourceCodeDirectory> --output <path/to/outputCpg>
```

Run the following to see a complete list of available options:

```bash
./adasrc2cpg.sh --help
```

## Ada File Extensions

This frontend recognizes the following Ada file extensions:
- `.adb` - Ada body files (implementation)
- `.ads` - Ada specification files (interface)
