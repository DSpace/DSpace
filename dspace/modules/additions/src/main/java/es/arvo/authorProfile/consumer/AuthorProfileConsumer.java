
package es.arvo.authorProfile.consumer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.content.AuthorProfile;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IndexEventConsumer;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 * Clase consumidora de eventos que rellena campos obligatorios de la pagina de autor cuando hay algun cambio en el author profile
 *  
 * @author Adán Román Ruiz at arvo.es
 */
public class AuthorProfileConsumer implements Consumer{

    private static Logger log = Logger.getLogger(AuthorProfileConsumer.class);
    private static SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMddHHmmssSSSS");
    private static String ENCODING="UTF8";

    // collect Items, Collections, Communities that need indexing
    private Set<DSpaceObject> objectsToUpdate = null;

    // handles to delete since IDs are not useful by now.
    private Set<Integer> idsToDelete = null;

    @Override
    public void initialize() throws Exception {

    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
	if (objectsToUpdate == null) {
	    objectsToUpdate = new HashSet<DSpaceObject>();
	    idsToDelete = new HashSet<Integer>();
	    }

	    int st = event.getSubjectType();
	    if (!(st == Constants.AUTHOR_PROFILE)) {
		log.warn("IndexConsumer should not have been given this kind of Subject in an event, skipping: "
			+ event.toString());
		return;
	    }

	    DSpaceObject subject = event.getSubject(ctx);

	    DSpaceObject object = event.getObject(ctx);


	    // If event subject is a Bundle and event was Add or Remove,
	    // transform the event to be a Modify on the owning Item.
	    // It could be a new bitstream in the TEXT bundle which
	    // would change the index.
	    int et = event.getEventType();
	    switch (et) {
	    case Event.CREATE:
	    case Event.MODIFY:
	    case Event.MODIFY_METADATA:
		if (subject == null)
		{
		    log.warn(event.getEventTypeAsString() + " event, could not get object for "
			    + event.getSubjectTypeAsString() + " id="
			    + String.valueOf(event.getSubjectID())
			    + ", perhaps it has been deleted.");
		}
		else {
		    log.debug("consume() adding event to update queue: " + event.toString());
		    objectsToUpdate.add(subject);
		}
		break;

	    case Event.REMOVE:
	    case Event.ADD:
		if (object == null)
		{
		    log.warn(event.getEventTypeAsString() + " event, could not get object for "
			    + event.getObjectTypeAsString() + " id="
			    + String.valueOf(event.getObjectID())
			    + ", perhaps it has been deleted.");
		}
		else {
		    log.debug("consume() adding event to update queue: " + event.toString());
		    objectsToUpdate.add(object);
		}
		break;

	    case Event.DELETE:
		String detail = event.getDetail();
		if (detail == null)
		{
		    log.warn("got null detail on DELETE event, skipping it.");
		}
		else {
		    log.debug("consume() adding event to delete queue: " + event.toString());
		    idsToDelete.add(event.getSubjectID());
		}
		break;
	    default:
		log
		.warn("IndexConsumer should not have been given a event of type="
			+ event.getEventTypeAsString()
			+ " on subject="
			+ event.getSubjectTypeAsString());
		break;
	    }

	}

    @Override
    public void end(Context ctx) throws Exception {
	if (objectsToUpdate != null && idsToDelete != null) {

	    // update the changed Items not deleted because they were on create list
	    for (DSpaceObject iu : objectsToUpdate) {
		/* we let all types through here and 
		 * allow the search DSIndexer to make 
		 * decisions on indexing and/or removal
		 */
		Integer hdl = iu.getID();
		if (hdl != null && !idsToDelete.contains(hdl)) {
		    try {
			// Seguro que es un item
			completeAuthorProfile(ctx, (AuthorProfile)iu, true);
			iu.update();
			ctx.commit();
			log.debug("Indexed "
				+ Constants.typeText[iu.getType()]
					+ ", id=" + String.valueOf(iu.getID())
					+ ", handle=" + hdl);
		    }
		    catch (Exception e) {
			log.error("Failed while create sipi file: ", e);
		    }
		}
	    }

//	    for (String hdl : handlesToDelete) {
//		try {
//		    createSIPIDeleteFile(ctx, hdl, true);
//		    if (log.isDebugEnabled())
//		    {
//			log.debug("UN-Indexed Item, handle=" + hdl);
//		    }
//		}
//		catch (Exception e) {
//		    log.error("Failed while UN-indexing object: " + hdl, e);
//		}
//	    }
	}
	// "free" the resources
	objectsToUpdate = null;
	idsToDelete = null;

    }

    private void completeAuthorProfile(Context ctx, AuthorProfile ap, boolean b) {
	String firstName=ap.getMetadata("authorProfile.name.first");
	String lastName=ap.getMetadata("authorProfile.name.last");
	String nombreCompleto=lastName+", "+firstName;
	ap.clearMetadata("authorProfile", "author", null, Item.ANY);
	ap.addMetadata("authorProfile", "author", null, Item.ANY, nombreCompleto);
	ap.clearMetadata("authorProfile", "name", "variant", Item.ANY);
	ap.addMetadata("authorProfile", "name", "variant", Item.ANY, nombreCompleto);
    }

    @Override
    public void finish(Context ctx) throws Exception {
	// TODO Auto-generated method stub

    }

}
