// mill plugins under test
import $exec.plugins
import $ivy.`org.scoverage::scalac-scoverage-runtime:1.4.1`

import de.tobiasroeser.mill.vcs.version._
import mill._
import mill.define.Command


val baseDir = build.millSourcePath

def initVcs: T[Unit] = T {
  if(!os.exists(baseDir / ".git")) {
    T.log.info("Initializing git repo...")
    os.proc("git", "init").call(cwd = baseDir)
    os.proc("git", "add", "build.sc").call(cwd = baseDir)
    os.proc("git", "commit", "-m", "first commit").call(cwd = baseDir)
    os.proc("git", "tag", "1.2.3").call(cwd = baseDir)
    os.proc("git", "add", "plugins.sc").call(cwd = baseDir)
    os.proc("git", "commit", "-m", "second commit").call(cwd = baseDir)

  }
  ()
}

def verify(): Command[Unit] = T.command {
  initVcs()
  val version = VcsVersion.vcsState().format()
  T.log.info(s"version=${version}")
  assert(version.startsWith("1.2.3-1-"))
  ()
}
