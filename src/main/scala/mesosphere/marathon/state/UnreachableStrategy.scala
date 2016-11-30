package mesosphere.marathon
package state

import com.wix.accord._
import com.wix.accord.dsl._

import scala.concurrent.duration._
import FiniteDuration._ // HACK: work around for "diverting implicit" compilation errors in IDEA

/**
  * Defines the time outs for unreachable tasks.
  */
case class UnreachableStrategy(
  unreachableInactiveAfter: FiniteDuration = UnreachableStrategy.DefaultTimeUntilInactive,
  unreachableExpungeAfter: FiniteDuration = UnreachableStrategy.DefaultTimeUntilExpunge)

object UnreachableStrategy {
  val DefaultTimeUntilInactive: FiniteDuration = 3.minutes
  val DefaultTimeUntilExpunge: FiniteDuration = 6.minutes
  val MinInactiveAfter: FiniteDuration = 1.second

  implicit val validStrategy: Validator[UnreachableStrategy] = validator[UnreachableStrategy] { strategy =>
    strategy.unreachableInactiveAfter should be >= MinInactiveAfter
    strategy.unreachableInactiveAfter should be < strategy.unreachableExpungeAfter
  }
}
