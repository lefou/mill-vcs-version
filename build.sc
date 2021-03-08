// mill plugins
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version_mill0.9:0.1.0`
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest_mill0.9:0.4.0-5-9dce73`
import $ivy.`com.lihaoyi::mill-contrib-scoverage:$MILL_VERSION`

import mill._
import mill.contrib.scoverage.ScoverageModule
import mill.define.{Target, Task}
import mill.scalalib._
import mill.scalalib.publish._
import de.tobiasroeser.mill.integrationtest._
import de.tobiasroeser.mill.vcs.version._
import os.Path

val baseDir = build.millSourcePath

trait Deps {
  def millPlatform = "0.9"
  def millVersion = "0.9.3" // scala-steward:off
  def scalaVersion = "2.13.5"
  def testWithMill = Seq("0.9.5", "0.9.4", "0.9.3")

  val millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  val millMainApi = ivy"com.lihaoyi::mill-main-api:${millVersion}"
  val millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"
  val millScalalibApi = ivy"com.lihaoyi::mill-scalalib-api:${millVersion}"
  val scalaTest = ivy"org.scalatest::scalatest:3.2.6"
  val slf4j = ivy"org.slf4j:slf4j-api:1.7.25"
}

object Deps_0_9 extends Deps
object Deps_0_7 extends Deps {
  override def millPlatform = "0.7"
  override def millVersion = "0.7.0" // scala-steward:off
  override def scalaVersion = "2.13.5"
  override def testWithMill = Seq("0.8.0", "0.7.4", "0.7.3", "0.7.2", "0.7.1", "0.7.0")
}
object Deps_0_6 extends Deps {
  override def millPlatform = "0.6"
  override def millVersion = "0.6.0" // scala-steward:off
  override def scalaVersion = "2.12.13"
  override def testWithMill = Seq("0.6.3", "0.6.2", "0.6.1", "0.6.0")
}

val crossDeps = Seq(Deps_0_9, Deps_0_7, Deps_0_6)
val millApiVersions = crossDeps.map(x => x.millPlatform -> x)
val millItestVersions = crossDeps.flatMap(x => x.testWithMill.map(_ -> x))

trait BaseModule extends CrossScalaModule with PublishModule with ScoverageModule {
  def millApiVersion: String
  def deps: Deps = millApiVersions.toMap.apply(millApiVersion)
  def crossScalaVersion = deps.scalaVersion
  override def artifactSuffix: T[String] = s"_mill${deps.millPlatform}_${artifactScalaVersion()}"

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

object core extends Cross[CoreCross](millApiVersions.map(_._1): _*)
class CoreCross(override val millApiVersion: String) extends BaseModule {

  override def artifactName = "de.tobiasroeser.mill.vcs.version"

  override def skipIdea: Boolean = deps != crossDeps.head

  override def compileIvyDeps = Agg(
    deps.millMain,
    deps.millScalalib
  )

  object test extends Tests {
    override def ivyDeps = Agg(deps.scalaTest)
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

object itest extends Cross[ItestCross](millItestVersions.map(_._1): _*)
class ItestCross(millItestVersion: String) extends MillIntegrationTestModule {
  val millApiVersion = millItestVersions.toMap.apply(millItestVersion).millPlatform
  override def millSourcePath: Path = super.millSourcePath / os.up
  override def millTestVersion = millItestVersion
  override def pluginsUnderTest = Seq(core(millApiVersion))

  /** Replaces the plugin jar with a scoverage-enhanced version of it. */
  override def pluginUnderTestDetails: Task.Sequence[(PathRef, (PathRef, (PathRef, (PathRef, (PathRef, Artifact)))))] =
    Target.traverse(pluginsUnderTest) { p =>
      val jar = p match {
        case p: ScoverageModule => p.scoverage.jar
        case p                  => p.jar
      }
      jar zip (p.sourceJar zip (p.docJar zip (p.pom zip (p.ivy zip p.artifactMetadata))))
    }

  override def testInvocations: Target[Seq[(PathRef, Seq[TestInvocation.Targets])]] = T {
    super.testInvocations().map {
      case (pr, _) if pr.path.last == "01-simple" =>
        pr -> Seq(
          TestInvocation.Targets(Seq("-d", "verify")),
          TestInvocation.Targets(Seq("de.tobiasroeser.mill.vcs.version.VcsVersion/vcsState"))
        )
      case (pr, _) => pr -> Seq(TestInvocation.Targets(Seq("-d", "verify")))
    }
  }

}
