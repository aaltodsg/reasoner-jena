package fi.aalto.cs.dsg.reasonerjena

import java.io.{FileOutputStream, PrintStream, File, PrintWriter}
import org.apache.jena.query.{Query, ResultSetFormatter, QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{InfModel, ModelFactory, Model}
import org.apache.jena.reasoner.{ReasonerRegistry, Reasoner}
import org.apache.jena.riot.RDFDataMgr
import org.apache.log4j.Logger

import scala.collection.mutable

/**
 * Created by mikko.rinne@aalto.fi on 28/09/15.
 */
object ReasonerJena {

  val argInfo =
    s"""
       |Arguments: -h --csv-output=csvfile -r reasoner -q queryfile -s schemafile --time=timefile -d datafile
       |
       |-h: Display this help text and terminate processing.
       |-r reasoner: Jena reasoner to use. Alternatives: Transitive, RDFSSimple, RDFS, OWLMicro, OWLMini, OWL
       |   Omitting the parameter means that no reasoner will be employed.
       |-q queryfile: Name of a SPARQL query file.
       |-s schemafile: Name of the file containing the OWL-schema.
       |-d datafile: Data to be processed.
       |--csv-output=csvfile: Name of the file, where CSV output will be written.
       |   Defaults to '/dev/stdout'.
       |--time=timefile: Starts a timer from the parameter onwards. '--time=-' outputs to /dev/stdout
       |
       |Note1: Command line arguments are processed sequentially and the order counts!
       |
       |Note2: --csv-output and --time can also be separated by space. If your path contains spaces,
       |       use '--csv-output "my file with spaces.csv"'
    """.stripMargin

  private var writeHandle: PrintStream = null

  private[this] val logger = Logger.getLogger(getClass.getName);

  def main(args: Array[String]): Unit = {

    var tStart: Long = 0L
    var timerFileHandle: PrintWriter = null

    openCSVOutput("/dev/stdout")

    var schema: Model = null
    var data: Model = null
    var query: Query = null
    var reasoner: Reasoner = null
    var model: Model = null

    val LUBM_HOME = "/Users/rinnem2/data/lubm"

    System.gc()

    if (args.length > 0) {
      val argBuffer = mutable.Buffer[String]()

      // Expand args with "--"
      for (arg <- args) {
        if (arg.startsWith("--")) {
          if (arg.contains('=')) {
            argBuffer ++= arg.split("=")
          } else {
            argBuffer += arg // accept both space and = because SBT messes up filenames with spaces otherwise
            // error("Don't know how to parse: "+arg)
            // println(argInfo)
          }
        } else {
          argBuffer += arg
        }
      }

      var argList = argBuffer.toList

      do {
        argList match {
          case "-q" :: string :: tail => {
            printTimer("Command: -q, Parameter: "+string)
            val source = scala.io.Source.fromFile(string)
            val queryString = try source.getLines mkString "\n" finally source.close()
            query = QueryFactory.create(queryString)
            argList = tail
          }
          case "-s" :: string :: tail => {
            printTimer("Command: -s, Parameter: "+string)
            schema = RDFDataMgr.loadModel(string)
            argList = tail
          }
          case "-d" :: string :: tail => {
            printTimer("Command: -d, Parameter: "+string)
            data = RDFDataMgr.loadModel(string)
            argList = tail
          }
          case "-r" :: string :: tail => {
            printTimer("Command: -r, Parameter: "+string)
            if (schema == null) println("Schema must be specified before reasoner!")
            else {
              string match {
                case "Transitive" => { reasoner = ReasonerRegistry.getTransitiveReasoner.bindSchema(schema) }
                case "RDFSSimple" => reasoner = ReasonerRegistry.getRDFSSimpleReasoner.bindSchema(schema)
                case "RDFS" => reasoner = ReasonerRegistry.getRDFSReasoner.bindSchema(schema)
                case "OWLMicro" => reasoner = ReasonerRegistry.getOWLMicroReasoner.bindSchema(schema)
                case "OWLMini" => reasoner = ReasonerRegistry.getOWLMiniReasoner.bindSchema(schema)
                case "OWL" => reasoner = ReasonerRegistry.getOWLReasoner.bindSchema(schema)
                case somethingElse => {
                  println("Unknown reasoner: "+string)
                  println(argInfo)
                  logger.error("Unknown reasoner: "+string)
              }
              }
            }
            argList = tail
          }
          case "--csv-output" :: string :: tail => {
            closeCSVOutput()
            // println("Opening file: "+string+" for csv-output")
            openCSVOutput(string)
            argList = tail
          }
          case "--time" :: string :: tail => {
            if (tStart > 0L) {
              printTimer("Re-starting timer to "+string+". Old value.")
              timerFileHandle.close()
            }
            var writeFileName = ""
            if (string == "-") writeFileName="/dev/stdout" else writeFileName=string
            timerFileHandle = {
              try {
                new PrintWriter(new File(writeFileName))
              } catch {
                case ex: Exception => { logger.error(ex.toString,ex)
                  null }
              }
            }
            tStart = System.nanoTime()
            argList = tail
          }
          case "-h" :: tail => {
            println(argInfo)
            argList = Nil
          }
          case somethingElse => {
            logger.error("Don't know how to parse: "+somethingElse)
            println(argInfo)
            argList = Nil
          }
        }
      } while (argList != Nil)
    } else {
      println(argInfo)
    }

    if (data == null) {
      println("No data available - cannot do anything!")
    } else {
      if (reasoner == null) {
        model = data
      } else {
        model = ModelFactory.createInfModel(reasoner, data)
      }

      val qe = QueryExecutionFactory.create(query, model)
      val results = qe.execSelect()

      // Output query results
      ResultSetFormatter.outputAsCSV(writeHandle, results)

      // Important - free up resources used running the query
      qe.close()
    }

    printTimer("Done")
    if (tStart > 0L) timerFileHandle.close()

    closeCSVOutput()


    // RDFS reasoner. RDFS_SIMPLE seems to work until q6 (inclusive)
    /*
    // Initiate a model
    val myModel = ModelFactory.createDefaultModel()

    // Supply with inferencing
    val myReasoner = RDFSRuleReasonerFactory.theInstance().create(null)
    myReasoner.setParameter(ReasonerVocabulary.PROPsetRDFSLevel,ReasonerVocabulary.RDFS_FULL);
    val inf = ModelFactory.createInfModel(myReasoner, myModel);
    // Populate with our ontology
    RDFDataMgr.read(myModel,LUBM_HOME+"/univ-bench.ttl")
    */

    // OWL Reasoner. So far did not seem to add anything
    /*
    val schema = RDFDataMgr.loadModel(LUBM_HOME+"/univ-bench.ttl")
    val data = RDFDataMgr.loadModel(LUBM_HOME+"/gen_1uni/University0.ttl")
    // Built-in OWL model spec: OntModelSpec.OWL_DL_MEM
    // val inf = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM,schema)
    val reasoner = ReasonerRegistry.getOWLReasoner.bindSchema(schema)
    val inf = ModelFactory.createInfModel(reasoner, data)
    */

    // Pellet Reasoner - didn't get it to work because:
    // 1) PelletReasonerFactory.THE_SPEC is of the wrong type for the current Jena. Didn't find a new-enough Pellet
    //    Could scan the newest Pellet in github and compile that instead of using SBT/MVN.
    // 2) Old Jena crashes already with the QueryFactory creation. Suspecting a problem with using Java8.
    //    Could try to solve by using older Javas. Jena 2.13-2.14 should be working with the available Pellets.
    // Tips: A) http://question.ikende.com/question/2D353534363131373639
    //       B) http://programcreek.com/java-api-examples/index.php?api=com.hp.hpl.jena.ontology.OntModel
    /*
    val schema = RDFDataMgr.loadModel(LUBM_HOME+"/univ-bench.ttl")
    val data = RDFDataMgr.loadModel(LUBM_HOME+"/gen_1uni/University0_0_0.ttl")
    val inf = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC,schema)
    */

    def printTimer(descr: String) = {
      if (tStart > 0L) timerFileHandle.write("At " + (System.nanoTime() - tStart)*.000000001 + ": " + descr + "\n")
    }

  }

  def openCSVOutput(writeFileName: String) = {
    writeHandle = {
      try {
        new PrintStream(writeFileName)
      } catch {
        case ex: Exception => { logger.error(ex.toString,ex)
          null }
      }
    }
  }

  def closeCSVOutput() = {
    writeHandle.flush()
    writeHandle.close()
  }

}
