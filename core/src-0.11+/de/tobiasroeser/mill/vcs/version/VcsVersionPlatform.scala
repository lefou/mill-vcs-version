package de.tobiasroeser.mill.vcs.version

import mill.T
import mill.define.Module

trait VcsVersionPlatform extends Module {

  /**
   * Calc a publishable version based on git tags and dirty state.
   *
   * @return A tuple of (the latest tag, the calculated version string)
   */
  def vcsState: T[VcsState]
}

trait VcsVersionPlatformCompanion {
  implicit def millScoptEvaluatorReads[T] = new mill.main.EvaluatorTokenReader[T]()
}
