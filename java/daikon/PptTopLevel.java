package daikon;

import daikon.derive.*;
import daikon.derive.unary.*;
import daikon.derive.binary.*;
import daikon.inv.*;

import java.util.*;

import utilMDE.*;




// All information about a single program point.
// A Ppt may also represent just part of the data: a disjunction.
// This probably doesn't do any direct computation, instead deferring that
// to its views that are slices.

class PptTopLevel extends Ppt {

  final static boolean debugPptTopLevel = false;
  // final static boolean debugPptTopLevel = true;

  final static boolean debugPptTopLevelDerive = false;
  // final static boolean debugPptTopLevelDerive = true;


  // do we need both a num_tracevars for the number of variables in the
  // tracefile and a num_non_dreived_vars for the number of variables
  // actually passed off to this Ppt?  The ppt wouldn't use num_tracevars,
  // but it would make sense to store it here anyway.

  // Maybe I should just use num_orig_vars instead of the first three.

  // number of variables in the trace file; -1 if not yet set
  int num_tracevars;
  int num_orig_vars;

  // Indicates whether derived variables have been introduced.
  // First number: invariants are computed up to this index, non-inclusive
  // Remaining numbers: values have been derived from up to these indices

  // invariant:  len(var_infos) >= invariants_index >= derivation_index[0]
  //   >= derivation_index[1] >= ...
  int[] derivation_indices = new int[derivation_passes+1];

  VarValues values;

  // These accessors are for abstract methods declared in Ppt
  public int num_samples() { return values.num_samples; }
  public int num_mod_non_missing_samples() { return values.num_mod_non_missing_samples(); }
  public int num_values() { return values.num_values; }
  // public int num_missing() { return values.num_missing; }


  // These are now in PptSlice objects instead.
  // Invariants invs;

  PptTopLevel(String name_, VarInfo[] var_infos_) {
    name = name_;
    var_infos = var_infos_;
    for (int i=0; i<var_infos.length; i++) {
      var_infos[i].value_index = i;
      var_infos[i].varinfo_index = i;
      var_infos[i].ppt = this;
    }

    values = new VarValues();
    views = new HashSet();

    // While there are no constants, this works.
    num_tracevars = var_infos.length;
    num_orig_vars = 0;
  }

  // This is used when merging two sets of data, to create Ppts that were
  // missing in one of them.
  PptTopLevel(PptTopLevel other) {
    name = other.name;
    var_infos = other.var_infos;
    derivation_indices = new int[other.derivation_indices.length];
    System.arraycopy(other.derivation_indices, 0, derivation_indices, 0, derivation_indices.length);
    values = new VarValues();
    // views = new WeakHashMap();
    views = new HashSet();
  }

  // Accessing data
  int num_vars() {
    return var_infos.length;
  }
  Iterator var_info_iterator() {
    return Arrays.asList(var_infos).iterator();
  }



  boolean compatible(Ppt other) {
    // This insists that the var_infos lists are identical.  The Ppt
    // copy constructor does reuse the var_infos field.
    return (var_infos == other.var_infos);
  }

  // // Strangely, this modifies "other" but not "this".  That looks wrong.
  // // Perhaps this should operate over VarValues objects or some such?
  // void merge(PptTopLevel other) {
  //   // ensure that the two program points are compatible
  //   Assert.assert(compatible(other));
  //
  //   Set other_entries = other.values.entrySet();
  //   for (Iterator itor = other_entries.iterator() ; itor.hasNext() ;) {
  //     Map.Entry other_entry = (Map.Entry) itor.next();
  //     ValueTuple value_tuple = (ValueTuple) other_entry.getKey();
  //     int new_count = (values.get(value_tuple)
  //                      + VarValues.getValue(other_entry));
  //     // equivalent but less efficient:  values.put(value_tuple, new_count);
  //     VarValues.setValue(other_entry, new_count);
  //   }


  /// Commented out until I decide to specially deal with constants.
  // // This sets the num_truevars and num_constants fields and also
  // // reorders the VarInfos so that constants are at the end.
  // private void count_vars() {
  //   Assert.assert(num_constants == -1);
  //   // Also, there had better be no values, derived variables, or other
  //   // things that could depend on the index.

  //   // First, move the constants to the end.
  //   Vector constants = new Vector();
  //   for (int i=0; i<var_infos.length; i++) {
  //     if (var_infos[i].constant_value != null) {
  //       constants.add(var_infos[i]);
  //     }
  //   }
  //   num_constants = constants.size();
  //   num_truevars = var_infos.length - num_constants;

  //   if ((num_constants > 0)
  //       && (constants.elementAt(0) != var_infos[num_truevars])) {
  //     // We must reorder.  I'm not sorting based on constness because I
  //     // would want a stable sort.
  //     VarInfo[] truevars = new VarInfo[num_truevars];
  //     int truevarindex = 0;
  //     for (int i=0; i<var_infos.length; i++) {
  //       if (!constants.contains(var_infos[i])) {
  // 	truevars[truevarindex] = var_infos[i];
  // 	truevarindex++;
  //       }
  //     }
  //     System.arraycopy(truevars, 0, var_infos, 0, num_truevars);
  //     for (int i=0; i<num_constants; i++) {
  //       var_infos[i+num_truevars] = (VarInfo)constants.elementAt(i);
  //     }
  //     for (int i=0; i<var_infos.length; i++) {
  //       var_infos[i].index = i;
  //     }
  //   }
  // }



