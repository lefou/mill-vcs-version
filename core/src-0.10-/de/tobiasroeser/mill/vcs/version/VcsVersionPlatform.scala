package de.tobiasroeser.mill.vcs.version

import mill.define.{Module, Input}

trait VcsVersionPlatform extends Module {

  /**
   * Calc a publishable version based on git tags and dirty state.
   *
   * @return A tuple of (the latest tag, the calculated version string)
   */
  def vcsState: Input[VcsState]
}

trait VcsVersionPlatformCompanion {
  implicit def millScoptEvaluatorReads[T] = new mill.main.EvaluatorScopt[T]()
}
