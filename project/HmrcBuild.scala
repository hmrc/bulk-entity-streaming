import play.core.PlayVersion
import sbt.Keys._
import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning


object HmrcBuild extends Build {

  import BuildDependencies._
  import uk.gov.hmrc.DefaultBuildSettings._

  val nameApp = "bulk-entity-streaming"

  lazy val BulkEntityStreaming = (project in file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      scalaVersion := "2.11.7",
      libraryDependencies ++= Seq(
        Compile.httpVerbs,
        Test.scalaTest,
        Test.pegdown,
        Test.playTest
      ),
      Developers()
    )
}

private object BuildDependencies {

  val httpVerbsVersion = "1.10.0"
  object Compile {
    val httpVerbs = "uk.gov.hmrc" %% "http-verbs" % httpVerbsVersion
  }

  sealed abstract class Test(scope: String) {
    val scalaTest = "org.scalatest" %% "scalatest" % "2.2.4" % scope
    val pegdown = "org.pegdown" % "pegdown" % "1.5.0" % scope
    val playTest = "com.typesafe.play" %% "play-test" % PlayVersion.current % scope
  }

  object Test extends Test("test")

}

object Developers {

  def apply() = developers := List[Developer]()
}
