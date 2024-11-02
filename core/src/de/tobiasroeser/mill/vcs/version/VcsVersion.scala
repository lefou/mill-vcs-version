package de.tobiasroeser.mill.vcs.version

import mill.T
import mill.api.Logger
import mill.define.{Discover, ExternalModule, Input, Module}
import os.{CommandResult, Shellable, SubprocessException}

import scala.util.control.NonFatal

trait VcsVersion extends Module {

  def vcsBasePath: os.Path = millSourcePath

  /**
   * Calc a publishable version based on git tags and dirty state.
   *
   * @return A tuple of (the latest tag, the calculated version string)
   */
  def vcsState: Input[VcsState] = T.input { calcVcsState(T.log) }

  def runGit(args: Shellable*): CommandResult = os.proc(Seq[Shellable]("git") ++ args).call(cwd = vcsBasePath, stderr = os.Pipe)

  private[this] def calcVcsState(logger: Logger): VcsState = {
    logger.error(s"vcsBasePath: ${vcsBasePath}")
    val curHeadRaw =
      try {
        Option(runGit("rev-parse", "HEAD").out.trim())
      } catch {
        case e: SubprocessException =>
          logger.error(s"${vcsBasePath} is not a git repository.")
          None
      }

    curHeadRaw match {
      case None =>
        VcsState("no-vcs", None, 0, None, None)

      case curHead =>
        // we have a proper git repo

        val exactTag =
          try {
            curHead
              .map(curHead =>
                runGit("describe", "--exact-match", "--tags", "--always", curHead).out.text().trim
              )
              .filter(_.nonEmpty)
          } catch {
            case NonFatal(_) => None
          }

        val lastTag: Option[String] = exactTag.orElse {
          try {
            Option(
              runGit("describe", "--abbrev=0", "--tags").out.text().trim()
            )
              .filter(_.nonEmpty)
          } catch {
            case NonFatal(_) => None
          }
        }

        val commitsSinceLastTag =
          if (exactTag.isDefined) 0
          else {
            curHead
              .map { curHead =>
                runGit(
                  "rev-list",
                  curHead,
                  lastTag match {
                    case Some(tag) => Seq("--not", tag)
                    case _         => Seq()
                  },
                  "--count"
                ).out.trim().toInt
              }
              .getOrElse(0)
          }

        val dirtyHashCode: Option[String] = Option(runGit("diff").out.text().trim()).flatMap {
          case "" => None
          case s  => Some(Integer.toHexString(s.hashCode))
        }

        new VcsState(
          currentRevision = curHead.getOrElse(""),
          lastTag = lastTag,
          commitsSinceLastTag = commitsSinceLastTag,
          dirtyHash = dirtyHashCode,
          vcs = Option(Vcs.git)
        )
    }
  }

}

object VcsVersion extends ExternalModule with VcsVersion {
  lazy val millDiscover = Discover[this.type]
}
