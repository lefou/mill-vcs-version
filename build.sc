import mill._
import mill.define.{Command, Module, TaskModule}
import mill.scalalib._
import mill.scalalib.publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest:0.3.1`
import de.tobiasroeser.mill.integrationtest._
import mill.api.Loose
import mill.main.Tasks
import os.Path

import scala.collection.immutable.ListMap

val baseDir = build.millSourcePath

trait Deps {
  def millVersion = "0.7.0"
  def scalaVersion = "2.13.2"

  val millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  val millMainApi = ivy"com.lihaoyi::mill-main-api:${millVersion}"
  val millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  val millScalalibApi = ivy"com.lihaoyi::mill-scalalib-api:${millVersion}"
  val scalaTest = ivy"org.scalatest::scalatest:3.1.2"
  val slf4j = ivy"org.slf4j:slf4j-api:1.7.25"
}

object Deps_0_7 extends Deps
object Deps_0_6 extends Deps {
  override def millVersion = "0.6.0"
  override def scalaVersion = "2.12.10"
}


val millApiVersions: Map[String, Deps] = ListMap("0.7" -> Deps_0_7, "0.6" -> Deps_0_6)

trait BaseModule extends CrossScalaModule with PublishModule {
  def millApiVersion: String
  def deps: Deps = millApiVersions(millApiVersion)
  def crossScalaVersion = deps.scalaVersion

  override def ivyDeps = T {
    Agg(ivy"${scalaOrganization()}:scala-library:${scalaVersion()}")
  }

  def publishVersion = "0.0.0-SNAPSHOT"

  override def javacOptions = Seq("-source", "1.8", "-target", "1.8")
  override def scalacOptions = Seq("-target:jvm-1.8")

  def pomSettings = T {
    PomSettings(
      description = "Mill plugin to derive a version from (last) git tag and edit state",
      organization = "de.tototec",
      url = "https://github.com/lefou/mill-vcs-version",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("lefou", "mill-vcs-version"),
      developers = Seq(Developer("lefou", "Tobias Roeser", "https.//github.com/lefou"))
    )
  }

}


object core extends Cross[CoreCross](millApiVersions.keysIterator.toSeq: _*)
class CoreCross(override val millApiVersion: String) extends BaseModule {

  override def artifactName = "de.tobiasroeser.mill.vcs.version"

  override def skipIdea: Boolean = millApiVersion != millApiVersions.head._1

  override def compileIvyDeps = Agg(
    deps.millMain,
    deps.millScalalib
  )

  object test extends Tests {
    override def ivyDeps = Agg(deps.scalaTest)
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

val testVersions = Seq(
  "0.7.3", "0.7.2", "0.7.1", "0.7.0",
  "0.6.3", "0.6.2", "0.6.1", "0.6.0"
)

object itest extends Cross[ItestCross](testVersions: _*)
class ItestCross(millVersion: String)  extends MillIntegrationTestModule {
  val millApiVersion = millVersion.split("[.]").take(2).mkString(".")
  override def millSourcePath: Path = super.millSourcePath / os.up
  override def millTestVersion = millVersion
  override def pluginsUnderTest = Seq(core(millApiVersion))
}
