scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.6" % "test",
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.6" % "test",
  "javax.activation" % "activation" % "1.1.1" % "test",
  "com.spotify" % "docker-client" % "8.11.1" % "test",
  "com.github.docker-java" % "docker-java" % "3.0.14"
)

javacOptions in compile ++= Seq("--release", "9") ++ Seq(
  "--add-modules=java.activation")

fork in Test := true

cancelable in Global := true

testOptions in Test += Tests.Argument("-oDF")
