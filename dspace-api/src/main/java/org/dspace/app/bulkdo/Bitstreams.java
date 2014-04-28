package org.dspace.app.bulkdo;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * Created by monikam on 4/25/14.
 */
public class Bitstreams {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(Bitstreams.class);

    Context context;
    Bitstream bit;
    String filename;
    InputStream stream;
    BitstreamFormat  fileFormat;
    boolean ignoreFormatMismatch;

    Bitstreams(BitstreamsArguments args) {
        context = args.getContext();
        bit = (Bitstream) args.getRoot();
        filename = args.filename;
        stream = args.stream;
        fileFormat = args.format;
        ignoreFormatMismatch = args.gogo;

        log.debug(this);
    }

    public String toString()  {
        return filename + "(" + fileFormat.getMIMEType() + ") ->" + bit;
    }

    void apply(Printer p) {
        p.addKey("BUNDLE.object");
        p.addKey("replace");
        p.addKey("success");
        p.addKey("bundles");

        BitstreamActionTarget target = new BitstreamActionTarget(null, bit);
        HashMap<String, Object> map = target.toHashMap();
        map.put("replace", filename);
        String result = "";
        try {
            Bundle[] bdls = replaceBitstream();
            map.put("bundles", bdls);
            result = "success";
        } catch (Exception e) {
            result = e.getMessage();
            e.printStackTrace();
        }
        map.put("success", result);
        p.println(target);

    }

    private Bundle[] replaceBitstream() throws SQLException, IOException, AuthorizeException {
        if (! bit.getFormat().getMIMEType().equals(fileFormat) && !ignoreFormatMismatch) {
                throw new RuntimeException("format mistmatch");
        }
        Item item = (Item) bit.getParentObject();
        Bundle[] bundles = bit.getBundles();
        for (Bundle bdl : bundles) {
            Bitstream nBit = bdl.createBitstream(stream);
            nBit.setName(bit.getName());
            nBit.setDescription(bit.getDescription());
            nBit.setSource(bit.getSource());
            nBit.setUserFormatDescription(bit.getFormatDescription());
            bdl.removeBitstream(bit);
        }
        item.update();
        context.commit();
        return bundles;
    }


    public static void main(String argv[]) {
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("%c: %m%n"));
        log.addAppender(ca);

        BitstreamsArguments args = new BitstreamsArguments();
        try {

            if (args.parseArgs(argv)) {
                Bitstreams bitActor = new Bitstreams(args);
                Printer p = args.getPrinter();
                bitActor.apply(p);



            }
        } catch (SQLException se) {
            System.err.println("ERROR: " + se.getMessage() + "\n");
            System.exit(1);
        } catch (ParseException pe) {
            System.err.println("ERROR: " + pe.getMessage() + "\n");
            args.usage();
            System.exit(1);
        }
    }
}

class BitstreamsArguments extends Arguments {

    public static String FILE = "b";
    public static String FILE_LONG = "bitstream";

    public static String GOGO = "g";
    public static String GOGO_LONG = "GO-GO-GO";


    String filename;
    InputStream stream;
    BitstreamFormat format;
    Boolean gogo;

    BitstreamsArguments() {
        super(new char[]{Arguments.DO_REPLACE});
        options.addOption(FILE, FILE_LONG, true, "file to replace given bitstream");
        options.addOption(GOGO, GOGO_LONG, false, "ignore file format incompatibilities");
    }

    @Override
    public Boolean parseArgs(String[] argv) throws ParseException, SQLException {
        if (super.parseArgs(argv)) {
            gogo = line.hasOption(GOGO);

            if (!line.hasOption(FILE)) {
                throw new ParseException("must give a filename");
            }

            if (! line.hasOption(Arguments.EPERSON)) {
                throw new ParseException("Must give EPerson");
            }

            // make sure root is a BITSTREAM  - should adjust the help message
            if (getRoot().getType() != Constants.BITSTREAM) {
                throw new ParseException(getRoot() + " is not a bitstream");
            }

            // TODO - really shouldn't require type option - we know we need BITSTREAM
            if (getType() != Constants.BITSTREAM) {
                throw new ParseException("Sorry only BITSTREAM allowed for tyep argument");
            }

            filename = line.getOptionValue(FILE);
            // see whether we can open file
            try {
                stream = new BufferedInputStream(new FileInputStream(filename));
            } catch (FileNotFoundException e) {
                throw new ParseException("Can't open file " + filename);
            }
            format = FormatIdentifier.guessFormat(getContext(), filename);
            if (format == null) {
                throw new ParseException("Can't guess file format for " + filename);
            }

            return true;
        }
        return false;
    }

}