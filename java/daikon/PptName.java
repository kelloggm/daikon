package daikon;

import utilMDE.*;

/**
 * ADT which represents naming data associated with a given program
 * point, such as the class or method.
 **/
public class PptName
  implements Serializable
{

  // any of these can be null
  private final String cls;
  private final String method;
  private final String point;

  /**
   * @param name non-null ppt name as given in the decls file
   **/
  public PptName(String name)
  {
    int sep = name.indexOf(FileIO.ppt_tag_separator);
    Assert.assert(sep >= 0);
    String pre_sep = name.substring(0, sep);
    String post_sep = name.substring(sep + FileIO.ppt_tag_separator.length());

    int dot = pre_sep.lastIndexOf('.');
    int lparen = pre_sep.indexOf('(');
    if (lparen == -1) {
      cls = pre_sep;
      method = null;
    } else {
      cls = name.substring(0, dot);
      method = name.substring(dot + 1);
    }
    point = post_sep;
  }

  /**
   * @param className fully-qualified class name
   **/
  public PptName(String className, String methodName, String pointName)
  {
    cls = (className != null) ? className.intern() : null;
    method = (methodName != null) ? methodName.intern() : null;
    point = (pointName != null) ? pointName.intern() : null;

    //System.out.println("\n\n" + className + " -- " + methodName + " -- " + pointName + "\n");
  }

  /**
   * @return the full-qualified class name, which uniquely identifies
   * a given class.
   **/
  public String getFullClassName()
  {
    return cls;
  }

  /**
   * @return the short name of the method, not including any
   * additional context, such as the package it is in.
   **/
  public String getShortClassName()
  {
    if (cls == null) return null;
    int pt = cls.lastIndexOf('.');
    if (pt == -1)
      return cls;
    else
      return cls.substring(0, pt);
  }

  /**
   * @return the full name which can uniquely identify a method within
   * a class.  The name includes symbols for the argument types and
   * return type.
   **/
  public String getFullMethodName()
  {
    return method;
  }
  
  /**
   * @return the name (identifier) of the method, not taking into
   * account any arguments, return values, etc.
   **/
  public String getShortMethodName()
  {
    if (method == null) return null;
    int lparen = method.indexOf('(');
    if (lparen == -1)
      return method;
    else
      return method.substring(0, lparen);
  }

  /**
   * @return true iff this name refers to a synthetic object instance
   * program point
   **/
  public boolean isObjectInstanceSynthetic()
  {
    return FileIO.object_suffix.equals(point);
  }

  /**
   * @return true iff this name refers to a synthetic class instance
   * program point
   **/
  public boolean isClassStaticSynthetic()
  {
    return FileIO.class_static_suffix.equals(point);
  }

  public boolean equals(Object o)
  {
    return (o instanceof PptName) && equals((PptName) o);
  }

  public boolean equals(PptName o)
  {
    return
      (o != null) &&
      (cls == o.cls) &&
      (method == o.method) &&
      (point == o.point) &&
      true;
  }

  public int hashCode()
  {
    // If the domains of the components overlap, we should multiply my
    // primes, but I think they are fairly distinct
    return
      ((cls == null) ? 0 : cls.hashCode()) +
      ((method == null) ? 0 : method.hashCode()) +
      ((point == null) ? 0 : point.hashCode()) +
      0;
  }
  
}
