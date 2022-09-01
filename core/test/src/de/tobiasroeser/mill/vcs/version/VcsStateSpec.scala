package de.tobiasroeser.mill.vcs.version

import scala.util.Try

import org.scalatest.freespec.AnyFreeSpec

class VcsStateSpec extends AnyFreeSpec {

  def state(
      lastTag: String,
      commitsSinceLastTag: Int,
      dirtyHash: String = null,
      currentRevision: String = "abcdefghijklmnopqrstuvwxyz"
  ): VcsState = VcsState(currentRevision, Option(lastTag), commitsSinceLastTag, Option(dirtyHash))

  "VcsState.format" - {
    "With default format options" - {
      "without any tag and commit" in {
        assert(state(null, 0, null).format() === "0.0.0-0-abcdef")
      }
      "without any tag" in {
        assert(state(null, 2, null).format() === "0.0.0-2-abcdef")
      }
      "locally changed without any tag" in {
        assert(state(null, 1, "d23456789").format() === "0.0.0-1-abcdef-DIRTYd2345678")
      }
      "locally changed without any tag and commit" in {
        assert(state(null, 0, "d23456789").format() === "0.0.0-0-abcdef-DIRTYd2345678")
      }
      "other" in {
        assert(
          state("0.0.1", 2, "d23456789")
            .format() === "0.0.1-2-abcdef-DIRTYd2345678"
        )
      }
    }

    "should strip the `v` prefix from the tag by default" in {
      assert(
        state("v0.7.3", 0).format() === "0.7.3"
      )

    }

    "should be able to use a tag modifier to change the tag" in {
      assert(
        state("v0.7.3", 0, null)
          .format(tagModifier = {
            case t if t.startsWith("v") => t.substring(1) + "v"
            case t                      => t
          }) === "0.7.3v"
      )
    }

    "should append a -SNAPSHOT suffix" in {
      assert(
        state("0.7.3", 0, null, "61568ec80f2465f3f01ea2c7e92273f4fbf94b01")
          .format(appendSnapshot = "-SNAPSHOT") === "0.7.3"
      )
      assert(
        state("0.7.3", 4, "a6ea44d3726", "61568ec80f2465f3f01ea2c7e92273f4fbf94b01")
          .format(appendSnapshot = "-SNAPSHOT") === "0.7.3-4-61568e-DIRTYa6ea44d3-SNAPSHOT"
      )
      assert(
        state("0.7.3", 4, null, "61568ec80f2465f3f01ea2c7e92273f4fbf94b01")
          .format(appendSnapshot = "-SNAPSHOT") === "0.7.3-4-61568e-SNAPSHOT"
      )
    }

    "Example format configs" - {
      "mill" in {
        assert(
          state("0.7.3", 4, "a6ea44d3726", "61568ec80f2465f3f01ea2c7e92273f4fbf94b01")
            .format(dirtyHashDigits = 8, commitCountPad = 0, countSep = "-") === "0.7.3-4-61568e-DIRTYa6ea44d3"
        )
        assert(
          state("0.7.3", 4, null, "61568ec80f2465f3f01ea2c7e92273f4fbf94b01")
            .format(dirtyHashDigits = 8, commitCountPad = 0, countSep = "-") === "0.7.3-4-61568e"
        )
      }
      "Comfis" in {
        assert(
          state("5.3.7", 30, "d23456789", "618c86095ce483feea2e331cc4e28e6466d634f7")
            .format(dirtyHashDigits = 0, commitCountPad = 4, countSep = ".") === "5.3.7.0030-618c86-DIRTY"
        )
        assert(
          state("5.3.7", 30, null, "618c86095ce483feea2e331cc4e28e6466d634f7")
            .format(dirtyHashDigits = 0, commitCountPad = 4, countSep = ".") === "5.3.7.0030-618c86"
        )
      }
    }

  }

}
