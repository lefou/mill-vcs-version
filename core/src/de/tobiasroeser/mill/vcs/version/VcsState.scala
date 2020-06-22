package de.tobiasroeser.mill.vcs.version

case class VcsState(
    currentRevision: String,
    lastTag: Option[String],
    commitsSinceLastTag: Int,
    dirtyHash: Option[String]
) {

  def format(
      noTagFallback: String = "0.0.0",
      countSep: String = "-",
      commitCountPad: Byte = 0,
      revSep: String = "-",
      revHashDigits: Int = 6,
      dirtySep: String = "-DIRTY",
      dirtyHashDigits: Int = 8,
      tagModifier: String => String = t => t
  ): String = {
    val versionPart = tagModifier(lastTag.getOrElse(noTagFallback))

    val commitCountPart = if (lastTag.isEmpty || commitsSinceLastTag > 0) {
      s"$countSep${if (commitCountPad > 0) {
        (10000000000000L + commitsSinceLastTag).toString().substring(14 - commitCountPad, 14)
      } else commitsSinceLastTag}"
    } else ""

    val revisionPart = if (lastTag.isEmpty || commitsSinceLastTag > 0) {
      s"$revSep${currentRevision.take(revHashDigits)}"
    } else ""

    val dirtyPart = dirtyHash match {
      case None    => ""
      case Some(d) => dirtySep + d.take(dirtyHashDigits)
    }

    s"$versionPart$commitCountPart$revisionPart$dirtyPart"
  }

}

object VcsState {
  implicit def jsonify: upickle.default.ReadWriter[VcsState] = upickle.default.macroRW
}
