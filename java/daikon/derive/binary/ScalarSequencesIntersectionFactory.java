package daikon.derive.binary;

import daikon.*;
import daikon.inv.binary.twoScalar.*; // for IntComparison
import daikon.inv.unary.scalar.*; // for LowerBound

import utilMDE.*;
import java.util.*;

// *****
// Do not edit this file directly:
// it is automatically generated from SequencesIntersectionFactory.java.jpp
// *****

// This controls derivations which use the scalar as an index into the
// sequence, such as getting the element at that index or a subsequence up
// to that index.

public final class ScalarSequencesIntersectionFactory  extends BinaryDerivationFactory {

  public BinaryDerivation[] instantiate(VarInfo seq1, VarInfo seq2) {
    if ((seq1.rep_type != ProglangType.INT_ARRAY )
        || (seq2.rep_type != ProglangType.INT_ARRAY )) {
      return null;
    }

    // Intersect only sets with the same declared element type 
    if (!seq1.type.base().equals(seq2.type.base())) 
      return null;

    Assert.assert(seq1.isCanonical());
    Assert.assert(seq2.isCanonical());

    // For now, do nothing if the sequences are derived.
    if ((seq1.derived != null)||(seq2.derived != null))
      return null;

    return new BinaryDerivation[] {
	new ScalarSequencesIntersection (seq1, seq2) };
  }
}

