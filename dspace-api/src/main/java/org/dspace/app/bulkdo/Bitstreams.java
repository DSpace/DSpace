package org.dspace.app.bulkdo;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.Constants;

import java.io.*;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by monikam on 4/25/14.
 */
public class Bitstreams {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(Bitstreams.class);

    BitstreamsArguments args;

    Bitstreams(BitstreamsArguments arguments) {
        args = arguments;
        log.debug(this);
    }

    public String toString() {
        String me = args.filename + "(" + args.format.getMIMEType() + ") --";
        if (args.gogo) {
            me += "ignoreFormat--";
        }
        return me + "> " + args.getRoot();
    }

    int apply(Printer p, boolean dryRun) {
        p.addKey("BUNDLE.name");
        p.addKey("replace");
        p.addKey("replace.mimeType");
        p.addKey("success");
        p.addKey("bundles");

        BitstreamActionTarget target = new BitstreamActionTarget(args.getContext(), null, (Bitstream) args.getRoot());
        int retCode = 0;
        try {
            Bundle[] bdls = replaceBitstream(dryRun);
            target.put("bundles", bdls);
        } catch (Exception e) {
            target.put("exception", e.getMessage().replaceAll(" ", "_"));
            p.addKey("exception");
            if (args.getVerbose()) {
                e.printStackTrace();
            }
            retCode = 1;
        }
        target.put("replace", args.filename);
        target.put("replace.mimeType", args.format.getMIMEType());
        target.put("success", (retCode == 0) ? " SUCCESS" : "ERROR");
        p.println(target);
        return retCode;
    }

    private Bundle[] replaceBitstream(boolean dryRun) throws SQLException, IOException, AuthorizeException {
        Bitstream bit = (Bitstream) args.getRoot();
        if (!bit.getFormat().getMIMEType().equals(args.format.getMIMEType()) && !args.gogo) {
            throw new RuntimeException("format mistmatch");
        }
        Item item = (Item) bit.getParentObject();
        if (item == null) {
            throw new RuntimeException(("refusing to replace file in orphaned bitstream " + bit));
        }
        Bundle[] bundles = bit.getBundles();
        for (Bundle bdl : bundles) {
            Bitstream nBit = bdl.createBitstream(args.stream);
            nBit.setName(bit.getName());
            nBit.setDescription(bit.getDescription());
            nBit.setSource(bit.getSource());
            nBit.setFormat(args.format);
            List<ResourcePolicy> pols = AuthorizeManager.getPolicies(args.getContext(), bit);
            AuthorizeManager.removeAllPolicies(args.getContext(), nBit);
            AuthorizeManager.addPolicies(args.getContext(), pols, nBit);
            bdl.removeBitstream(bit);
        }
        item.update();
        if (!dryRun) {
            args.getContext().commit();
        }
        return bundles;
    }


    public static void main(String argv[]) {
        ConsoleAppender ca = new ConsoleAppender();
        ca.setWriter(new OutputStreamWriter(System.out));
        ca.setLayout(new PatternLayout("# %c: %m%n"));
        log.addAppender(ca);

        BitstreamsArguments args = new BitstreamsArguments();
        int retCode = 0;
        try {
            if (args.parseArgs(argv)) {
                Bitstreams bitActor = new Bitstreams(args);
                Printer p = args.getPrinter();
                retCode = bitActor.apply(p, args.getDryRun());
            }
        } catch (ParseException pe) {
            System.err.println("ERROR: " + pe.getMessage() + "\n");
            args.usage();
            retCode = 0;
        }
        System.exit(retCode);
    }
}

class BitstreamsArguments extends Arguments {
    String filename;
    InputStream stream;
    BitstreamFormat format;
    Boolean gogo;

    BitstreamsArguments() {
        super(Bitstreams.class.getCanonicalName(),  new char[]{Arguments.DO_REPLACE});
        options.addOption(Arguments.BITSTREAM_FILE, Arguments.BITSTREAM_FILE_LONG, true, "file to replace given bitstream");
        options.addOption(Arguments.GOGO, Arguments.GOGO_LONG, false, "ignore file format incompatibilities");
    }

    @Override
    public Boolean parseArgs(String[] argv) throws ParseException {
        try {
            if (super.parseArgs(argv)) {
                gogo = line.hasOption(GOGO);

                if (!line.hasOption(Arguments.BITSTREAM_FILE)) {
                    throw new ParseException("missing " + Arguments.BITSTREAM_FILE_LONG + " option");
                }

                if (!line.hasOption(Arguments.EPERSON)) {
                    throw new ParseException("missing " + Arguments.EPERSON_LONG + " option");
                }

                // make action is replace
                if (getAction() != Arguments.DO_REPLACE) {
                    throw new ParseException("Can only " + Arguments.actionText[Arguments.DO_REPLACE] + " bitstreams");
                }

                // make sure root is a BITSTREAM
                if (getRoot().getType() != Constants.BITSTREAM) {
                    throw new ParseException(getRoot() + " is not a bitstream");
                }

                // TODO - really shouldn't allow type option - we know we need BITSTREAM
                if (getType() != Constants.BITSTREAM) {
                    throw new ParseException("Sorry only working on single BITSTREAMs");
                }

                filename = line.getOptionValue(Arguments.BITSTREAM_FILE);
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
            } return false;
        } catch (SQLException ex) {
            throw new ParseException("Configuration error: " + ex.getMessage());
        }
    }

    public void printArgs(PrintStream out, String prefix) {
        super.printArgs(out, prefix);
        out.println(prefix + " " + Arguments.BITSTREAM_FILE_LONG + "=" + filename);
    }
}