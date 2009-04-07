package daikon.tools;
import java.io.*;
import java.util.*;
import utilMDE.*;
import daikon.*;
import daikon.config.Configuration;
import java.util.regex.*;
import gnu.getopt.*;

/**
 * This tool converts between the uncompressed and compressed Daikon file
 * formats.
 */
public class DtraceConvert {

  public static void main (String[] args) {
    try {
      mainHelper(args);
    } catch (daikon.Daikon.TerminationMessage e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    // Any exception other than daikon.Daikon.TerminationMessage gets
    // propagated.  This simplifies debugging by showing the stack trace.
  }

  /**
   * This entry point is useful for testing.  It returns a boolean to indicate
   * return status instead of croaking with an error.
   **/

  /**
   * This does the work of main, but it never calls System.exit, so it
   * is appropriate to be called progrmmatically.
   * Termination of the program with a message to the user is indicated by
   * throwing daikon.Daikon.TerminationMessage.
   * @see #main(String[])
   * @see daikon.Daikon.TerminationMessage
   **/
  public static void mainHelper(final String[] args) {
    if (args.length == 0) {
      throw new daikon.Daikon.TerminationMessage("No trace files specified.");
    }
    for (String filename : args) {
      dtraceConvert (filename);
    }
  }

  public static void dtraceConvert (String filename) {

    PptMap ppts = new PptMap();

    try {
      FileIO.ParseState state =
        new FileIO.ParseState (filename, false, true, ppts);

      while (state.status != FileIO.ParseStatus.EOF) {
        FileIO.read_data_trace_record (state);
        switch(state.status) {
        case NULL:
          System.out.println("Got null");
          break;
        case TRUNCATED:
          System.out.println("Got truncated");
          break;
        case ERROR:
          System.out.println("Got an error");
          break;
        case EOF:
          System.out.println("Got EOF");
          break;
        case DECL:
          System.out.println("Got a decl");
          break;
        case SAMPLE:
          // This should eventually call
          // processor.process_sample (state.all_ppts, state.ppt, state.vt, state.nonce);
          // but for the time being, it would be enough to hard-code the logic here.
          System.out.println("Got a sample");
          break;
        case COMPARABILITY:
          System.out.println("Got comparability");
          break;
        case LIST:
          System.out.println("Got list");
          break;
        default:
          throw new Error();
        }
      }
    } catch (IOException e) {
      System.out.println();
      e.printStackTrace();
      throw new Error(e);
    }
  }

}
