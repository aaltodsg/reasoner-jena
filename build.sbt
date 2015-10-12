name := "reasoner-jena"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.jena" % "jena-arq" % "3.0.0" )

fork in run := true

javaOptions in run ++= Seq("-Xms256M", "-Xmx32G", "-XX:+UseConcMarkSweepGC")
