package org.dspace.app.bulkdo;


import sun.tools.attach.HotSpotAttachProvider;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by monikam on 4/2/14.
 */
public class Printer extends PrintStream {

    public static final int TXT_FORMAT = 0;
    public static final int TSV_FORMAT = 1;

    public static final String[] formatText = { "TXT", "TSV" };

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

    public static Printer create(PrintStream p, int format) {
        switch (format) {
            case TSV_FORMAT:
                return new TSVPrinter(p);
            case TXT_FORMAT:
            default:
                return new Printer(p);
        }
    }

    public Printer (OutputStream out) {
        super(out);
    }

    public void println(ActionTarget at) {
            String txt = "";
            HashMap<String,Object> map = at.toHashMap();
            for (Map.Entry entry : map.entrySet()) {
                Object v = entry.getValue();
                if (null == v) {
                    v = "";
                }
                txt = txt + " " + entry.getKey() + "=" + v.toString();
            }
            println(txt);
    }

}

class TSVPrinter extends Printer {

    boolean firstTime = true;

    public TSVPrinter(OutputStream out) {
        super(out);
    }

    /**
     * sloppy TSV output
     * @param at
     */
    @Override
    public void println(ActionTarget at) {
        String txt = "";
        HashMap<String, Object> map = at.toHashMap();
        if (firstTime) {
            // print header wit key names
            for (Map.Entry entry : map.entrySet()) {
                Object v = entry.getValue();
                txt = txt + "\t" + entry.getKey();
            }
            println(txt);
        }
        firstTime = false;
        txt = "";
        for (Map.Entry entry : map.entrySet()) {
            Object v = entry.getValue();
            if (null == v) {
                v = "";
            }
            txt = txt + "\t" + v.toString();
        }
        println(txt);
    }

}