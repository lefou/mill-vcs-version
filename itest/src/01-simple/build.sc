// mill plugins under test
import $exec.plugins
import $ivy.`org.scoverage:::scalac-scoverage-runtime:1.4.3`

import de.tobiasroeser.mill.vcs.version._
import mill._
import mill.define.Command

val baseDir = build.millSourcePath

def initVcs: T[Unit] =
  T {
    if (!os.exists(baseDir / ".git")) {
      T.log.info("Initializing git repo...")
      Seq(
        os.proc("git", "init"),
        os.proc("git", "config", "user.email", "mill@tototec.de"),
        os.proc("git", "config", "user.name", "Mill CI"),
        os.proc("git", "add", "build.sc"),
        os.proc("git", "commit", "-m", "first commit"),
        os.proc("git", "tag", "1.2.3"),
        os.proc("git", "add", "plugins.sc"),
        os.proc("git", "commit", "-m", "second commit")
      ) foreach (_.call(cwd = baseDir))
    }
    ()
  }

def verify(): Command[Unit] =
  T.command {
    initVcs()
    val version = VcsVersion.vcsState().format()
    T.log.info(s"version=${version}")
    assert(version.startsWith("1.2.3-1-"))
    ()
  }
