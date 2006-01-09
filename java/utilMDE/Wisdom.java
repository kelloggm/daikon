package utilMDE;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import static utilMDE.MultiReader.Entry;

/**
 * Wisdom searches a set of files for information.  By default, it
 * searches the file ~/wisdom/root.  A wisdom file can contain one or more
 * entries.  A short entry is a single paragraph (delimited from the
 * next entry by a blank line).  A long entry is introduced by a line
 * that begins with '>entry'.  The remainder of that line is a one
 * line description of the entry.  A long entry is terminated by
 * '@lt;entry', the start of a new long entry, or the start of a new
 * file. <p>
 *
 * Wisdom files can contain comments and can include other files.
 * Comments start with a % sign in the first column.  Any line that
 * starts with a comment is ignored (it is not treated as a blank line
 * for the purpose of separating entries). <p>
 *
 * A file can include another file by including a line of the form
 * '\include{filename}'. <p>
 *
 * The user specifies a set of keywords in which they are interested.
 * Wisdom will look for those keywords in the body of short entries
 * and in the description of long entries.  Entries that contain all
 * of the keywords match.  If multiple entries match, the first line
 * of each is printed.  If only one entry matches, then that entry is
 * printed in its entirety. <p>
 *
 * Options to wisdom are noted below
 **/
public class Wisdom {

  /**
   * Search the body of long entries in addition to the entry's
   * description.  The bodies of short entries are always searched.
   */
  @Option ("-b Search body of long entries for matches")
  public static boolean search_body = false;

  /**
   * By default, if multiple entries are matched, only a synopsis
   * of each entry is printed.  If 'print_all' is selected then
   * the body of each matching entry is printed.
   */
  @Option ("-a Print the entire entry for each match")
  public static boolean print_all = false;

  /**
   * Specifies that keywords are regular expressions.  If false, keywords
   * are text matches
   */
  @Option ("-e Keywords are regular expressions")
  public static boolean regular_expressions = false;

  /**
   * Specify the search list for the file that contains wisdom information.
   * The first file found is used.  Files are separated by colons (:)
   */
  @Option ("-f Specify the search list of files of wisdom information")
  public static String entry_file = "~/wisdom/root";

  /**
   * Specifies which item to print when there are multiple matches
   */
  @Option ("-i Choose a specific item when there are multiple matches")
  public static Integer item_num;

  /**
   * If true, keywords matching is case sensistive.  By default both
   * regular expressions and text keywords are case insensitive.
   */
  @Option ("-c Keywords are case sensistive")
  public static boolean case_sensitive = false;

  /**
   * If true, match a text keyword anywhere that it is found.  By default
   * keywords are searched for only as words.  Words are delimited by
   * any non-alphabetic character.  This option is ignored if
   * regular_expressions is true
   */
  @Option ("-w Only match text keywords against words")
  public static boolean word_match = false;

  /**
   * If true show the filename/line number of each matching entry
   * in the output
   */
  @Option ("-l Show the location of each matching entry")
  public static boolean show_location = false;

  /** Comments start with percent signs in the first column **/
  private static String comment_re = "^%.*";

  /** Include directive is of the form \include{filename} **/
  private static String include_re = "\\\\include\\{(.*)\\}";

  /** Platform specific line separator **/
  private static final String lineSep = System.getProperty("line.separator");


