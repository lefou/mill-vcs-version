package de.tobiasroeser.mill.vcs.version

import scala.util.control.NonFatal

import mill.T
import mill.define.{Discover, ExternalModule, Module}

trait VcsVersion extends Module {

  def vcsBasePath: os.Path = millSourcePath

  /**
    * Calc a publishable version based on git tags and dirty state.
    *
    * @return A tuple of (the latest tag, the calculated version string)
    */
  def vcsState: T[VcsState] = T.input {

    val curHead = os.proc('git, "rev-parse", "HEAD").call(cwd = vcsBasePath).out.trim

    val exactTag =
      try {
        Option(
          os.proc(
            'git,
            'describe,
            "--exact-match",
            "--tags",
            "--always",
            curHead
          )
            .call(cwd = vcsBasePath)
            .out
            .trim
        ).filter(_.nonEmpty)
      } catch {
        case NonFatal(_) => None
      }

    val lastTag: Option[String] = exactTag.orElse {
      try {
        Option(os
          .proc('git, 'describe, "--abbrev=0", "--always", "--tags")
          .call()
          .out
          .trim
        ).filter(_.nonEmpty)
      } catch {
       case NonFatal(_) => None
      }
    }

    val commitsSinceLastTag = if(exactTag.isDefined) 0
    else {
      os.proc(
        'git,
        "rev-list",
        curHead,
        lastTag match {
          case Some(tag) => Seq("--not", tag)
          case _ => Seq()
        },
        "--count"
      ).call()
        .out
        .trim
        .toInt
    }

    val dirtyHashCode: Option[String] = os.proc('git, 'diff).call().out.text.trim() match {
      case "" => None
      case s  => Some(Integer.toHexString(s.hashCode))
    }

    new VcsState(
      currentRevision = curHead,
      lastTag = lastTag,
      commitsSinceLastTag = commitsSinceLastTag,
      dirtyHash = dirtyHashCode
    )

  }

}

object VcsVersion extends ExternalModule with VcsVersion {
  lazy val millDiscover = Discover[this.type]
  implicit def millScoptEvaluatorReads[T] = new mill.main.EvaluatorScopt[T]()
}
