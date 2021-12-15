package org.clas.test;

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

    /** Print usage to stdout and exit. */
    private static boolean usage() {
        System.out.printf("\n");
        System.out.printf("Usage: alignment <file> [-n --nevents] [-v --var] [-i --inter]\n");
        System.out.printf("                        [-s --swim] [-c --cutsinfo] [-V --variation]\n");
        System.out.printf("                        [-p -plot]\n");
        System.out.printf("                        [-x --dx] [-y --dy] [-z --dz]\n");
        System.out.printf("                        [-X --rx] [-Y --ry] [-Z --rz]\n");
        System.out.printf("  * file      : hipo input file.\n");
        System.out.printf("  * nevents   : number of events to run. If unspecified, runs all\n");
        System.out.printf("                events in input file.\n");
        System.out.printf("  * var       : variable to be aligned. Can be dXY, dZ, rXY, or rZ.\n");
        System.out.printf("  * inter (2) : [0] range between nominal position and position to\n");
        System.out.printf("                    be tested.\n");
        System.out.printf("                [1] step size for each tested value between\n");
        System.out.printf("                    <nominal - range> and <nominal + range>.\n");
        System.out.printf("  * swim  (3) : Setup for the Swim class. If unspecified, uses\n");
        System.out.printf("                default from RG-F data (-0.75, -1.0, 3.0).\n");
        System.out.printf("                [0] Solenoid magnet scale.\n");
        System.out.printf("                [1] Torus magnet scale.\n");
        System.out.printf("                [2] Solenoid magnet shift.\n");
        System.out.printf("  * cutsinfo  : int describing how much info on the cuts should be\n");
        System.out.printf("                printed. 0 is no info, 1 is minimal, 2 is detailed.\n");
        System.out.printf("                Default is 1.\n");
        System.out.printf("  * variation : CCDB variation to be used. Default is\n");
        System.out.printf("                ``rgf_spring2020''.\n");
        System.out.printf("  * plot      : int describing if plots are to be shown. 1 means\n");
        System.out.printf("                show them. Plots are always saved in the\n");
        System.out.printf("                ``histograms.hipo'' file.\n");
        System.out.printf("  * dx    (3) : x shift for each FMT layer.\n");
        System.out.printf("  * dy    (3) : y shift for each FMT layer.\n");
        System.out.printf("  * dz    (3) : z shift for each FMT layer.\n");
        System.out.printf("  * rx    (3) : x rotation for each FMT layer.\n");
        System.out.printf("  * ry    (3) : y rotation for each FMT layer.\n");
        System.out.printf("  * rz    (3) : z rotation for each FMT layer.\n");
        System.out.printf("\n");
        System.out.printf("For example, if <var> == 'dZ', <inter> == '0.2 0.1', and\n");
        System.out.printf("<dz> == 0.5, then the values tested for z are:\n");
        System.out.printf("            (0.3, 0.4, 0.5, 0.6, 0.7).\n");
        System.out.printf("If a position or rotation is not specified, it is assumed to be 0\n");
        System.out.printf("for all FMT layers. If no argument is specified, a plot showing\n");
        System.out.printf("the residuals is shown.\n");
        System.out.printf("\n");
        System.out.printf("NOTE. All measurements are in cm, while the ccdb works in mm.\n");
        System.out.printf("\n");
        return true;
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

    /** Arguments parser. Arguments are detailed in usage(). */
    public static boolean parseArgs(String[] args, Map<Character, List<String>> params) {
        // NOTE. Better error messages here would be cool, but not strictly necessary at the moment.
        if (initArgmap()) return true;
        if (args.length < 1) {
            System.out.printf("\n[ERROR] File to be opened needs to be added as positional arg.");
            return usage();
        }

        // Get args.
        for (int i = 0; i < args.length; ++i) {
            final String argS = args[i];
            char argC;
            if (argS.charAt(0) == '-') {
                int count = 0;
                if (argS.length() < 2) {
                    System.out.printf("\n[ERROR] Invalid char-indexed optional argument: ");
                    System.out.printf("``%s''", argS);
                    return usage();
                }
                if (argS.charAt(1) == '-') { // string-indexed argument.
                    if (argS.length() < 3) {
                        System.out.printf("\n[ERROR] Invalid string-indexed optional argument: ");
                        System.out.printf("``%s''", argS);
                        return usage();
                    }
                    if (argmap.get(argS) == null) {
                        System.out.printf("\n[ERROR] Unknown argument: ``%s''", argS);
                        return usage();
                    }
                    argC = argmap.get(argS);
                }
                else { // char-indexed argument.
                    if (argS.length() > 2) {
                        System.out.printf("\n[ERROR] String-indexed argument needs two dashes: ");
                        System.out.printf("``%s''", argS);
                        return usage();
                    }
                    argC = argS.charAt(1);
                }
                params.put(argC, new ArrayList<String>());
                if      (L1ARGS.contains(argC))   count = 1;
                else if (L2ARGS.contains(argC))   count = 2;
                else if (L3ARGS.contains(argC))   count = 3;
                else if (LFMTARGS.contains(argC)) count = Constants.FMTLAYERS;
                else {
                    System.out.printf("\n[ERROR] Unknown argument: ``-%s''", argC);
                    return usage();
                }
                for (int j = 0; j < count; ++j) {
                    if (i == args.length-1) {
                        System.out.printf("\n[ERROR] Key ``%s'' needs more values!", argS);
                        return usage();
                    }
                    params.get(argC).add(args[++i]);
                }
            }
            else { // positional argument.
                if (params.get('f') != null) {
                    System.out.printf("\n[ERROR] Only one positional argument is allowed.");
                    return usage();
                }
                params.put('f', new ArrayList<String>());
                params.get('f').add(argS);
            }
        }

        // Check that args are of correct type.
        if (params.get('f') == null) {
            System.out.printf("\n[ERROR] File to be opened needs to be added as positional arg.");
            return usage();
        }
        if (!params.get('f').get(0).endsWith(".hipo")) {
            System.out.printf("\n[ERROR] Program can only process .hipo files. filename is ");
            System.out.printf("``%s''", params.get('f').get(0));
            return usage();
        }
        for (Map.Entry<Character, List<String>> entry : params.entrySet()) {
            Character    key = entry.getKey();
            List<String> vals = entry.getValue();
            for (String val : vals) {
                if (val == null) {
                    System.out.printf("[ERROR] Key ``-%s'' has a null value.", key);
                    return usage();
                }
                if ((key.equals('c') || key.equals('n') || key.equals('p'))
                        && checkInt(val)) {
                    System.out.printf("[ERROR] Key ``-%s'' requires integer values ", key);
                    System.out.printf("(``%s'' provided)", val);
                    return usage();
                }
                if (key.equals('v') && (!val.equals("dXY") && !val.equals("dZ")
                                     && !val.equals("rXY") && !val.equals("rZ"))) {
                    System.out.printf("[ERROR] The only allowed values for key ``-v'' are ``dXY''");
                    System.out.printf(", ``dZ'', ``rXY'', and ``rZ'' (``%s'' provided).", val);
                    return usage();
                }
                if ((L2ARGS.contains(key) || L3ARGS.contains(key) || LFMTARGS.contains(key))
                        && checkDouble(val)) {
                    System.out.printf("[ERROR] Key ``%s'' requires Double values", key);
                    System.out.printf(" (``%s'' provided).", val);
                    return usage();
                }
            }
        }
        if ((params.get('v') == null) != (params.get('i') == null)) {
            System.out.printf("[ERROR] Both ``-v'' and ``i'' arguments need to be specified if ");
            System.out.printf("one of them is.");
            return usage();
        }

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
