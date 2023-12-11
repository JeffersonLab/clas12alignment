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
                if (argS.length() < 2) {
                    System.out.printf("\n[ERROR] Invalid char-indexed optional argument: ");
                    System.out.printf("``%s''", argS);
                    return true;
                }
                if (argS.charAt(1) == '-') { // string-indexed argument.
                    if (argS.length() < 3) {
                        System.out.printf("\n[ERROR] Invalid string-indexed optional argument: ");
                        System.out.printf("``%s''", argS);
                        return true;
                    }
                    if (argmap.get(argS) == null) {
                        System.out.printf("\n[ERROR] Unknown argument: ``%s''", argS);
                        return true;
                    }
                    argC = argmap.get(argS);
                }
                else { // char-indexed argument.
                    if (argS.length() > 2) {
                        System.out.printf("\n[ERROR] String-indexed argument needs two dashes: ");
                        System.out.printf("``%s''", argS);
                        return true;
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
                    return true;
                }
                for (int j = 0; j < count; ++j) {
                    if (i == args.length-1) {
                        System.out.printf("\n[ERROR] Key ``%s'' needs more values!", argS);
                        return true;
                    }
                    params.get(argC).add(args[++i]);
                }
            }
            else { // positional argument.
                if (params.get('f') != null) {
                    System.out.printf("\n[ERROR] Only one positional argument is allowed.");
                    return true;
                }
                params.put('f', new ArrayList<String>());
                params.get('f').add(argS);
            }
        }

        // Check that args are of correct type.
        if (params.get('f') == null) {
            System.out.printf("\n[ERROR] File to be opened needs to be added as positional arg.");
            return true;
        }
        if (!params.get('f').get(0).endsWith(".hipo")) {
            System.out.printf("\n[ERROR] Program can only process .hipo files. filename is ");
            System.out.printf("``%s''", params.get('f').get(0));
            return true;
        }
        for (Map.Entry<Character, List<String>> entry : params.entrySet()) {
            Character    key = entry.getKey();
            List<String> vals = entry.getValue();
            for (String val : vals) {
                if (val == null) {
                    System.out.printf("[ERROR] Key ``-%s'' has a null value.", key);
                    return true;
                }
                if ((key.equals('c') || key.equals('n') || key.equals('p'))
                        && checkInt(val)) {
                    System.out.printf("[ERROR] Key ``-%s'' requires integer values ", key);
                    System.out.printf("(``%s'' provided)", val);
                    return true;
                }
                if (key.equals('v') && (!val.equals("dXY") && !val.equals("dZ")
                                     && !val.equals("rXY") && !val.equals("rZ"))) {
                    System.out.printf("[ERROR] The only allowed values for key ``-v'' are ``dXY''");
                    System.out.printf(", ``dZ'', ``rXY'', and ``rZ'' (``%s'' provided).", val);
                    return true;
                }
                if ((L2ARGS.contains(key) || L3ARGS.contains(key) || LFMTARGS.contains(key))
                        && checkDouble(val)) {
                    System.out.printf("[ERROR] Key ``%s'' requires Double values", key);
                    System.out.printf(" (``%s'' provided).", val);
                    return true;
                }
            }
        }
        if ((params.get('v') == null) != (params.get('i') == null)) {
            System.out.printf("[ERROR] Both ``-v'' and ``i'' arguments need to be specified if ");
            System.out.printf("one of them is.");
            return true;
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
