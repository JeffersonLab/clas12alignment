package org.clas.test;

import java.lang.NumberFormatException;
import java.util.HashMap;
import java.util.Map;

/** Handler of all input-output of the program. */
public final class IOHandler {
    // No global static classes are allowed in java so this is the closest second...
    private IOHandler() {}

    private static char[] argnames = {'n', 'v', 'i', 'l', 'd'};
    static Map<String, Character> argmap;

    /** Associate char-indexed args with String-indexed args. */
    private static boolean initialize_argmap() {
    argmap = new HashMap<>();
        for (char argname : argnames) {
            if      (argname == 'n') argmap.put("--nevents", 'n');
            else if (argname == 'v') argmap.put("--var",     'v');
            else if (argname == 'i') argmap.put("--init",    'i');
            else if (argname == 'l') argmap.put("--last",    'l');
            else if (argname == 'd') argmap.put("--delta",   'd');
            else return true; // Programmer error.
        }
        return false;
    }

    /** Print usage to stdout and exit. */
    private static boolean usage() {
        System.out.printf("Usage: fmt_alignment [-n --nevents] [-v --var] <file>\n");
        System.out.printf("  * nevents : number of events to run. If unspecified, runs all\n");
        System.out.printf("              events in input file.\n");
        System.out.printf("  * var     : variable to be aligned. Can be dXY, dZ, rXY, or rZ.\n");
        System.out.printf("  * init    : minimum value to be tested for alignment.\n");
        System.out.printf("  * last    : maximum value to be tested.\n");
        System.out.printf("  * delta   : length of intervals between <init> and <last>.\n");
        System.out.printf("  * file    : hipo input file.\n");
        System.out.printf("  For example, if <var> == dZ, <init> == -0.2, <last> == 0.2, and\n");
        System.out.printf("  <delta> == 0.1, then the values tested for z are:\n");
        System.out.printf("              (-0.2, 0.1, 0.0, 0.1, 0.2).\n");
        System.out.printf("  If <var>, <init>, <last>, or <delta> are specified, all the others\n");
        System.out.printf("  arguments must be specified too.\n");
        System.out.printf("  If no argument is specified, a plot showing the residuals is shown.\n");
        System.out.printf("  NOTE. All measurements are in cm, while the ccdb works in mm!\n");
        return true;
    }

    /** Check if an argument can be parsed into an int. */
    private static boolean check_int(String arg) {
        try {Integer.parseInt(arg);}
        catch (NumberFormatException e) {return true;}
        return false;
    }

    /** Check if an argument can be parsed into a double. */
    private static boolean check_double(String arg) {
        try {Double.parseDouble(arg);}
        catch (NumberFormatException e) {return true;}
        return false;
    }

    /** Arguments parser. Arguments are detailed in usage(). */
    public static boolean parse_args(String[] args, Map<Character, String> params) {
        // NOTE. Better error messages here would be cool, but not necessary atm.
        if (initialize_argmap()) return true;
        if (args.length < 1) return usage();

        // Get args. Can't believe that Java doesn't have a standard method for this.
        for (int i=0; i<args.length; ++i) {
            final String a = args[i];
            if (args[i].charAt(0) == '-') {
                if (args[i].length() < 2) return usage();
                if (args[i].charAt(1) == '-') { // string-indexed argument.
                    if (args[i].length() < 3) return usage();
                    if (argmap.get(args[i]) == null) return usage();
                    params.put(argmap.get(args[i]), args[++i]);
                }
                else { // char-indexed argument.
                    if (args[i].length() > 2) return usage();
                    params.put(args[i].substring(args[i].length()-1).charAt(0), args[++i]);
                }
            }
            else { // positional argument.
                params.put('f', args[i]);
            }
        }

        // Check that positional args are correct.
        if (params.get('f') == null) return usage();

        // If one argument is non-null, all arguments must be non-null.
        if ((params.get('v') != null || params.get('i') != null
                || params.get('l') != null || params.get('d') != null)
        && (params.get('v') == null || params.get('i') == null
                || params.get('l') == null || params.get('d') == null))
            return usage();

        // Check that no arg is outside accepted keys.
        for (Character key : params.keySet()) {
            if (key == 'f') continue;
            boolean accept = false;
            for (int i = 0; i < argnames.length; ++i) {
                if (key == argnames[i]) {
                    accept = true;
                    break;
                }
            }
            if (!accept) return usage();
        }

        // Check that args are of correct type.
        if (!params.get('f').endsWith(".hipo")) return usage();
        if (params.get('n') != null && check_int(params.get('n'))) return usage();
        if (params.get('v') != null) {
            String var = params.get('v');
            if (!var.equals("dXY") && !var.equals("dZ") && !var.equals("rXY") && !var.equals("rZ"))
                return usage();

            if (check_double(params.get('i'))) return usage();
            if (check_double(params.get('l'))) return usage();
            if (check_double(params.get('d'))) return usage();
        }

        return false;
    }
}
