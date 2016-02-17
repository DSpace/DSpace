/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkdo;

import org.apache.commons.lang.ArrayUtils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

import static java.util.Arrays.deepToString;


/**
 * Created by monikam on 4/2/14.
 */
public class Printer extends PrintStream {

    public static final int TXT_FORMAT = 0;
    public static final int TSV_FORMAT = 1;

    public static final String[] formatText = { "TXT", "TSV" };

    protected String[] keys = { "id" };

    public static int getFormat(String format)
    {
        for (int i = 0; i < formatText.length; i++)
        {
            if (formatText[i].equals(format))
            {
                return i;
            }
        }
        return -1;
    }

    public static Printer create(PrintStream p, int format, String[] keys) {
        switch (format) {
            case TSV_FORMAT:
                return new TSVPrinter(p, keys);
            case TXT_FORMAT:
            default:
                return new Printer(p, keys);
        }
    }

    public Printer (OutputStream out, String[] keysToPrint) {
        super(out);
        keys = keysToPrint;
    }

    protected void addKey(String key) {
        for (String k : keys) {
            if (k.equals(key))
                return;
        }
        keys = (String[]) ArrayUtils.add(keys, key);
    }

    public void println(ActionTarget at) {
            String txt = "";
            for (Integer i = 0; i < keys.length; i++) {
                txt = txt + " " + keys[i] + "=" + expandToString(at.get(keys[i]));
            }
            println(txt);
    }

    public static String expandToString(Object obj) {
        if (obj == null)
            return "";
        if (obj.getClass().isArray()) {
            return deepToString((Object[]) obj);
        }
        return obj.toString().replaceAll("\n", " ");
    }

}

class TSVPrinter extends Printer {

    boolean firstTime = true;

    public TSVPrinter(OutputStream out, String[] keys) {
        super(out, keys);
    }

    private static Pattern pattern = Pattern.compile("\\s");

    public static String expandToString(Object obj) {
        String str = Printer.expandToString(obj);
        if (pattern.matcher(str).find()) {
            str = "\"" + str.replaceAll("\"", "\"\"") + "\"";
        }
        return str;
    }

    @Override
    public void println(ActionTarget at) {
        String txt = "";
        if (keys.length > 0) {
            if (firstTime) {
                // print header with key names
                txt = keys[0];
                for (int i = 1; i < keys.length; i++) {
                    txt = txt + "\t" + keys[i];
                }
                println(txt);
            }
            firstTime = false;
            txt = expandToString(at.get(keys[0]));
            for (Integer i = 1; i < keys.length; i++) {
                txt = txt + "\t" + expandToString(at.get(keys[i]));
            }
            println(txt);
        }
    }

}
