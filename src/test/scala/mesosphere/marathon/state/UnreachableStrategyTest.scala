package mesosphere.marathon
package state

import mesosphere.UnitTest
import com.wix.accord.scalatest.ResultMatchers

import scala.concurrent.duration._

class UnreachableStrategyTest extends UnitTest with ResultMatchers {

  def validate = UnreachableStrategy.validStrategy

  "UnreachableStrategy.unreachableStrategyValidator" should {
    "validate default strategy" in {
      val strategy = UnreachableStrategy()
      validate(strategy) shouldBe aSuccess
    }

    "validate with other paramters successfully" in {
      val strategy = UnreachableStrategy(13.minutes, 37.minutes)
      validate(strategy) shouldBe aSuccess
    }

    "fail with invalid time until inactive" in {
      val strategy = UnreachableStrategy(unreachableInactiveAfter = 0.second)
      validate(strategy) should failWith("unreachableInactiveAfter" -> "got 0 seconds, expected 1 second or more")
    }

    "fail when time until expunge is smaller" in {
      val strategy = UnreachableStrategy(unreachableInactiveAfter = 2.seconds, unreachableExpungeAfter = 1.second)
      validate(strategy) should failWith("unreachableInactiveAfter" -> "got 2 seconds, expected less than 1 second")
    }

    "fail when time until expunge is equal to time until inactive" in {
      val strategy = UnreachableStrategy(unreachableInactiveAfter = 2.seconds, unreachableExpungeAfter = 2.seconds)
      validate(strategy) should failWith("unreachableInactiveAfter" -> "got 2 seconds, expected less than 2 seconds")
    }
  }
}