  ///////////////////////////////////////////////////////////////////////////
  /// Adding variables
  ///

  // Some of this should perhaps be moved up into Ppt.

  // I'm not using a Vector for the var_infos field, even though that would
  // simplify adding new elements, because I want static typechecking and I
  // don't want the overheads of lots of casts and of extra space for
  // Vector objects.

  private void addVarInfo(VarInfo vi) {
    VarInfo[] vis = new VarInfo[] { vi };
    addVarInfos(vis);
  }

  private void addVarInfos(VarInfo[] vis) {
    if (vis.length == 0)
      return;
    int old_length = var_infos.length;
    VarInfo[] new_var_infos = new VarInfo[var_infos.length + vis.length];
    System.arraycopy(var_infos, 0, new_var_infos, 0, old_length);
    System.arraycopy(vis, 0, new_var_infos, old_length, vis.length);
    for (int i=old_length; i<new_var_infos.length; i++) {
       new_var_infos[i].value_index = i;
       new_var_infos[i].varinfo_index = i;
       new_var_infos[i].ppt = this;
    }
    var_infos = new_var_infos;
  }

  private void addVarInfos(Vector v) {
    int old_length = var_infos.length;
    VarInfo[] new_var_infos = new VarInfo[var_infos.length + v.size()];
    System.arraycopy(var_infos, 0, new_var_infos, 0, old_length);
    for (int i=0, size=v.size(); i<size; i++) {
      VarInfo vi = (VarInfo) v.elementAt(i);
      new_var_infos[i+old_length] = vi;
      vi.value_index = i+old_length;
      vi.varinfo_index = i+old_length;
      vi.ppt = this;
    }
    var_infos = new_var_infos;
    // Now I still have to add the values of the new variables.
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Adding special variables
  ///

  // I should support this with a HashMapEq which is computed just once.  I
  // don't want to add yet another slot because it's used only while
  // reading files.

  // Given a program point, if it represents a function exit, then
  // return the corresponding function entry point.

  PptTopLevel entry_ppt(PptMap all_ppts) {
    return PptTopLevel.entry_ppt(this, all_ppts);
  }

  static PptTopLevel entry_ppt(PptTopLevel ppt, PptMap all_ppts) {

    // Don't do this, because it returns a value for :::LOOP, etc.
    // if (ppt.name.endsWith(FileIO.enter_tag))
    //   return null;
    // String fn_name = ppt.fn_name();
    // if (fn_name == null)
    //   return null;
    // String entry_ppt_name = (fn_name + FileIO.enter_tag).intern();

    if (!ppt.name.endsWith(FileIO.exit_tag))
      return null;
    String fn_name = ppt.name.substring(0, ppt.name.length() - FileIO.exit_tag.length());
    String entry_ppt_name = (fn_name + FileIO.enter_tag).intern();

    return (PptTopLevel) all_ppts.get(entry_ppt_name);
  }


  // Add "_orig" variables to the program point.
  void add_orig_vars(Ppt entry_ppt) {

    VarInfo[] begin_vis = entry_ppt.var_infos;
    num_orig_vars = begin_vis.length;
    // Don't bother to include the constants.
    Vector new_vis = new Vector(num_orig_vars);
    for (int i=0; i<num_orig_vars; i++) {
      VarInfo vi = begin_vis[i];
      if (vi.isConstant() || vi.isDerived())
	continue;
      new_vis.add(new VarInfo(vi.name + "_orig", vi.type, vi.rep_type, ExplicitVarComparability.makeAlias(vi.name, vi.comparability)));
    }
    addVarInfos(new_vis);
  }



  /// Possibly just blow this off; I'm not sure I care about it.
  /// In any event, leave it until later.
  //
  // void add_invocation_count_vars() {
  //
  //   // Add invocation counts
  //   if (compute_invocation_counts) {
  //     for ppt in fns_to_process {
  //       these_var_infos = fn_var_infos[ppt];
  //       for callee in fn_invocations.keys() {
  // 	calls_var_name = "calls(%s)" % callee;
  // 	these_var_infos.append(var_info(calls_var_name, "integral", "always", len(these_var_infos)));
  // 	these_values.append(fn_invocations[callee]);
  // 	current_var_index++;
  //       }
  //     }
  //   }
  //
  //       (ppt_sans_suffix, ppt_suffix) = (string.split(ppt, ":::", 1) + [""])[0:2]
  //       if ((ppt_suffix != "EXIT")
  // 	  and (ppt_suffix[0:4] != "EXIT")):
  // 	  continue
  //       these_var_infos = fn_var_infos[ppt]
  //       entry_ppt = ppt_sans_suffix + ":::ENTER"
  //       for vi in fn_var_infos[entry_ppt][0:fn_truevars[entry_ppt]]:
  // 	  these_var_infos.append(var_info(vi.name + "_orig", vi.type, comparability_make_alias(vi.name, vi.comparability), len(these_var_infos)))
  //
  // }


  ///////////////////////////////////////////////////////////////////////////
  /// Derived variables
  ///


  // This is here because I think it doesn't make sense to derive except
  // from a PptTopLevel (and possibly a PptConditional?).  Perhaps move it
  // later.

  public static boolean worthDerivingFrom(VarInfo vi) {
    // This prevents derivation from ever occurring on
    // derived variables.  Ought to put this under the
    // control of the individual Derivation objects.

    // System.out.println("worthDerivingFrom(" + vi.name + "): "
    //                    + "derivedDepth=" + vi.derivedDepth()
    //                    + ", isCanonical=" + vi.isCanonical()
    //                    + ", canBeMissing=" + vi.canBeMissing);
    return ((vi.derivedDepth() < 2)
            && (vi.isCanonical())
            && (!vi.canBeMissing));

    // Testing for being canonical is going to be a touch tricky when we
    // integrate derivation and inference, because when something becomes
    // non-canonical we'll have to go back and derive from it, etc.  It's
    // almost as if that is a new variable appearing.  But it did appear in
    // the list until it was found to be equal to another and removed from
    // the list!  I need to decide whether the time savings of not
    // processing the non-canonical variable are worth the time and
    // complexity of making variables non-canonical and possibly canonical
    // again.

    // return (vi.isCanonical()
    //         // This prevents derivation from ever occurring on
    //         // derived variables.  Ought to put this under the
    //         // control of the individual Derivation objects.
    //         && !vi.isDerived());
    // Should add this (back) in:
	    // && !vi.always_missing()
	    // && !vi.always_equal_to_null();
  }


  // There are just two passes of interest right now...
  final static int derivation_passes = 2;

  UnaryDerivationFactory[][] unaryDerivations
    = new UnaryDerivationFactory[][] {
      // pass1
      new UnaryDerivationFactory[] {
	new SequenceLengthFactory() },
      // pass2
      new UnaryDerivationFactory[] {
	new SequenceExtremumFactory(),
	new SequenceMinMaxSumFactory(), } };

  BinaryDerivationFactory[][] binaryDerivations
    = new BinaryDerivationFactory[][] {
      // pass1
      new BinaryDerivationFactory[] { },
      // pass2
      new BinaryDerivationFactory[] {
	new SequenceScalarSubscriptFactory() }
    };


  // This does no inference; it just calls deriveVariablesOnePass once per pass.
  // It returns a Vector of Derivation objects (or are they VarInfos?)

  // If derivation_index == (a, b, c) and n = len(var_infos), then
  // the body of this loop:
  //     * does pass1 introduction for b..a
  //     * does pass2 introduction for c..b
  // and afterward, derivation_index == (n, a, b).
  public Vector derive() {
    // Actually, it should be sorted in *reverse* order.
    // Assert.assert(ArraysMDE.sorted(derivation_indices));

    Vector result = new Vector();
    for (int pass=1; pass<=derivation_passes; pass++) {
      int this_di = derivation_indices[pass];
      int last_di = derivation_indices[pass-1];
      if (debugPptTopLevelDerive)
        System.out.println("pass=" + pass + ", range=" + this_di + ".." + last_di);
      if (this_di == last_di) {
        if (debugPptTopLevelDerive) {
          System.out.println("Bailing...");
        }
	continue;
      }
      result.addAll(deriveVariablesOnePass(this_di, last_di,
					   unaryDerivations[pass-1],
					   binaryDerivations[pass-1]));
    }
    // shift values in derivation_indices:  convert [a,b,c] into [n,a,b]
    // in Python:  derivation_index = (num_vars,) + derivation_indices[:-1]
    for (int i=derivation_passes; i>0; i--)
      derivation_indices[i] = derivation_indices[i-1];
    derivation_indices[0] = var_infos.length + result.size();

    if (debugPptTopLevelDerive) {
      System.out.println(name + ": derived " + result.size() + " new variables; "
                         + "new derivation_indices: "
                         + ArraysMDE.toString(derivation_indices));
      // Alternately, and probably more usefully
      for (int i=0; i<result.size(); i++) {
        // System.out.println("  " + ((Derivation)result.elementAt(i)));
        System.out.println("  " + ((Derivation)result.elementAt(i)).getVarInfo().name);
      }
    }
    return result;
  }


  // This routine does one "pass"; that is, it adds some set of derived
  // variables, according to the functions that are passed in.

  // Returns a vector of Derivation objects.
  // The arguments are:
  //   // VAR_INFOS: only values (partially) computed from these are candidates
  //   VI_INDEX_MIN and VI_INDEX_LIMIT take the place of the old VAR_INFOS arg.
  //   INDICES:  only values (partially) computed from the VarInfo objects at
  //     these indices are candidates.  I could imagine doing this for an
  //     arbitrary list of VarInfo objects as well, but put that off until later.
  //   FUNCTIONS: (long) list of functions for adding new variables; see the code
  Vector deriveVariablesOnePass(int vi_index_min, int vi_index_limit, UnaryDerivationFactory[] unary, BinaryDerivationFactory[] binary) {

    if (debugPptTopLevelDerive)
      System.out.println("deriveVariablesOnePass: vi_index_min=" + vi_index_min
                         + ", vi_index_limit=" + vi_index_limit
                         + ", unary.length=" + unary.length
                         + ", binary.length=" + binary.length);

    Vector result = new Vector();

    for (int i=vi_index_min; i<vi_index_limit; i++) {
      VarInfo vi = var_infos[i];
      if (!worthDerivingFrom(vi)) {
        if (debugPptTopLevelDerive) {
          System.out.println("Not worth deriving from " + vi.name);
        }
	continue;
      }
      for (int di=0; di<unary.length; di++) {
	UnaryDerivationFactory d = unary[di];
        UnaryDerivation[] uderivs = d.instantiate(vi);
        if (uderivs != null) {
          for (int udi=0; udi<uderivs.length; udi++) {
            result.add(uderivs[udi]);
          }
        }
      }
    }

    // I want to get all pairs such that at least one of the elements is
    // under consideration, but I want to generate each such pair only
    // once.  This probably isn't the most efficient technique, but it's
    // probably adequate and is not excessively complicated or excessively
    // slow.
    for (int i1=0; i1<var_infos.length; i1++) {
      VarInfo vi1 = var_infos[i1];
      if (!worthDerivingFrom(vi1)) {
        if (debugPptTopLevelDerive) {
          System.out.println("Not worth deriving from " + vi1.name);
        }
	continue;
      }
      boolean target1 = (i1 >= vi_index_min) && (i1 < vi_index_limit);
      int i2_min = (target1 ? i1+1 : Math.max(i1+1, vi_index_min));
      int i2_limit = (target1 ? var_infos.length : vi_index_limit);
      // if (debugPptTopLevelDerive)
      //   System.out.println("i1=" + i1
      //                      + ", i2_min=" + i2_min
      //                      + ", i2_limit=" + i2_limit);
      for (int i2=i2_min; i2<i2_limit; i2++) {
	VarInfo vi2 = var_infos[i2];
	if (!worthDerivingFrom(vi2)) {
          if (debugPptTopLevelDerive) {
            System.out.println("Not worth deriving from ("
                               + vi1.name + "," + vi2.name + ")");
          }
          continue;
        }
	// if ((!target1) && (ArraysMDE.indexOfEq(var_infos, vi2) == -1))
	//   // Do nothing if neither of these variables is under consideration.
	//   continue;
	for (int di=0; di<binary.length; di++) {
	  BinaryDerivationFactory d = binary[di];
          BinaryDerivation[] bderivs = d.instantiate(vi1, vi2);
          if (bderivs != null) {
            for (int bdi=0; bdi<bderivs.length; bdi++) {
              result.add(bderivs[bdi]);
            }
          }
	}
      }
    }

    for (int i=0; i<result.size(); i++) {
      Assert.assert(result.elementAt(i) instanceof Derivation);
    }

    return result;
  }


  ///
  /// Adding derived variables
  ///

  // This doesn't compute what the derived variables should be, just adds
  // them after being computed.

  void addDerivedVariables(Vector derivs) {
    Derivation[] derivs_array
      = (Derivation[]) derivs.toArray(new Derivation[0]);
    addDerivedVariables(derivs_array);
  }

  void addDerivedVariables(Derivation[] derivs) {

    VarInfo[] vis = new VarInfo[derivs.length];
    for (int i=0; i<derivs.length; i++) {
      vis[i] = derivs[i].getVarInfo();
    }
    addVarInfos(vis);

    // Since I am only modifying members, not making new objects, and since
    // I am using an Eq hash table, I don't need to rehash.
    for (Iterator itor = values.entrySet().iterator() ; itor.hasNext() ; ) {
      Map.Entry entry = (Map.Entry) itor.next();
      ValueTuple vt = (ValueTuple) entry.getKey();
      vt.extend(derivs);
    }
  }




  ///////////////////////////////////////////////////////////////////////////
  /// Manipulating values
  ///

  void add(ValueTuple vt, int count) {
    // System.out.println("PptTopLevel " + name + ": add " + vt);

    values.increment(vt, count);

    // Add to all the views
    // for (Iterator itor = views.keySet().iterator() ; itor.hasNext() ; ) {
    for (Iterator itor = views.iterator() ; itor.hasNext() ; ) {
      PptSliceGeneric view = (PptSliceGeneric) itor.next();
      view.add(vt, count);
      if (view.invs.size() == 0)
        itor.remove();
    }

  }

  // void process() {
  //   throw new Error("To implement");
  // }

  boolean contains(ValueTuple vt) {
    return values.containsKey(vt);
  }

  int count(ValueTuple vt) {
    return values.get(vt);
  }

  Iterator entrySet() {
    return values.entrySet().iterator();
  }



  ///////////////////////////////////////////////////////////////////////////
  ///
  ///

  // This function is called to jump-start processing; it creates all the
  // views (and thus invariants) and derived variables.  Afterward, we just
  // check those invariants.  (That might require us to add more derived
  // variables and invariants, though -- for instance, if an invariant that
  // had suppressed a derived variable becomes falsified.  An equality
  // invariant is an example of such an invariant.)

  // As of 10/25/99, we don't do the second part:  just read the entire
  // data file before doing any processing.

  // The way this works is:
  //  * do derivation by stages
  //  * all inference must be performed over a variable before it may be
  //    derived from.  This implies that as soon as we derive a variable,
  //    we should do all inference over it.
  //  * inference could be by stages, too:  first equality invariants,
  //    then all others.  Probably do this soon.


  public void initial_processing() {

    // I probably can't do anything about it if this is called
    // subsequently; but I should be putting off initial_processing for
    // each program point until it has many samples anyway.
    if (num_samples() == 0)
      return;

    derivation_indices = new int[derivation_passes+1];
    for (int i=1; i<=derivation_passes; i++)
      derivation_indices[i] = 0;
    derivation_indices[0] = 0;
    instantiate_views(0, num_tracevars);

    // Eventually, integrate derivation and inference.  That will require
    // incrementally adding new variables, slices, and invariants.  For
    // now, don't bother:  I want to just get something working first.
    while (derivation_indices[derivation_passes] < var_infos.length) {
      Vector derivations = derive();
      // This only does part of the work:  addVarInfos(derivations);
      addDerivedVariables(derivations);

      Assert.assert(derivation_indices[0] == var_infos.length);
      instantiate_views(var_infos.length - derivations.size(), var_infos.length);
    }
    // System.out.println("Done with initial_processing, " + var_infos.length
    //                    + " variables");
    Assert.assert(derivation_indices[derivation_passes] == var_infos.length);
  }


//         if fn_derived_from.has_key(fn_name):
//             # Don't do any variable derivation, only invariant inference
//             numeric_invariants_over_index(
//                 range(0, len(var_infos)), var_infos, var_values)
//         else:
//             # Perform variable derivation as well as invariant inference
//             fn_derived_from[fn_name] = true
//
//             derivation_functions = (None, pass1_functions, pass2_functions)
//             derivation_passes = len(derivation_functions)-1
//             # First number: invariants are computed up to this index, non-inclusive
//             # Remaining numbers: values have been derived from up to these indices
//             derivation_index = (0,) * (derivation_passes+1)
//
//             # invariant:  len(var_infos) >= invariants_index >= derivation_index[0]
//             #   >= derivation_index[1] >= ...
//
//             while derivation_index[-1] < len(var_infos):
//                 assert util.sorted(derivation_index, lambda x,y:-cmp(x,y))
//                 for i in range(0,derivation_index[1]):
//                     vi = var_infos[i]
//                     assert (not vi.type.is_array()) or vi.derived_len != None or not vi.is_canonical() or vi.invariant.can_be_None or vi.is_derived
//                 if debug_derive:
//                     print "old derivation_index =", derivation_index, "num_vars =", len(var_infos)
//
//                 # If derivation_index == (a, b, c) and n = len(var_infos), then
//                 # the body of this loop:
//                 #     * computes invariants over a..n
//                 #     * does pass1 introduction for b..a
//                 #     * does pass2 introduction for c..b
//                 # and afterward, derivation_index == (n, a, b).
//
//                 # original number of vars; this body may well add more
//                 num_vars = len(var_infos)
//                 if derivation_index[0] != num_vars:
//                     numeric_invariants_over_index(
//                         range(derivation_index[0], num_vars), var_infos, var_values)
//
//                 for pass_no in range(1,derivation_passes+1):
//                     if debug_derive:
//                         print "pass", pass_no, "range", derivation_index[pass_no], derivation_index[pass_no-1]
//                     if derivation_index[pass_no] == derivation_index[pass_no-1]:
//                         continue
//                     introduce_new_variables_one_pass(
//                         var_infos, var_values,
//                         range(derivation_index[pass_no], derivation_index[pass_no-1]),
//                         derivation_functions[pass_no])
//
//                 derivation_index = (num_vars,) + derivation_index[:-1]
//                 if debug_derive:
//                     print "new derivation_index =", derivation_index, "num_vars =", len(var_infos)



  // Old, non-staged implementation that was run before rather than after
  // variable values were read.
  // public void initial_processing() {
  //   derive_all();
  //   instantiate_views();
  // }


  // This may now be identical to initial_processing, which has taken its place.
  // // This isn't in the constructor because it needs to come after adding
  // // _orig variables.
  // void derive_all() {
  //   derivation_indices = new int[derivation_passes+1];
  //   for (int i=1; i<=derivation_passes; i++)
  //     derivation_indices[i] = 0;
  //   derivation_indices[0] = num_tracevars;
  //   // Eventually, integrate derivation and inference.  That will require
  //   // incrementally adding new variables, slices, and invariants.  For
  //   // now, don't bother:  I want to just get something working first.
  //   while (derivation_indices[derivation_passes] < var_infos.length) {
  //     Vector derivations = derive();
  //     addVarInfos(derivations);
  //     instantiate_views(derivations);
  //   }
  // }


  ///////////////////////////////////////////////////////////////////////////
  /// Printing invariants
  ///

  private boolean adding_views;
  private Ppt view_to_remove_deferred = null;

  // Vector of PptSliceGeneric.
  void addViews(Vector slices) {
    adding_views = true;
    // Since I more valuetuples than program points, and program points are
    // likely to disappear midway through processing, put the program point
    // loop outermost.  (Does this make sense?  I'm not sure...)
    for (Iterator slice_itor = slices.iterator() ; slice_itor.hasNext() ; ) {
      Ppt this_slice = (Ppt) slice_itor.next();
      Assert.assert(view_to_remove_deferred == null);
      for (Iterator vt_itor = values.entrySet().iterator() ;
           ((view_to_remove_deferred == null) && vt_itor.hasNext()) ;
           /* no increment in for loop */ ) {
        Map.Entry entry = (Map.Entry) vt_itor.next();
        ValueTuple vt = (ValueTuple) entry.getKey();
        int count = ((Integer) entry.getValue()).intValue();
        this_slice.add(vt, count);
      }
      if (view_to_remove_deferred != null) {
        Assert.assert(view_to_remove_deferred == this_slice);
        // removes this_slice from slice_itor
        slice_itor.remove();
      }
    }

    views.addAll(slices);

    adding_views = false;
  }

  void removeView(Ppt slice) {
    if (adding_views) {
      Assert.assert(view_to_remove_deferred == null);
      view_to_remove_deferred = slice;
    } else {
      boolean removed = views.remove(slice);
      Assert.assert(removed);
    }
  }


  // At present, this needs to occur after deriving variables, because
  // I haven't integrated derivation and inference yet.
  // (This function doesn't exactly belong in this part of the file.)

  // Should return a list of the views created, perhaps.

  // This doesn't work because we have a Vector of Derivation objects, not
  // a vector of VarInfo objects.
  //   // Convenience version.
  //   void instantiate_views(Vector vars) {
  //     if (vars.size() == 0)
  //       return;
  //
  //     for (int i=0; i<vars.size()-1; i++)
  //       Assert.assert( ((VarInfo)vars.elementAt(i)).varinfo_index
  //                      == ((VarInfo)vars.elementAt(i+1)).varinfo_index - 1 );
  //     instantiate_views( ((VarInfo)vars.elementAt(0)).varinfo_index,
  //                        ((VarInfo)vars.elementAt(vars.size()-1)).varinfo_index + 1 );
  //   }

  /**
   * Install views (and thus invariants).
   * This function does cause all invariants over the new views to be computed.
   * The installed views and invariants will all have at least one element with
   * index i such that min_index <= i < index_limit.
   */
  void instantiate_views(int vi_index_min, int vi_index_limit) {
    // System.out.println("instantiate_views: " + this.name
    //                    + ", vi_index_min=" + vi_index_min
    //                    + ", vi_index_limit=" + vi_index_limit
    //                    + ", var_infos.length=" + var_infos.length);

    if (vi_index_min == vi_index_limit)
      return;

//     if var_values == {}:
//         # This function was never executed
//         return

    // if (debugInfer) {
    //   System.out.println("add_views: " + vars);
    // }

    // used only in assertion
    int old_num_vars = var_infos.length;

    Vector new_views = new Vector();

    // Unary slices/invariants.
    for (int i=vi_index_min; i<vi_index_limit; i++) {
      if (var_infos[i].canBeMissing)
        continue;
      // I haven't computed any invariants over it yet -- how am I to know
      // whether it's canonical??
      // if (!var_infos[i].isCanonical())
      //   continue;
      PptSliceGeneric slice1 = new PptSliceGeneric(this, var_infos[i]);
      new_views.add(slice1);
    }

    // This is not always true; the assert was to check it.
    // Assert.assert(var_infos.length == vi_index_limit);

    // Binary slices/invariants.
    for (int i1=0; i1<var_infos.length; i1++) {
      if (var_infos[i1].canBeMissing) {
        if (debugPptTopLevelDerive) {
          System.out.println(var_infos[i1].name + " can be missing");
        }
        continue;
      }
      // I haven't computed any invariants over it yet -- how am I to know
      // whether it's canonical??
      // if (!var_infos[i1].isCanonical())
      //   continue;
      // But I can check if we've already computed invariants over it.
      if ((i1 < vi_index_min) && (!var_infos[i1].isCanonical())) {
        if (debugPptTopLevelDerive) {
          System.out.println("Skipping non-canonical non-target variable1 "
                             + var_infos[i1].name);
        }
        continue;
      }
      boolean target1 = (i1 >= vi_index_min) && (i1 < vi_index_limit);
      int i2_min = (target1 ? i1+1 : Math.max(i1+1, vi_index_min));
      int i2_limit = (target1 ? var_infos.length : vi_index_limit);
      // System.out.println("instantiate_views"
      //                    + "(" + vi_index_min + "," + vi_index_limit + ")"
      //                    + "i1=" + i1
      //                    + ", i2_min=" + i2_min
      //                    + ", i2_limit=" + i2_limit);
      for (int i2=i2_min; i2<i2_limit; i2++) {
        if (var_infos[i2].canBeMissing) {
          if (debugPptTopLevelDerive) {
            System.out.println(var_infos[i2].name + " can be missing");
          }
          continue;
        }
        // I haven't computed any invariants over it yet -- how am I to know
        // whether it's canonical??
        // if (!var_infos[i2].isCanonical())
        //   continue;
        // But I can check if we've already computed invariants over it.
        if ((i2 < vi_index_min) && (!var_infos[i1].isCanonical())) {
          if (debugPptTopLevelDerive) {
            System.out.println("Skipping non-canonical non-target variable2 "
                               + var_infos[i2].name);
          }
          continue;
        }
        // System.out.println("vi1=" + var_infos[i1].name + ";" + var_infos[i1]
        //                    + ", vi2=" + var_infos[i2].name + ";" + var_infos[i2]
        //                    + ", i1=" + i1 + ", i2=" + i2
        //                    + ", vi_index1=" + var_infos[i1].varinfo_index
        //                    + ", vi_index2=" + var_infos[i2].varinfo_index);
        // Assert.assert(i1 < i2);
        // Assert.assert(var_infos[i1].varinfo_index < var_infos[i2].varinfo_index);
        // for (int i=0; i<var_infos.length-1; i++)
        //   Assert.assert(var_infos[i].varinfo_index < var_infos[i+1].varinfo_index);
        // System.out.println("PptSliceGeneric(2): "
        //                    + i1 + "=" + var_infos[i1].name
        //                    + ", " + i2 + "=" + var_infos[i2].name);
        PptSliceGeneric slice2 = new PptSliceGeneric(this, var_infos[i1], var_infos[i2]);
        new_views.add(slice2);
      }
    }

    // Arity 3 is not yet implemented.

    if (debugPptTopLevel)
      System.out.println(new_views.size() + " new views for " + name);

    addViews(new_views);

    // This method didn't add any new variables.
    Assert.assert(old_num_vars == var_infos.length);
  }

  // // At present, this needs to occur after deriving variables, because
  // // I haven't integrated derivation and inference yet.
  // // (This function doesn't exactly belong in this part of the file.)

  // // Should return a list of the views created, perhaps.
  // void instantiate_views() {
  //   // views = new WeakHashMap();
  //   views = new HashSet();
  //   for (int i=0; i<var_infos.length; i++) {
  //     PptSliceGeneric slice1 = new PptSliceGeneric(this, var_infos[i]);
  //     addView(slice1);
  //     for (int j=i+1; j<var_infos.length; j++) {
  //       PptSliceGeneric slice2 = new PptSliceGeneric(this, var_infos[i], var_infos[j]);
  //       addView(slice2);
  //       /// Arity 3 is not yet implemented.
  //       // for (int k=j+1; j<var_infos.length; k++) {
  //       //   PptSliceGeneric slice3 = new PptSliceGeneric(this, var_infos[i], var_infos[j], var_infos[k]);
  //       //   addView(slice3);
  //       // }
  //     }
  //   }
  //   if (debugPptTopLevel)
  //     System.out.println("" + views.size() + " views for " + name);
  // }


  public Iterator invariants() {
    Iterator itorOfItors = new Iterator() {
        Iterator views_itor = views.iterator();
        public boolean hasNext() { return views_itor.hasNext(); }
        public Object next() {
          PptSliceGeneric slice = (PptSliceGeneric) views_itor.next();
          Invariants invs = slice.invs;
          return invs.iterator();
        }
        public void remove() { throw new UnsupportedOperationException(); }
      };
    return new UtilMDE.MergedIterator(itorOfItors);
  }


  // In original implementation, known as print_invariants_ppt
  /*
   * Print invariants for a single program point.
   * Does no output if no samples or no views.
   */
  public void print_invariants_maybe() {
    if (num_samples() == 0)
      return;
    if (views.size() == 0) {
      System.out.println("[No views for " + name + "]");
      return;
    }
    System.out.println("===========================================================================");
    print_invariants();
  }

  /** Print invariants for a single program point. */
  public void print_invariants() {
    System.out.println(name + "  "
		       + num_samples() + " samples");
    System.out.println("    Samples breakdown: "
		       + values.tuplemod_samples_summary());
    System.out.print("    Variables:");
    for (int i=0; i<var_infos.length; i++)
      System.out.print(" " + var_infos[i].name);
    System.out.println();

    // System.out.println("    Variables:");
    // for (int i=0; i<var_infos.length; i++) {
    //   System.out.println("      " + var_infos[i].name
    //                      + " canonical=" + var_infos[i].isCanonical()
    //                      + " equal_to=" + var_infos[i].equal_to);
    // }

    // First, do the equality invariants.  They don't show up in the below
    // because one of the two variables is non-canonical!
    // This technique is a bit non-orthogonal, but probably fine.
    // We might do no output if all the other variables are vacuous.
    // First make sure that we've computed equal_to everywhere.
    // (We might not have so far.)
    for (int i=0; i<var_infos.length; i++)
      var_infos[i].isCanonical();
    for (int i=0; i<var_infos.length; i++) {
      VarInfo vi = var_infos[i];
      if (vi.isCanonical()) {
        Vector equal_vars = vi.equalTo();
        if (equal_vars.size() > 0) {
          StringBuffer sb = new StringBuffer(vi.name);
          for (int j=0; j<equal_vars.size(); j++) {
            VarInfo other = (VarInfo) equal_vars.elementAt(j);
            sb.append(" = ");
            sb.append(other.name);
          }
          System.out.println(sb.toString());
        }
      }
    }

    for (Iterator inv_itor = invariants() ; inv_itor.hasNext() ; ) {
      Invariant inv = (Invariant) inv_itor.next();

      // // I could imagine printing information about the PptSliceGeneric
      // // if it has changed since the last Invariant I examined.
      // PptSliceGeneric slice = (PptSliceGeneric) itor2.next();
      // if (debugPptTopLevel) {
      //   System.out.println("Slice: " + slice.varNames() + "  "
      //                      + slice.num_samples() + " samples");
      //   System.out.println("    Samples breakdown: "
      //                      + slice.values_cache.tuplemod_samples_summary());
      // }

      // It's hard to know in exactly what order to do these checks that
      // eliminate some invariants from consideration.  Which is cheapest?
      // Which is most often successful?

      // This should be a symbolic constant, not an integer literal.
      if (((PptSliceGeneric)inv.ppt).num_mod_non_missing_samples() < 5)
        continue;

      {
        boolean all_canonical = true;
        VarInfo[] vis = inv.ppt.var_infos;
        for (int i=0; i<vis.length; i++) {
          if (! vis[i].isCanonical()) {
            // System.out.println("Suppressing " + inv.repr() + " because " + vis[i].name + " is non-canonical");
            all_canonical = false;
            break;
          }
        }
        if (! all_canonical) {
          if (debugPptTopLevel) {
            System.out.println("[not all vars canonical:  " + inv.repr() + " ]");
            // This *does* happen, because we instantiate all invariants
            // simultaneously. so we don't yet know which of the new
            // variables is canonical.  I should fix this.
            // throw new Error("this shouldn't happen");
          }
          continue;
        }
      }

      if (inv.isObvious()) {
        if (debugPptTopLevel) {
          System.out.println("[obvious:  " + inv.repr() + " ]");
        }
        continue;
      }

      if (!inv.justified()) {
        if (debugPptTopLevel) {
          System.out.println("[not justified:  " + inv.repr() + " ]");
        }
        continue;
      }

      String inv_rep = inv.format();
      if (inv_rep != null) {
        System.out.println(inv_rep);
        if (debugPptTopLevel) {
          System.out.println("  " + inv.repr());
        }
      } else {
        if (debugPptTopLevel) {
          System.out.println("[format returns null: " + inv.repr() + " ]");
        }
      }
    }
  }


  // To be deleted (already exists in commented-out form).
  /** Print invariants for a single program point. */
  public void print_invariants_old() {
    System.out.println(name + "  "
		       + num_samples() + " samples");
    System.out.println("    Samples breakdown: "
		       + values.tuplemod_samples_summary());
    // for (Iterator itor2 = views.keySet().iterator() ; itor2.hasNext() ; ) {
    for (Iterator itor2 = views.iterator() ; itor2.hasNext() ; ) {
      PptSliceGeneric slice = (PptSliceGeneric) itor2.next();
      if (debugPptTopLevel) {
        System.out.println("Slice: " + slice.varNames() + "  "
                           + slice.num_samples() + " samples");
        System.out.println("    Samples breakdown: "
                           + slice.values_cache.tuplemod_samples_summary());
      }
      Invariants invs = slice.invs;
      int num_invs = invs.size();
      for (int i=0; i<num_invs; i++) {
	Invariant inv = (Invariant) invs.elementAt(i);
	String inv_rep = inv.format();
	if (inv_rep != null) {
	  System.out.println(inv_rep);
          if (debugPptTopLevel) {
            System.out.println("  " + inv.repr());
          }
	} else {
          if (debugPptTopLevel) {
            System.out.println("[suppressed: " + inv.repr() + " ]");
          }
	}
      }
    }
  }


  // /** Print invariants for a single program point. */
  // public void print_invariants() {
  //   System.out.println(name + "  "
  //                      + num_samples() + " samples");
  //   System.out.println("    Samples breakdown: "
  //                      + values.tuplemod_samples_summary());
  //   // for (Iterator itor2 = views.keySet().iterator() ; itor2.hasNext() ; ) {
  //   for (Iterator itor2 = views.iterator() ; itor2.hasNext() ; ) {
  //     PptSliceGeneric slice = (PptSliceGeneric) itor2.next();
  //     if (debugPptTopLevel) {
  //       System.out.println("Slice: " + slice.varNames() + "  "
  //                          + slice.num_samples() + " samples");
  //       System.out.println("    Samples breakdown: "
  //                          + slice.values_cache.tuplemod_samples_summary());
  //     }
  //     Invariants invs = slice.invs;
  //     int num_invs = invs.size();
  //     for (int i=0; i<num_invs; i++) {
  //       Invariant inv = invs.elementAt(i);
  //       String inv_rep = inv.format();
  //       if (inv_rep != null) {
  //         System.out.println(inv_rep);
  //         if (debugPptTopLevel) {
  //           System.out.println("  " + inv.repr());
  //         }
  //       } else {
  //         if (debugPptTopLevel) {
  //           System.out.println("[suppressed: " + inv.repr() + " ]");
  //         }
  //       }
  //     }
  //   }
  // }

  /// I need to incorporate all this into the above.

  // def print_invariants_ppt(fn_name, print_unconstrained=0):
  //   """Print invariants for a single program point."""
  //   print fn_name, fn_samples[fn_name], "samples"
  //   var_infos = fn_var_infos[fn_name]
  //
  //   # Single invariants
  //   for vi in var_infos:
  //       if not vi.is_canonical():
  //           continue
  //       this_inv = vi.invariant
  //       if (this_inv.is_exact() and vi.equal_to != []):
  //           # Already printed above in "equality invariants" section
  //           continue
  //       if print_unconstrained or not this_inv.is_unconstrained():
  //           print " ", this_inv.format((vi.name,))
  //   # Pairwise invariants
  //   nonequal_constraints = []
  //   # Maybe this is faster than calling "string.find"; I'm not sure.
  //   nonequal_re = re.compile(" != ")
  //   for vi in var_infos:
  //       if not vi.is_canonical():
  //           continue
  //       vname = vi.name
  //       for (index,inv) in vi.invariants.items():
  //           if type(index) != types.IntType:
  //               continue
  //           if not var_infos[index].is_canonical():
  //               continue
  //           if (not print_unconstrained) and inv.is_unconstrained():
  //               continue
  //           formatted = inv.format((vname, var_infos[index].name))
  //           # Not .match(...), which only checks at start of string!
  //           if nonequal_re.search(formatted):
  //               nonequal_constraints.append(formatted)
  //           else:
  //               print "   ", formatted
  //   for ne_constraint in nonequal_constraints:
  //       print "    ", ne_constraint
  //   # Three-way (and greater) invariants
  //   for vi in var_infos:
  //       if not vi.is_canonical():
  //           continue
  //       vname = vi.name
  //       for (index_pair,inv) in vi.invariants.items():
  //           if type(index_pair) == types.IntType:
  //               continue
  //           (i1, i2) = index_pair
  //           if not var_infos[i1].is_canonical():
  //               # Perhaps err; this shouldn't happen, right?
  //               continue
  //           if not var_infos[i2].is_canonical():
  //               # Perhaps err; this shouldn't happen, right?
  //               continue
  //           if print_unconstrained or not inv.is_unconstrained():
  //               print "     ", inv.format((vname, var_infos[i1].name, var_infos[i2].name))
  //   sys.stdout.flush()



}
