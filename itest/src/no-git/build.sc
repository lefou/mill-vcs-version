// mill plugins under test
import $exec.plugins
import $ivy.`org.scoverage::scalac-scoverage-runtime:1.4.6`

import de.tobiasroeser.mill.vcs.version._
import mill._
import mill.define.Command

val baseDir = build.millSourcePath

def verify(): Command[Unit] =
  T.command {
    val vcState = VcsVersion.vcsState()

    // TODO currently, we can't control ENV vars in mill-integrationtest
    // but without setting GIT_DIR, git will automatically detect the outer git repo
//    val version = vcState.format()
//    assert(version.startsWith("0.0.0"), s"""Expected: starts with "0.0.0", actual: "$version"""")

    ()
  }
