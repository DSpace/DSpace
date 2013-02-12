package org.dspace.versioning;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

import java.sql.SQLException;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Jun 16, 2011
 * Time: 1:26:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class VersioningConsumer implements Consumer {
    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(VersioningConsumer.class);

    // collect Items, Collections, Communities that need indexing
    private Set<DSpaceObject> objectsToUpdate = null;

    // handles to delete since IDs are not useful by now.
    private Set<String> handlesToDelete = null;

    public void initialize() throws Exception {
        // No-op

    }

    public void finish(Context ctx) throws Exception {
        // No-op
    }

    public void end(Context ctx) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    //public void update(Context ctx, Event event) throws Exception{

    public void consume(Context ctx, Event event) throws Exception {
        int st = event.getSubjectType();
        int et = event.getEventType();

        try {
            ctx = new Context();
            ctx.turnOffAuthorisationSystem();

            switch (st) {

                case Constants.ITEM: {
                    Item item = (Item) event.getSubject(ctx);
                    VersionHistory history = retrieveVersionHistory(ctx, item);

                    if(history!=null && history.size()>0){
                        if (et == Event.INSTALL) {
                            moveCanonicalToPointToTheNewVersion(ctx, item);
                            setArhcivedFalsePreviousVersion(ctx, item, history);
                        }
                        break;
                    }
                }

                case Constants.BUNDLE: {
                    if (et == Event.ADD || et == Event.REMOVE) {
                        Bundle b = (Bundle) event.getSubject(ctx);
                        Item item = (Item) b.getParentObject();
                        if(item.isArchived()){
                            upgradeDOIDataFile(ctx, item);
                        }
                    }
                }
                break;
            }
        }
        catch (Exception e) {
            ctx.abort();
        }
        finally {
            ctx.complete();
        }

    }

    private void upgradeDOIDataFile(Context ctx, Item item) throws AuthorizeException, SQLException, IdentifierException
    {
        IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
        identifierService.register(ctx, item);
    }

    private void setArhcivedFalsePreviousVersion(Context ctx, Item item, VersionHistory history) throws AuthorizeException, SQLException
    {
        setArchivedFalse(ctx, item, history);
        Item[] items = org.dspace.workflow.DryadWorkflowUtils.getDataFiles(ctx, item);
        for (Item datafile : items) {
            setArchivedFalse(ctx, datafile, history);
        }
    }

    private void moveCanonicalToPointToTheNewVersion(Context ctx, Item item) throws IdentifierException
    {
        DOIIdentifierProvider dis = new DSpace().getSingletonService(DOIIdentifierProvider.class);
        dis.moveCanonical(ctx, item);
    }


    private static org.dspace.versioning.VersionHistory retrieveVersionHistory(Context c, Item item) {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        org.dspace.versioning.VersionHistory history = versioningService.findVersionHistory(c, item.getID());

        return history;
    }

    private void setArchivedFalse(Context ctx, Item item, VersionHistory history) throws AuthorizeException, SQLException {

        if (history != null) {
            Version latest = history.getLatestVersion();
            Version previous = history.getPrevious(latest);
            previous.getItem().setArchived(false);
            previous.getItem().update();
        }
    }


}
