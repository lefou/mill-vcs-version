// mill plugins under test
import $file.plugins
// generated by mill
import $file.shared

import de.tobiasroeser.mill.vcs.version._
import mill._
import mill.define.Command

def baseDir = build.millSourcePath

def initVcs: T[Unit] = T {
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

def verify1(): Command[Unit] = T.command {
  initVcs()
  val vcsState = VcsVersion.vcsState()
  assert(vcsState.vcs == Some(Vcs.git))

  val version = vcsState.format()
  T.log.info(s"version=${version}")
  assert(version.startsWith("1.2.3-1-") && !version.contains("DIRTY"))
  ()
}

def changeSomething(): Command[Unit] = T.command {
  os.write.append(baseDir / "plugins.sc", "\n// dummy text")
  ()
}

def verify2(): Command[Unit] = T.command {
  initVcs()
  val version = VcsVersion.vcsState().format()
  T.log.info(s"version=${version}")
  assert(version.startsWith("1.2.3-1-") && version.contains("DIRTY"), s"Version was: ${version}")
  ()
}
