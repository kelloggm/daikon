package daikon.inv.unary.sequence;

import daikon.*;
import daikon.inv.*;

import utilMDE.*;

import java.util.*;

// States that the value is one of the specified values.

// This subsumes an "exact" invariant that says the value is always exactly
// a specific value.  Do I want to make that a separate invariant
// nonetheless?  Probably not, as this will simplify implication and such.

public final class EltNonZero extends SingleSequence {
  long min = Long.MAX_VALUE;
  long max = Long.MIN_VALUE;

  // If nonzero, use this as the range instead of the actual range.
  // This lets one use a specified probability of nonzero (say, 1/10
  // for pointers).
  int override_range = 0;
  boolean pointer_type = false;

  EltNonZero(PptSlice ppt) {
    super(ppt);
  }

  public static EltNonZero instantiate(PptSlice ppt) {
    EltNonZero result = new EltNonZero(ppt);
    if (! ppt.var_infos[0].type.baseIsIntegral()) {
      result.pointer_type = true;
      result.override_range = 10;
    }
    return result;
  }

  public String repr() {
    double probability = getProbability();
    return "EltNonZero(" + var().name + "): "
      + !no_invariant + ",min=" + min + ",max=" + max
      + "; probability = " + probability;
  }

  public String format() {
    if ((!no_invariant) && justified())
      return var().name + " elements != " + (pointer_type ? "null" : "0");
    else
      return null;
  }

  public void add_modified(long[] a, int count) {
    for (int ai=0; ai<a.length; ai++) {
      long v = a[ai];

      // The min and max tests will simultaneoulsy succeed exactly once (for
      // the first value).
      if (v == 0) {
        destroy();
        return;
      }
      if (v < min) min = v;
      if (v > max) max = v;
      // probability_cache_accurate = false;
    }
  }

  protected double computeProbability() {
    Assert.assert(! no_invariant);
    // Maybe just use 0 as the min or max instead, and see what happens:
    // see whether the "nonzero" invariant holds anyway.  (Perhaps only
    // makes sense to do if the {Lower,Upper}Bound invariant doesn't imply
    // the non-zeroness.)  In that case, do still check for no values yet
    // received.
    if ((override_range == 0) && ((min > 0) || (max < 0)))
      return Invariant.PROBABILITY_UNKNOWN;
    else {
      long range;
      if (override_range != 0) {
        range = override_range;
      } else {
        int modulus = 1;

        // I need to come back and make this work.
        // {
        //   for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
        //     Invariant inv = (Invariant) itor.next();
        //     if ((inv instanceof Modulus) && inv.justified()) {
        //       modulus = ((Modulus) inv).modulus;
        //       break;
        //     }
        //   }
        // }

        // Perhaps I ought to check that it's possible (given the modulus
        // constraints) for the value to be zero; otherwise, the modulus
        // constraint implies non-zero.
        range = (max - min + 1) / modulus;
      }
      double probability_one_elt_nonzero = 1 - 1.0/range;
      // This could underflow; so consider doing
      //   double log_probability = self.samples*math.log(probability);
      // then calling Math.exp (if the value is in the range that wouldn't
      // cause underflow).
      return Math.pow(probability_one_elt_nonzero, ppt.num_mod_non_missing_samples());
    }
  }

  public boolean isSameFormula(Invariant other)
  {
    Assert.assert(other instanceof EltNonZero);
    return true;
  }
}
