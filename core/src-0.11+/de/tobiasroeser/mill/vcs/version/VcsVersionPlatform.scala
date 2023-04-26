package de.tobiasroeser.mill.vcs.version

trait VcsVersionPlatformCompanion {
  implicit def millScoptEvaluatorReads[T] = new mill.main.EvaluatorTokenReader[T]()
}