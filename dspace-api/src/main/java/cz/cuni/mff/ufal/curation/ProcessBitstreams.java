/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.core.Context;
import org.dspace.content.Bitstream;
import org.dspace.core.Constants;
import org.dspace.storage.bitstore.BitstreamStorageManager;


public class ProcessBitstreams extends AbstractCurationTask implements Consumer {

    // The log4j logger for this class
    private static Logger log = Logger.getLogger(ProcessBitstreams.class);

    public static String schema = "local";
    public static String element = "bitstream";

    private int status = Curator.CURATE_UNSET;

    // curator
    //

	@Override
	public int perform(DSpaceObject dso) throws IOException {

        // Unless this is  an item, we'll skip this item
        status = Curator.CURATE_SKIP;
        StringBuilder results = new StringBuilder();

        if (dso instanceof Item) {
            try {
                Item item = (Item)dso;
                processItem(item);
            } catch (Exception ex) {
                status = Curator.CURATE_FAIL;
                results.append(ex.getLocalizedMessage()).append("\n");
            }
        }
        
        report(results.toString());
        setResult(results.toString());
		return status;
	}

	void processItem(Item item) throws SQLException, AuthorizeException {
        for ( Bundle bundle : item.getBundles() ) {
            for ( Bitstream b : bundle.getBitstreams() ) {
                processBitstream(b);
            }
        }
	}

    // event consumer
    //
    public void initialize() throws Exception {
    }

    public void end(Context ctx) throws Exception {
    }

    public void finish(Context ctx) throws Exception {
    }

    public void consume(Context ctx, Event event) throws Exception {
        if (Constants.BITSTREAM != event.getSubjectType()) {
            return;
        }

        DSpaceObject subject = event.getSubject(ctx);
        DSpaceObject object = event.getObject(ctx);
        int et = event.getEventType();
        Bitstream b = (Bitstream)subject;

        if (null != subject) {
            if (Event.ADD == et || Event.CREATE == et) {
                processBitstream(b);
            } else if (Event.DELETE == et || Event.REMOVE == et) {
                // automatically removed
            }
        }

    }


    // do the processing
    //

    static void processBitstream(Bitstream b) throws SQLException, AuthorizeException {
        addBitstreamContent(b);
    }

    static void addBitstreamContent(Bitstream b) throws SQLException, AuthorizeException {
        b.clearMetadata(schema, element, "file", Item.ANY);

        //
        if ("application/zip".equals(b.getFormat().getMIMEType())) {
            ZipInputStream zip;
            try {
                zip = new ZipInputStream(b.retrieve());
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String content = String.format(
                        "%s|%d", entry.getName(), entry.getSize()
                    );
                    b.addMetadata( schema, element, "file", Item.ANY, content );
                }
            } catch (Exception e) {
                log.error(e);
            }
        }

        b.update();
    }

}
