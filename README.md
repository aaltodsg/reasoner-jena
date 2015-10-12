# reasoner-jena

Scala wrapper code having command line parameters to select a Jena reasoner and start a timer, reporting times for each subsequent command.

## Development status

Currently functions as expected by the author. No further roadmap has been established, but new features may be added if needed.

## Installation

Has been developed as an [SBT](http://www.scala-sbt.org/) project under [IDEA](https://www.jetbrains.com/idea/). Execution should be as simple as:

1) Install SBT if not already installed. Version 0.13.8 was used by
the developer. Due to some changes in Java parameter passing it is
expected that minimum 0.13.6 would be needed.

2) Clone this repository: `$ git clone https://github.com/aaltodsg/xevepro.git`

## Instructions for use

Arguments: -h --csv-output=csvfile -r reasoner -q queryfile -s schemafile --time=timefile -d datafile

-h: Display this help text and terminate processing.
-r reasoner: Jena reasoner to use. Alternatives: Transitive, RDFSSimple, RDFS, OWLMicro, OWLMini, OWL
   Omitting the parameter means that no reasoner will be employed.
-q queryfile: Name of a SPARQL query file.
-s schemafile: Name of the file containing the OWL-schema.
-d datafile: Data to be processed.
--csv-output=csvfile: Name of the file, where CSV output will be written.
   Defaults to '/dev/stdout'.
--time=timefile: Starts a timer from the parameter onwards. '--time=-' outputs to /dev/stdout

Note1: Command line arguments are processed sequentially and the order counts!

Note2: --csv-output and --time can also be separated by space. If your
path contains spaces, use '--csv-output "my file with spaces.csv"'

There is currently no sample data included in this repository. Please
refer to the Jena LUBM example under
[instans-reasoning](https://github.com/aaltodsg/instans-reasoning) for
instructions on execution.