  /**
   * Look for the specified keywords in the wisdom file(s) and prints
   * the corresponding entries
   */
  public static void main (String args[]) throws IOException {

    Options options = new Options (Wisdom.class);
    String[] keywords = options.parse_and_usage (args,
                               "wisdom [options] <keyword> <keyword> ...");


    // Find our own entry if there are no arguments
    if (keywords.length == 0)
      keywords = new String[] {"help program"};

    // Open the first readable root file
    MultiReader reader = null;
    String entry_files[] = entry_file.split (":");
    List<Exception> file_errors = new ArrayList<Exception>();
    for (String ef : entry_files) {
      ef = UtilMDE.fix_filename (ef);
      try {
        reader = new MultiReader (ef, comment_re, include_re);
      } catch (FileNotFoundException e) {
        file_errors.add (e);
      }
      if (reader != null)
        break;
    }
    if (reader == null) {
      System.out.println ("Error: Can't read any entry files");
      for (Exception file_error : file_errors)
        System.out.printf ("  entry file %s%n", file_error.getMessage());
      System.exit (254);
    }

    // Setup the regular expressions for long entries
    reader.set_entry_start_stop ("^>entry", "^<entry");

    List<Entry> matching_entries = new ArrayList<Entry>();

    try {
      // Process each entry looking for matches
      Entry entry = reader.get_entry ();
      while (entry != null) {
        int matchcount = 0;
        for (String keyword : keywords) {
          String search = entry.first_line;
          if (search_body || entry.short_entry)
            search = entry.body;
          if (!case_sensitive) {
            search = search.toLowerCase();
          }
          if (regular_expressions) {
            int flags = Pattern.CASE_INSENSITIVE;
            if (case_sensitive)
              flags = 0;
            if (Pattern.compile (keyword, flags).matcher(search).find())
              matchcount++;
          } else {
            if (!case_sensitive)
              keyword = keyword.toLowerCase();
            if (word_match) {
              keyword = "[^a-zA-z]" + keyword + "[^a-zA-z]";
              if (Pattern.compile (keyword).matcher(search).find())
                matchcount++;
            } else if (search.contains(keyword))
              matchcount++;
          }
        }
        if (matchcount == keywords.length)
          matching_entries.add (entry);
        entry = reader.get_entry ();
      }
    } catch (FileNotFoundException e) {
      System.out.printf ("Error: Can't read %s at line %d in file %s%n",
                         e.getMessage(), reader.get_line_number(),
                         reader.get_filename());
      System.exit (254);
    }

    // Print the results
    if (matching_entries.size() == 0) {
      System.out.println ("No help found");
    } else if (matching_entries.size() == 1) {
      Entry e = matching_entries.get(0);
      if (show_location)
        System.out.printf ("%s:%d%n", e.filename, e.line_number);
      System.out.print (e.body);
    } else { // there must be multiple matches
      if (item_num != null) {
        Entry e = matching_entries.get (item_num-1);
        if (show_location)
          System.out.printf ("%s:%d%n", e.filename, e.line_number);
        System.out.print (e.body);
      } else {
        int i = 0;
        if (print_all)
          System.out.printf ("%d matches found (separated by dashes "
                              +"below)%n", matching_entries.size());
        else
          System.out.printf ("%d matches found. Use the -i switch to print a "
                             + "specific match or -a to see them all%n",
                             matching_entries.size());

        for (Entry e : matching_entries) {
          i++;
          if (print_all) {
            if (show_location)
              System.out.printf ("%n-------------------------%n%s:%d%n",
                                 e.filename, e.line_number);
            else
              System.out.printf ("%n-------------------------%n");
            System.out.print (e.body);
          } else {
            if (show_location)
              System.out.printf ("  -i=%d %s:%d %s%n", i, e.filename,
                                 e.line_number, e.first_line);
            else
              System.out.printf ("  -i=%d %s%n", i, e.first_line);

          }
        }
      }
    }
  }

  /**
   * Returns the next entry.  If no more entries are available returns null.
   */
  public static Entry old_get_entry (MultiReader reader) throws IOException {

    try {

      // Skip any preceeding blank lines
      String line = reader.readLine();
      while ((line != null) && (line.trim().length() == 0))
        line = reader.readLine();
      if (line == null)
        return (null);

      String body = "";
      Entry entry = null;
      String filename = reader.get_filename();
      long line_number = reader.get_line_number();

      // If this is a long entry
      if (line.startsWith (">entry")) {

        // Get the current filename
        String current_filename = reader.get_filename();

        // Remove '>entry' from the line
        line = line.replaceFirst ("^>entry *", "");
        String first_line = line;

        // Read until we find the termination of the entry
        while ((line != null) && !line.startsWith (">entry") &&
               !line.equals ("<entry")
               && current_filename.equals (reader.get_filename())) {
          body += line + lineSep;
          line = reader.readLine();
        }

        // If this entry was terminated by the start of the next one,
        // put that line back
        if ((line != null) && (line.startsWith (">entry")
                           || !current_filename.equals (reader.get_filename())))
          reader.putback (line);

        entry = new Entry (first_line, body, filename, line_number, false);

      } else { // blank separated entry

        String first_line = line;

        // Read until we find another blank line
        while ((line != null) && (line.trim().length() != 0)) {
          body += String.format ("%s%n", line);
          line = reader.readLine();
        }

        entry = new Entry (first_line, body, filename, line_number, true);
      }

      return (entry);

    } catch (FileNotFoundException e) {
      System.out.printf ("Error: Can't read %s at line %d in file %s%n",
                         e.getMessage(), reader.get_line_number(),
                         reader.get_filename());
      System.exit (254);
      return (null);
    }
  }

  /** Returns the first line of entry **/
  public static String first_line (String entry) {

    int ii = entry.indexOf (lineSep);
    if (ii == -1)
      return entry;
    return entry.substring (0, ii);
  }
}
