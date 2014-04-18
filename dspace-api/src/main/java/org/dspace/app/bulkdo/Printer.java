package org.dspace.app.bulkdo;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

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

    public void println(ActionTarget at) {
            String txt = "";
            HashMap<String,Object> map = at.toHashMap();
            for (Integer i = 0; i < keys.length; i++) {
                txt = txt + " " + keys[i] + "=" + expandToString(map.get(keys[i]));
            }
            println(txt);
    }

    public static String expandToString(Object obj) {
        if (obj == null)
            return "";
        if (obj.getClass().isArray()) {
            return deepToString((Object[]) obj);
        }
        return obj.toString();
    }

}

class TSVPrinter extends Printer {

    boolean firstTime = true;

    public TSVPrinter(OutputStream out, String[] keys) {
        super(out, keys);
    }

    /**
     * sloppy TSV output
     * @param at
     */
    @Override
    public void println(ActionTarget at) {
        String txt = "";
        HashMap<String, Object> map = at.toHashMap();
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
            txt = expandToString(map.get(0));
            for (Integer i = 1; i < keys.length; i++) {
                txt = txt + "\t" + expandToString(map.get(keys[i]));
            }
            println(txt);
        }
    }

}