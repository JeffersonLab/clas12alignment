package org.clas.test;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.NumberFormatException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Handler of all input-output of the program. */
public final class IOHandler {
    private IOHandler() {}

    private static Set<Character> L1ARGS =
            new HashSet<>(Arrays.asList('c', 'n', 'p', 'v', 'V'));
    private static Set<Character> L2ARGS =
            new HashSet<>(Arrays.asList('i'));
    private static Set<Character> L3ARGS =
            new HashSet<>(Arrays.asList('s'));
    private static Set<Character> LFMTARGS =
            new HashSet<>(Arrays.asList('x', 'y', 'z', 'X', 'Y', 'Z'));
    private static Map<String, Character> argmap;

    /**
     * Write a short error report to file `error_report.txt`. This file will be
     *     then read, printed, and deleted by `run.sh`.
     */
    private static boolean writeErrReport(String report) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream("error_report.txt"), "utf-8"))) {
            writer.write(report);
        } catch (Exception ex) {
            // If error_report.txt cannot be written, simply print the report to stdout and hope the
            //     user catches it.
            System.out.printf("\n\n\n%s\n\n\n", report);
        }

        return true;
    }

    /** Associate char-indexed args with String-indexed args. */
    private static boolean initArgmap() {
        argmap = new HashMap<>();
        for (char argname : L1ARGS) {
            if      (argname == 'c') argmap.put("--cutsinfo",  'c');
            else if (argname == 'n') argmap.put("--nevents",   'n');
            else if (argname == 'p') argmap.put("--plot",      'p');
            else if (argname == 'v') argmap.put("--var",       'v');
            else if (argname == 'V') argmap.put("--variation", 'V');
            else return true; // Programmer error.
        }
        for (char argname : L2ARGS) {
            if      (argname == 'i') argmap.put("--inter",     'i');
            else return true; // Programmer error.
        }
        for (char argname : L3ARGS) {
            if      (argname == 's') argmap.put("--swim",      's');
            else return true; // Programmer error.
        }
        for (char argname : LFMTARGS) {
            if      (argname == 'x') argmap.put("--dx",        'x');
            else if (argname == 'y') argmap.put("--dy",        'y');
            else if (argname == 'z') argmap.put("--dz",        'z');
            else if (argname == 'X') argmap.put("--rx",        'X');
            else if (argname == 'Y') argmap.put("--ry",        'Y');
            else if (argname == 'Z') argmap.put("--rz",        'Z');
            else return true; // Programmer error.
        }
        return false;
    }

    /** Check if an argument can be parsed into an int. */
    private static boolean checkInt(String arg) {
        try {Integer.parseInt(arg);}
        catch (NumberFormatException e) {return true;}
        return false;
    }

    /** Check if an argument can be parsed into a double. */
    private static boolean checkDouble(String arg) {
        try {Double.parseDouble(arg);}
        catch (NumberFormatException e) {return true;}
        return false;
    }

    /** Arguments parser. Arguments are detailed in usage.txt. */
    public static boolean parseArgs(String[] args, Map<Character, List<String>> params) {
        // NOTE. Better error messages here would be cool, but not strictly necessary at the moment.
        if (initArgmap()) return true;

        // Get args.
        for (int i = 0; i < args.length; ++i) {
            final String argS = args[i];
            char argC;
            if (argS.charAt(0) == '-') {
                int count = 0;
                if (argS.length() < 2)
                    return writeErrReport("char-indexed argument `" + argS + "` is invalid.");
                if (argS.charAt(1) == '-') { // string-indexed argument.
                    if (argS.length() < 3)
                        return writeErrReport("argument: `" + argS + "` is invalid.");
                    if (argmap.get(argS) == null)
                        return writeErrReport("argument: `" + argS + "` is unknown.");
                    argC = argmap.get(argS);
                }
                else { // char-indexed argument.
                    if (argS.length() > 2) {
                        return writeErrReport(
                            "string-indexed argument `" + argS + "` should have two dashes."
                        );
                    }
                    argC = argS.charAt(1);
                }
                params.put(argC, new ArrayList<String>());
                if      (L1ARGS.contains(argC))   count = 1;
                else if (L2ARGS.contains(argC))   count = 2;
                else if (L3ARGS.contains(argC))   count = 3;
                else if (LFMTARGS.contains(argC)) count = Constants.FMTLAYERS;
                else
                    return writeErrReport("Argument `" + argC + "` is unknown.");
                for (int j = 0; j < count; ++j) {
                    if (i == args.length-1)
                        return writeErrReport("Argument `" + argS + "` needs more values.");
                    params.get(argC).add(args[++i]);
                }
            }
            else { // positional argument.
                if (params.get('f') != null)
                    return writeErrReport("Program can only receive one positional argument.");
                params.put('f', new ArrayList<String>());
                params.get('f').add(argS);
            }
        }

        // Check that args are of correct type.
        if (params.get('f') == null)
            return writeErrReport("Missing input hipo file.");
        if (!params.get('f').get(0).endsWith(".hipo")) {
            return writeErrReport(
                "Input file `" + params.get('f').get(0) + "` is not a valid hipo file."
            );
        }
        for (Map.Entry<Character, List<String>> entry : params.entrySet()) {
            Character    key = entry.getKey();
            List<String> vals = entry.getValue();
            for (String val : vals) {
                if (val == null)
                    return writeErrReport("Key `" + key + "` has a null value.");
                if ((key.equals('c') || key.equals('n') || key.equals('p')) && checkInt(val)) {
                    return writeErrReport(
                        "Key `"+key+"` requires integer values, while `"+val+"` was provided."
                    );
                }
                if (key.equals('v') && (
                    !val.equals("dXY") && !val.equals("dZ") &&
                    !val.equals("rXY") && !val.equals("rZ"))
                ) {
                    return writeErrReport(
                        "Key `-v` only accepts `dXY`, `dZ`, `rXY`, or `rZ`. `"+val+"` was provided."
                    );
                }
                if (
                    (L2ARGS.contains(key) || L3ARGS.contains(key) || LFMTARGS.contains(key)) &&
                    checkDouble(val)
                ) {
                    return writeErrReport(
                        "Key `" + key + "` only accepts double values. `" + val + "` was provided."
                    );
                }
            }
        }
        if ((params.get('v') == null) != (params.get('i') == null))
            return writeErrReport("If `-v` is specified, `-i` must be too (and vice versa).");

        return false;
    }

    /** Prints all parameters. Useful for debugging. */
    private boolean printParams(Map<Character, List<String>> params) {
        System.out.printf("\n");
        for (Character key : params.keySet()) {
            System.out.printf("\n%c :", key);
            for (String val : params.get(key)) System.out.printf(" %s", val);
        }
        System.out.printf("\n\n");

        return false;
    }
}
