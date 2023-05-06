package de.tobiasroeser.mill.vcs.version

//import mill.T
//import mill.api.Logger
//import mill.define.Module
//
//trait VcsVersionPlatform extends Module {
//
//  /**
//   * Calc a publishable version based on git tags and dirty state.
//   *
//   * @return A tuple of (the latest tag, the calculated version string)
//   */
//  def vcsState: T[VcsState] = T.input {
//    calcVcsState(T.log)
//  }
//
//  private[version] def calcVcsState(logger: Logger): VcsState
//}
//
trait VcsVersionPlatformCompanion {
  implicit def millScoptEvaluatorReads[T] = new mill.main.EvaluatorTokenReader[T]()
}
