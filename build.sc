// mill plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version:0.0.1`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest:0.3.3`
import $ivy.`com.lihaoyi::mill-contrib-scoverage:$MILL_VERSION`

import mill._
import mill.contrib.scoverage.ScoverageModule
import mill.define.{Target, Task}
import mill.scalalib._
import mill.scalalib.publish._
import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version._
import os.Path
import scala.collection.immutable.ListMap


val baseDir = build.millSourcePath

trait Deps {
  def millVersion = "0.7.0" // scala-steward:off
  def scalaVersion = "2.13.2"

  val millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  val millMainApi = ivy"com.lihaoyi::mill-main-api:${millVersion}"
  val millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  val millScalalibApi = ivy"com.lihaoyi::mill-scalalib-api:${millVersion}"
  val scalaTest = ivy"org.scalatest::scalatest:3.2.1"
  val slf4j = ivy"org.slf4j:slf4j-api:1.7.25"
}

object Deps_0_7 extends Deps
object Deps_0_6 extends Deps {
  override def millVersion = "0.6.0" // scala-steward:off
  override def scalaVersion = "2.12.10"
}


val millApiVersions: Map[String, Deps] = ListMap("0.7" -> Deps_0_7, "0.6" -> Deps_0_6)

val millItestVersions = Seq(
  "0.7.3", "0.7.2", "0.7.1", "0.7.0",
  "0.6.3", "0.6.2", "0.6.1", "0.6.0"
)

trait BaseModule extends CrossScalaModule with PublishModule with ScoverageModule {
  def millApiVersion: String
  def deps: Deps = millApiVersions(millApiVersion)
  def crossScalaVersion = deps.scalaVersion

  override def ivyDeps = T {
    Agg(ivy"${scalaOrganization()}:scala-library:${scalaVersion()}")
  }

  def publishVersion = VcsVersion.vcsState().format()

  override def javacOptions = Seq("-source", "1.8", "-target", "1.8", "-encoding", "UTF-8")
  override def scalacOptions = Seq("-target:jvm-1.8", "-encoding", "UTF-8")

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

  override def scoverageVersion = "1.4.1"

  trait Tests extends ScoverageTests

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

object itest extends Cross[ItestCross](millItestVersions: _*)
class ItestCross(millItestVersion: String)  extends MillIntegrationTestModule {
  val millApiVersion = millItestVersion.split("[.]").take(2).mkString(".")
  override def millSourcePath: Path = super.millSourcePath / os.up
  override def millTestVersion = millItestVersion
  override def pluginsUnderTest = Seq(core(millApiVersion))
  /** Replaces the plugin jar with a scoverage-enhanced version of it. */
  override def pluginUnderTestDetails: Task.Sequence[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))] =
    Target.traverse(pluginsUnderTest) { p =>
      val jar = p match {
        case p: ScoverageModule => p.scoverage.jar
        case p => p.jar
      }
      jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
    }

  override def testInvocations: Target[Seq[(PathRef, Seq[TestInvocation.Targets])]] = T{
    Seq(
      PathRef(millSourcePath / "src" / "01-simple") -> Seq(
        TestInvocation.Targets(Seq("-d", "verify")),
        TestInvocation.Targets(Seq("de.tobiasroeser.mill.vcs.version.VcsVersion/vcsState"))
      )
    )
  }

}
