package daikon.derive.binary;

import daikon.*;
import daikon.derive.*;

public abstract class BinaryDerivation implements Derivation {

  public BinaryDerivation(VarInfo vi1, VarInfo vi2) {
    var_info1 = vi1;
    var_info2 = vi2;
  }

  VarInfo var_info1;
  VarInfo var_info2;

  public abstract ValueAndModified computeValueAndModified(ValueTuple full_vt);

  private VarInfo this_var_info;
  public VarInfo getVarInfo() {
    if (this_var_info == null) {
      this_var_info = makeVarInfo();
      this_var_info.derived = this;
      var_info1.derivees.add(this);
      var_info2.derivees.add(this);
    }
    return this_var_info;
  }

  // This is in each class, but I can't have a private abstract method.
  abstract protected VarInfo makeVarInfo();

  public int derivedDepth() {
    return 1 + Math.max(var_info1.derivedDepth(), var_info2.derivedDepth());
  }

  // public boolean isDerivedFromNonCanonical() {
  //   // We insist that both are canonical, not just one.
  //   return !(var_info1.isCanonical() && var_info2.isCanonical());
  // }

}
