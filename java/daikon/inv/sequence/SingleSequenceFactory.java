package daikon.inv.sequence;

import daikon.*;

import utilMDE.*;

import java.util.*;

public final class SingleSequenceFactory {

  // Adds the appropriate new Invariant objects to the specified Invariants
  // collection.
  public static Vector instantiate(PptSlice ppt, int pass) {

    VarInfo var = ppt.var_infos[0];
    Assert.assert(var.rep_type == ProglangType.INT_ARRAY);


    Vector result = new Vector();
    if (pass == 1) {
      result.add(OneOfSequence.instantiate(ppt));
    } else if (pass == 2) {
      // I'm not checking var.isConstant() for now

      result.add(EltIntComparison.instantiate(ppt));
      result.add(EltOneOf.instantiate(ppt));
      result.add(EltNonZero.instantiate(ppt));
      result.add(NoDuplicates.instantiate(ppt));
    }
    return result;
  }

  private SingleSequenceFactory() {
  }

}
