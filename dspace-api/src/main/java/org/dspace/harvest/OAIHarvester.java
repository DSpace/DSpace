/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Stack;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.PluginManager;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.DOMBuilder;
import org.jdom.output.XMLOutputter;
import org.xml.sax.SAXException;

import ORG.oclc.oai.harvester2.verb.GetRecord;
import ORG.oclc.oai.harvester2.verb.Identify;
import ORG.oclc.oai.harvester2.verb.ListMetadataFormats;
import ORG.oclc.oai.harvester2.verb.ListRecords;
import ORG.oclc.oai.harvester2.verb.ListSets;


/**
 * This class handles OAI harvesting of externally located records into this repository.
 *
 * @author Alexey Maslov
 */


public class OAIHarvester {

	/* The main harvesting thread */
	private static HarvestScheduler harvester;
	private static Thread mainHarvestThread;

	/** log4j category */
    private static Logger log = Logger.getLogger(OAIHarvester.class);

    private static final Namespace ATOM_NS = Namespace.getNamespace("http://www.w3.org/2005/Atom");
    private static final Namespace ORE_NS = Namespace.getNamespace("http://www.openarchives.org/ore/terms/");
    private static final Namespace OAI_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");

    public static final String OAI_ADDRESS_ERROR = "invalidAddress";
    public static final String OAI_SET_ERROR = "noSuchSet";
    public static final String OAI_DMD_ERROR = "metadataNotSupported";
    public static final String OAI_ORE_ERROR = "oreNotSupported";


    //  The collection this harvester instance is dealing with
	Collection targetCollection;
	HarvestedCollection harvestRow;

	// our context
	Context ourContext;

    // Namespace used by the ORE serialization format
    // Set in dspace.cfg as harvester.oai.oreSerializationFormat.{ORESerialKey} = {ORESerialNS}
    private Namespace ORESerialNS;
    private String ORESerialKey;

    // Namespace of the descriptive metadata that should be harvested in addition to the ORE
    // Set in dspace.cfg as harvester.oai.metadataformats.{MetadataKey} = {MetadataNS},{Display Name}
    private Namespace metadataNS;
    private String metadataKey;

    // DOMbuilder class for the DOM -> JDOM conversions
    private static DOMBuilder db = new DOMBuilder();

    // The point at which this thread should terminate itself

    /* Initialize the harvester with a collection object */
	public OAIHarvester(Context c, DSpaceObject dso, HarvestedCollection hc) throws HarvestingException, SQLException
	{
		if (dso.getType() != Constants.COLLECTION)
        {
            throw new HarvestingException("OAIHarvester can only harvest collections");
        }

		ourContext = c;
		targetCollection = (Collection)dso;

		harvestRow = hc;
		if (harvestRow == null || !harvestRow.isHarvestable())
        {
            throw new HarvestingException("Provided collection is not set up for harvesting");
        }

        // Set the ORE options
		Namespace ORESerializationNamespace = OAIHarvester.getORENamespace();

        //No need to worry about ORESerializationNamespace, this can never be null
        ORESerialNS = Namespace.getNamespace(ORESerializationNamespace.getURI());
        ORESerialKey = ORESerializationNamespace.getPrefix();

        // Set the metadata options
        metadataKey = harvestRow.getHarvestMetadataConfig();
        metadataNS = OAIHarvester.getDMDNamespace(metadataKey);

        if (metadataNS == null) {
        	log.error("No matching metadata namespace found for \"" + metadataKey + "\", see oai.cfg option \"harvester.oai.metadataformats.{MetadataKey} = {MetadataNS},{Display Name}\"");
        	throw new HarvestingException("Metadata declaration not found");
        }
	}


	/**
	 * Search the configuration options and find the ORE serialization string
	 * @return Namespace of the supported ORE format. Returns null if not found.
	 */
	private static Namespace getORENamespace() {
		String ORESerializationString = null;
		String ORESeialKey = null;
		String oreString = "harvester.oai.oreSerializationFormat.";

        Enumeration pe = ConfigurationManager.propertyNames("oai");

        while (pe.hasMoreElements())
        {
            String key = (String)pe.nextElement();
            if (key.startsWith(oreString)) {
            	ORESeialKey = key.substring(oreString.length());
            	ORESerializationString = ConfigurationManager.getProperty("oai", key);

                return Namespace.getNamespace(ORESeialKey, ORESerializationString);
            }
        }

        // Fallback if the configuration option is not present
        return Namespace.getNamespace("ore", ATOM_NS.getURI());
	}


	/**
	 * Cycle through the options and find the metadata namespace matching the provided key.
	 * @param metadataKey
	 * @return Namespace of the designated metadata format. Returns null of not found.
	 */
	private static Namespace getDMDNamespace(String metadataKey) {
		String metadataString = null;
        String metaString = "harvester.oai.metadataformats.";

        Enumeration pe = ConfigurationManager.propertyNames("oai");

        while (pe.hasMoreElements())
        {
            String key = (String)pe.nextElement();

            if (key.startsWith(metaString) && key.substring(metaString.length()).equals((metadataKey))) {
            	metadataString = ConfigurationManager.getProperty("oai", key);
            	String namespacePiece;
            	if (metadataString.indexOf(',') != -1)
                {
                    namespacePiece = metadataString.substring(0, metadataString.indexOf(','));
                }
            	else
                {
                    namespacePiece = metadataString;
                }

            	return Namespace.getNamespace(namespacePiece);
            }
        }
        return null;
	}





	/**
     * Performs a harvest cycle on this collection. This will query the remote OAI-PMH provider, check for updates since last
     * harvest, and ingest the returned items.
     */
	public void runHarvest() throws SQLException, IOException, AuthorizeException
	{
		// figure out the relevant parameters
		String oaiSource = harvestRow.getOaiSource();
		String oaiSetId = harvestRow.getOaiSetId();
        //If we have all selected then make sure that we do not include a set filter
        if("all".equals(oaiSetId))
        {
            oaiSetId = null;
        }

		Date lastHarvestDate = harvestRow.getHarvestDate();
		String fromDate = null;
		if (lastHarvestDate != null)
        {
            fromDate = processDate(harvestRow.getHarvestDate());
        }

		Date startTime = new Date();
		String toDate = processDate(startTime,0);

		String dateGranularity;

		try
		{
			// obtain the desired descriptive metadata format and verify that the OAI server actually provides it
			// do the same thing for ORE, which should be encoded in Atom and carry its namespace
			String descMDPrefix = null;
			String OREPrefix;
	    	try {
	    		dateGranularity = oaiGetDateGranularity(oaiSource);
	    		if (fromDate != null)
                {
                    fromDate = fromDate.substring(0, dateGranularity.length());
                }
	    		toDate = toDate.substring(0, dateGranularity.length());

	    		descMDPrefix = oaiResolveNamespaceToPrefix(oaiSource, metadataNS.getURI());
	    		OREPrefix = oaiResolveNamespaceToPrefix(oaiSource, ORESerialNS.getURI());
	    	}
	    	catch (FileNotFoundException fe) {
	    		log.error("The OAI server did not respond.");
	    		throw new HarvestingException("The OAI server did not respond.", fe);
	    	}
	    	catch (ConnectException fe) {
	    		log.error("The OAI server did not respond.");
	    		throw new HarvestingException("The OAI server did not respond.", fe);
	    	}
			if (descMDPrefix == null) {
				log.error("The OAI server does not support this metadata format");
				throw new HarvestingException("The OAI server does not support this metadata format: " + metadataNS.getURI());
			}
			if (OREPrefix == null && harvestRow.getHarvestType() != HarvestedCollection.TYPE_DMD) {
				throw new HarvestingException("The OAI server does not support ORE dissemination in the configured serialization format: " + ORESerialNS.getURI());
			}

			Document oaiResponse = null;
			Element root = null;
			String resumptionToken;

			// set the status indicating the collection is currently being processed
			harvestRow.setHarvestStatus(HarvestedCollection.STATUS_BUSY);
			harvestRow.setHarvestMessage("Collection is currently being harvested");
			harvestRow.setHarvestStartTime(startTime);
			harvestRow.update();
			ourContext.commit();

			// expiration timer starts
			int expirationInterval = ConfigurationManager.getIntProperty("oai", "harvester.threadTimeout");
	    	if (expirationInterval == 0)
            {
                expirationInterval = 24;
            }

			Calendar calendar = Calendar.getInstance();
			calendar.setTime(startTime);
			calendar.add(Calendar.HOUR, expirationInterval);
            Date expirationTime = calendar.getTime();

			// main loop to keep requesting more objects until we're done
			List<Element> records;
			Set<String> errorSet = new HashSet<String>();

			ListRecords listRecords = new ListRecords(oaiSource, fromDate, toDate, oaiSetId, descMDPrefix);
			log.debug("Harvesting request parameters: listRecords " + oaiSource + " " + fromDate + " " + toDate + " " + oaiSetId + " " + descMDPrefix);
			if (listRecords != null)
            {
                log.info("HTTP Request: " + listRecords.getRequestURL());
            }
			while (listRecords != null)
			{
				records = new ArrayList<Element>();
				oaiResponse = db.build(listRecords.getDocument());

				if (listRecords.getErrors() != null && listRecords.getErrors().getLength() > 0)
				{
					for (int i=0; i<listRecords.getErrors().getLength(); i++)
					{
						String errorCode = listRecords.getErrors().item(i).getAttributes().getNamedItem("code").getTextContent();
						errorSet.add(errorCode);
					}
					if (errorSet.contains("noRecordsMatch"))
					{
						log.info("noRecordsMatch: OAI server did not contain any updates");
						harvestRow.setHarvestResult(new Date(), "OAI server did not contain any updates");
						harvestRow.setHarvestStatus(HarvestedCollection.STATUS_READY);
						harvestRow.update();
						return;
					} else {
						throw new HarvestingException(errorSet.toString());
					}
				}
				else
				{
					root = oaiResponse.getRootElement();
					records.addAll(root.getChild("ListRecords", OAI_NS).getChildren("record", OAI_NS));
				}

				// Process the obtained records
				if (records != null && records.size()>0)
				{
					log.info("Found " + records.size() + " records to process");
					for (Element record : records) {
						// check for STOP interrupt from the scheduler
						if (HarvestScheduler.interrupt == HarvestScheduler.HARVESTER_INTERRUPT_STOP)
                        {
                            throw new HarvestingException("Harvest process for " + targetCollection.getID() + " interrupted by stopping the scheduler.");
                        }
						// check for timeout
						if (expirationTime.before(new Date()))
                        {
                            throw new HarvestingException("runHarvest method timed out for collection " + targetCollection.getID());
                        }

						processRecord(record,OREPrefix);
						ourContext.commit();
					}
				}

				// keep going if there are more records to process
				resumptionToken = listRecords.getResumptionToken();
				if (resumptionToken == null || resumptionToken.length() == 0) {
					listRecords = null;
				}
				else {
					listRecords = new ListRecords(oaiSource, resumptionToken);
				}
				targetCollection.update();
				ourContext.commit();
			}
		}
		catch (HarvestingException hex) {
			log.error("Harvesting error occured while processing an OAI record: " + hex.getMessage());
			harvestRow.setHarvestMessage("Error occured while processing an OAI record");

			// if the last status is also an error, alert the admin
			if (harvestRow.getHarvestMessage().contains("Error")) {
				alertAdmin(HarvestedCollection.STATUS_OAI_ERROR, hex);
			}
			harvestRow.setHarvestStatus(HarvestedCollection.STATUS_OAI_ERROR);
			return;
		}
		catch (Exception ex) {
			harvestRow.setHarvestMessage("Unknown error occured while generating an OAI response");
			harvestRow.setHarvestStatus(HarvestedCollection.STATUS_UNKNOWN_ERROR);
			alertAdmin(HarvestedCollection.STATUS_UNKNOWN_ERROR, ex);
			log.error("Error occured while generating an OAI response: " + ex.getMessage() + " " + ex.getCause());
			ex.printStackTrace();
			return;
		}
		finally {
			harvestRow.update();
			targetCollection.update();
			ourContext.commit();
			ourContext.restoreAuthSystemState();
		}

		// If we got to this point, it means the harvest was completely successful
		Date finishTime = new Date();
		long timeTaken = finishTime.getTime() - startTime.getTime();
		harvestRow.setHarvestResult(startTime, "Harvest from " + oaiSource + " successful");
		harvestRow.setHarvestStatus(HarvestedCollection.STATUS_READY);
		log.info("Harvest from " + oaiSource + " successful. The process took " + timeTaken + " milliseconds.");
		harvestRow.update();
		ourContext.commit();
	}

    /**
     * Process an individual PMH record, making (or updating) a corresponding DSpace Item.
     * @param record a JDOM Element containing the actual PMH record with descriptive metadata.
     * @param OREPrefix the metadataprefix value used by the remote PMH server to disseminate ORE. Only used for collections set up to harvest content.
     */
    private void processRecord(Element record, String OREPrefix) throws SQLException, AuthorizeException, IOException, CrosswalkException, HarvestingException, ParserConfigurationException, SAXException, TransformerException
    {
    	WorkspaceItem wi = null;
    	Date timeStart = new Date();

    	// grab the oai identifier
    	String itemOaiID = record.getChild("header", OAI_NS).getChild("identifier", OAI_NS).getText();
    	Element header = record.getChild("header",OAI_NS);

    	// look up the item corresponding to the OAI identifier
    	Item item = HarvestedItem.getItemByOAIId(ourContext, itemOaiID, targetCollection.getID());

    	// Make sure the item hasn't been deleted in the mean time
		if (header.getAttribute("status") != null && header.getAttribute("status").getValue().equals("deleted")) {
			log.info("Item " + itemOaiID + " has been marked as deleted on the OAI server.");
			if (item != null)
            {
                targetCollection.removeItem(item);
            }

			ourContext.restoreAuthSystemState();
			return;
		}

		// If we are only harvesting descriptive metadata, the record should already contain all we need
    	List<Element> descMD = record.getChild("metadata", OAI_NS).getChildren();
    	IngestionCrosswalk MDxwalk = (IngestionCrosswalk)PluginManager.getNamedPlugin(IngestionCrosswalk.class, this.metadataKey);

    	// Otherwise, obtain the ORE ReM and initiate the ORE crosswalk
    	IngestionCrosswalk ORExwalk = null;
    	Element oreREM = null;
    	if (harvestRow.getHarvestType() > 1) {
    		oreREM = getMDrecord(harvestRow.getOaiSource(), itemOaiID, OREPrefix).get(0);
    		ORExwalk = (IngestionCrosswalk)PluginManager.getNamedPlugin(IngestionCrosswalk.class, this.ORESerialKey);
    	}

    	// Ignore authorization
    	ourContext.turnOffAuthorisationSystem();

    	HarvestedItem hi;

    	if (item != null) // found an item so we modify
    	{
    		log.debug("Item " + item.getHandle() + " was found locally. Using it to harvest " + itemOaiID + ".");

    		// FIXME: check for null pointer if for some odd reason we don't have a matching hi
    		hi = HarvestedItem.find(ourContext, item.getID());

    		// Compare last-harvest on the item versus the last time the item was updated on the OAI provider side
			// If ours is more recent, forgo this item, since it's probably a left-over from a previous harvesting attempt
			Date OAIDatestamp = Utils.parseISO8601Date(header.getChildText("datestamp", OAI_NS));
			Date itemLastHarvest = hi.getHarvestDate();
			if (itemLastHarvest != null && OAIDatestamp.before(itemLastHarvest)) {
				log.info("Item " + item.getHandle() + " was harvested more recently than the last update time reporetd by the OAI server; skipping.");
				return;
			}

			// Otherwise, clear and re-import the metadata and bitstreams
    		item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
    		if (descMD.size() == 1)
            {
                MDxwalk.ingest(ourContext, item, descMD.get(0));
            }
    		else
            {
                MDxwalk.ingest(ourContext, item, descMD);
            }

    		// Import the actual bitstreams
    		if (harvestRow.getHarvestType() == 3) {
    			log.info("Running ORE ingest on: " + item.getHandle());

    			Bundle[] allBundles = item.getBundles();
    			for (Bundle bundle : allBundles) {
    				item.removeBundle(bundle);
    			}
    			ORExwalk.ingest(ourContext, item, oreREM);
    		}

    		scrubMetadata(item);
    	}
    	else
    		// NOTE: did not find, so we create (presumably, there will never be a case where an item already
    		// exists in a harvest collection but does not have an OAI_id)
    	{
    		wi = WorkspaceItem.create(ourContext, targetCollection, false);
    		item = wi.getItem();

    		hi = HarvestedItem.create(ourContext, item.getID(), itemOaiID);
    		//item.setOaiID(itemOaiID);

    		if (descMD.size() == 1)
            {
                MDxwalk.ingest(ourContext, item, descMD.get(0));
            }
    		else
            {
                MDxwalk.ingest(ourContext, item, descMD);
            }

    		if (harvestRow.getHarvestType() == 3) {
    			ORExwalk.ingest(ourContext, item, oreREM);
    		}

    		// see if we can do something about the wonky metadata
    		scrubMetadata(item);

    		// see if a handle can be extracted for the item
    		String handle = extractHandle(item);

    		if (handle != null)
    		{
    			DSpaceObject dso = HandleManager.resolveToObject(ourContext, handle);
    			if (dso != null)
                {
                    throw new HarvestingException("Handle collision: attempted to re-assign handle '" + handle + "' to an incoming harvested item '" + hi.getOaiID() + "'.");
                }
    		}

    		try {
    			item = InstallItem.installItem(ourContext, wi, handle);
    			//item = InstallItem.installItem(ourContext, wi);
    		}
    		// clean up the workspace item if something goes wrong before
    		catch(SQLException se) {
    			wi.deleteWrapper();
    			throw se;
    		}
    		catch(IOException ioe) {
    			wi.deleteWrapper();
    			throw ioe;
    		}
    		catch(AuthorizeException ae) {
    			wi.deleteWrapper();
    			throw ae;
    		}
    	}

    	// Now create the special ORE bundle and drop the ORE document in it
		if (harvestRow.getHarvestType() == 2 || harvestRow.getHarvestType() == 3)
		{
			Bundle OREBundle = item.createBundle("ORE");

			XMLOutputter outputter = new XMLOutputter();
			String OREString = outputter.outputString(oreREM);
			ByteArrayInputStream OREStream = new ByteArrayInputStream(OREString.getBytes());

			Bitstream OREBitstream = OREBundle.createBitstream(OREStream);
			OREBitstream.setName("ORE.xml");

			BitstreamFormat bf = FormatIdentifier.guessFormat(ourContext, OREBitstream);
			OREBitstream.setFormat(bf);
			OREBitstream.update();

			OREBundle.addBitstream(OREBitstream);
			OREBundle.update();
		}

		//item.setHarvestDate(new Date());
		hi.setHarvestDate(new Date());

                 // Add provenance that this item was harvested via OAI
                String provenanceMsg = "Item created via OAI harvest from source: "
                                        + this.harvestRow.getOaiSource() + " on " +  new DCDate(hi.getHarvestDate())
                                        + " (GMT).  Item's OAI Record identifier: " + hi.getOaiID();
                item.addMetadata("dc", "description", "provenance", "en", provenanceMsg);

		item.update();
		hi.update();
		long timeTaken = new Date().getTime() - timeStart.getTime();
		log.info("Item " + item.getHandle() + "(" + item.getID() + ")" + " has been ingested. The whole process took: " + timeTaken + " ms. ");

    	// Stop ignoring authorization
    	ourContext.restoreAuthSystemState();
    }



    /**
     * Scan an item's metadata, looking for the value "identifier.*". If it meets the parameters that identify it as valid handle
     * as set in dspace.cfg (harvester.acceptedHandleServer and harvester.rejectedHandlePrefix), use that handle instead of
     * minting a new one.
     * @param item a newly created, but not yet installed, DSpace Item
     * @return null or the handle to be used.
     */
    private String extractHandle(Item item)
    {
    	String acceptedHandleServersString = ConfigurationManager.getProperty("oai", "harvester.acceptedHandleServer");
    	if (acceptedHandleServersString == null)
        {
            acceptedHandleServersString = "hdl.handle.net";
        }

    	String rejectedHandlePrefixString = ConfigurationManager.getProperty("oai", "harvester.rejectedHandlePrefix");
    	if (rejectedHandlePrefixString == null)
        {
            rejectedHandlePrefixString = "123456789";
        }

    	DCValue[] values = item.getMetadata("dc", "identifier", Item.ANY, Item.ANY);

    	if (values.length > 0 && !acceptedHandleServersString.equals(""))
    	{
    		String[] acceptedHandleServers = acceptedHandleServersString.split(",");
    		String[] rejectedHandlePrefixes = rejectedHandlePrefixString.split(",");

    		for (DCValue value : values)
    		{
    			//     0   1       2         3   4
    			//   http://hdl.handle.net/1234/12
    			String[] urlPieces = value.value.split("/");
    			if (urlPieces.length != 5)
                {
                    continue;
                }

    			for (String server : acceptedHandleServers) {
    				if (urlPieces[2].equals(server)) {
    					for (String prefix : rejectedHandlePrefixes) {
    						if (!urlPieces[3].equals(prefix))
                            {
                                return urlPieces[3] + "/" + urlPieces[4];
                            }
    					}

    				}
    			}
    		}
    	}

    	return null;
    }


    /**
     * Scans an item's newly ingested metadata for elements not defined in this DSpace instance. It then takes action based
     * on a configurable parameter (fail, ignore, add).
     * @param item a DSpace item recently pushed through an ingestion crosswalk but prior to update/installation
     */
    private void scrubMetadata(Item item) throws SQLException, HarvestingException, AuthorizeException, IOException
    {
    	// The two options, with three possibilities each: add, ignore, fail
    	String schemaChoice = ConfigurationManager.getProperty("oai", "harvester.unknownSchema");
    	if (schemaChoice == null)
        {
            schemaChoice = "fail";
        }

    	String fieldChoice = ConfigurationManager.getProperty("oai", "harvester.unknownField");
    	if (fieldChoice == null)
        {
            fieldChoice = "fail";
        }

    	List<String> clearList = new ArrayList<String>();

    	DCValue[] values = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
    	for (DCValue value : values)
    	{
    		// Verify that the schema exists
    		MetadataSchema mdSchema = MetadataSchema.find(ourContext, value.schema);
    		if (mdSchema == null && !clearList.contains(value.schema)) {
    			// add a new schema, giving it a namespace of "unknown". Possibly a very bad idea.
    			if (schemaChoice.equals("add")) {
    				mdSchema = new MetadataSchema(value.schema,String.valueOf(new Date().getTime()));
    				try {
						mdSchema.create(ourContext);
						mdSchema.setName(value.schema);
						mdSchema.setNamespace("unknown"+mdSchema.getSchemaID());
	    				mdSchema.update(ourContext);
					} catch (NonUniqueMetadataException e) {
						// This case should not be possible
						e.printStackTrace();
					}
					clearList.add(value.schema);
    			}
    			// ignore the offending schema, quietly dropping all of its metadata elements before they clog our gears
    			else if (schemaChoice.equals("ignore")) {
    				item.clearMetadata(value.schema, Item.ANY, Item.ANY, Item.ANY);
    				continue;
    			}
    			// otherwise, go ahead and generate the error
    			else {
    				throw new HarvestingException("The '" + value.schema + "' schema has not been defined in this DSpace instance. ");
    			}
    		}

            if (mdSchema != null) {
                // Verify that the element exists; this part is reachable only if the metadata schema is valid
                MetadataField mdField = MetadataField.findByElement(ourContext, mdSchema.getSchemaID(), value.element, value.qualifier);
                if (mdField == null) {
                    if (fieldChoice.equals("add")) {
                        mdField = new MetadataField(mdSchema, value.element, value.qualifier, null);
                        try {
                            mdField.create(ourContext);
                            mdField.update(ourContext);
                        } catch (NonUniqueMetadataException e) {
                            // This case should also not be possible
                            e.printStackTrace();
                        }
                    }
                    else if (fieldChoice.equals("ignore")) {
                        item.clearMetadata(value.schema, value.element, value.qualifier, Item.ANY);
                    }
                    else {
                        throw new HarvestingException("The '" + value.element + "." + value.qualifier + "' element has not been defined in this DSpace instance. ");
                    }
                }
            }
    	}

    	return;
    }




   	/**
   	 * Process a date, converting it to RFC3339 format, setting the timezone to UTC and subtracting time padding
   	 * from the config file.
   	 * @param date source Date
   	 * @return a string in the format 'yyyy-mm-ddThh:mm:ssZ' and converted to UTC timezone
   	 */
    private String processDate(Date date) {
    	Integer timePad = ConfigurationManager.getIntProperty("oai", "harvester.timePadding");

    	if (timePad == 0) {
    		timePad = 120;
		}

    	return processDate(date, timePad);
    }

    /**
   	 * Process a date, converting it to RFC3339 format, setting the timezone to UTC and subtracting time padding
   	 * from the config file.
   	 * @param date source Date
   	 * @param secondsPad number of seconds to subtract from the date
   	 * @return a string in the format 'yyyy-mm-ddThh:mm:ssZ' and converted to UTC timezone
   	 */
    private String processDate(Date date, int secondsPad) {

    	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    	formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.SECOND, -1*secondsPad);
		date = calendar.getTime();

		return formatter.format(date);
    }


    /**
     * Query OAI-PMH server for the granularity of its datestamps.
     * @throws TransformerException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    private String oaiGetDateGranularity(String oaiSource) throws IOException, ParserConfigurationException, SAXException, TransformerException
    {
    	Identify iden = new Identify(oaiSource);
    	return iden.getDocument().getElementsByTagNameNS(OAI_NS.getURI(), "granularity").item(0).getTextContent();
    }

    /**
     * Query the OAI-PMH server for its mapping of the supplied namespace and metadata prefix.
     * For example for a typical OAI-PMH server a query "http://www.openarchives.org/OAI/2.0/oai_dc/" would return "oai_dc".
     * @param oaiSource the address of the OAI-PMH provider
     * @param MDNamespace the namespace that we are trying to resolve to the metadataPrefix
     * @return metadataPrefix the OAI-PMH provider has assigned to the supplied namespace
     */
    public static String oaiResolveNamespaceToPrefix(String oaiSource, String MDNamespace) throws IOException, ParserConfigurationException, SAXException, TransformerException, ConnectException
    {
    	String metaPrefix = null;

    	// Query the OAI server for the metadata
    	ListMetadataFormats lmf = new ListMetadataFormats(oaiSource);

    	if (lmf != null) {
    		Document lmfResponse = db.build(lmf.getDocument());
    		List<Element> mdFormats = lmfResponse.getRootElement().getChild("ListMetadataFormats", OAI_NS).getChildren("metadataFormat", OAI_NS);

    		for (Element mdFormat : mdFormats) {
    			if (MDNamespace.equals(mdFormat.getChildText("metadataNamespace", OAI_NS)))
    			{
    				metaPrefix = mdFormat.getChildText("metadataPrefix", OAI_NS);
    				break;
    			}
    		}
    	}

    	return metaPrefix;
    }

    /**
     * Generate and send an email to the administrator. Prompted by errors encountered during harvesting.
     * @param status the current status of the collection, usually HarvestedCollection.STATUS_OAI_ERROR or HarvestedCollection.STATUS_UNKNOWN_ERROR
     * @param ex the Exception that prompted this action
     */
    private void alertAdmin(int status, Exception ex)
    {
    	try {
			String recipient = ConfigurationManager.getProperty("alert.recipient");

			if (StringUtils.isNotBlank(recipient)) {
				Email email = Email.getEmail(I18nUtil.getEmailFilename(Locale.getDefault(), "harvesting_error"));
				email.addRecipient(recipient);
				email.addArgument(targetCollection.getID());
				email.addArgument(new Date());
				email.addArgument(status);

				String stackTrace;

				if (ex != null) {
                    email.addArgument(ex.getMessage());

					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					ex.printStackTrace(pw);
					pw.flush();
					stackTrace = sw.toString();
				} else {
					stackTrace = "No exception";
				}

				email.addArgument(stackTrace);
				email.send();
			}
		} catch (Exception e) {
			log.warn("Unable to send email alert", e);
		}

    }



    /**
     * Query the OAI-PMH provider for a specific metadata record.
     * @param oaiSource the address of the OAI-PMH provider
     * @param itemOaiId the OAI identifier of the target item
     * @param metadataPrefix the OAI metadataPrefix of the desired metadata
     * @return list of JDOM elements corresponding to the metadata entries in the located record.
     */
    private List<Element> getMDrecord(String oaiSource, String itemOaiId, String metadataPrefix) throws IOException, ParserConfigurationException, SAXException, TransformerException, HarvestingException
    {
		GetRecord getRecord = new GetRecord(oaiSource,itemOaiId,metadataPrefix);
		Set<String> errorSet = new HashSet<String>();
		// If the metadata is not available for this item, can the whole thing
		if (getRecord != null && getRecord.getErrors() != null && getRecord.getErrors().getLength() > 0) {
			for (int i=0; i<getRecord.getErrors().getLength(); i++) {
				String errorCode = getRecord.getErrors().item(i).getAttributes().getNamedItem("code").getTextContent();
				errorSet.add(errorCode);
			}
			throw new HarvestingException("OAI server returned the following errors during getDescMD execution: " + errorSet.toString());
		}

		Document record = db.build(getRecord.getDocument());
		Element root = record.getRootElement();

		return root.getChild("GetRecord",OAI_NS).getChild("record", OAI_NS).getChild("metadata",OAI_NS).getChildren();
    }


    /**
     * Verify OAI settings for the current collection
     * @return list of errors encountered during verification. Empty list indicates a "success" condition.
     */
    public List<String> verifyOAIharvester() {
    	String oaiSource = harvestRow.getOaiSource();
    	String oaiSetId = harvestRow.getOaiSetId();
    	String metaPrefix = harvestRow.getHarvestMetadataConfig();

    	return verifyOAIharvester(oaiSource, oaiSetId, metaPrefix, true);
    }

    /**
     * Verify the existence of an OAI server with the specified set and
     * supporting the provided metadata formats.
     *
     * @param oaiSource the address of the OAI-PMH provider
     * @param oaiSetId
     * @param metaPrefix
     * @param testORE whether the method should also check the PMH provider for ORE support
     * @return list of errors encountered during verification. Empty list indicates a "success" condition.
     */
    public static List<String> verifyOAIharvester(String oaiSource,
            String oaiSetId, String metaPrefix, boolean testORE)
    {
    	List<String> errorSet = new ArrayList<String>();

        // First, see if we can contact the target server at all.
    	try {
    		Identify idenTest = new Identify(oaiSource);
    	}
    	catch (Exception ex) {
    		errorSet.add(OAI_ADDRESS_ERROR + ": OAI server could not be reached.");
    		return errorSet;
    	}

        // Next, make sure the metadata we need is supported by the target server
        Namespace DMD_NS = OAIHarvester.getDMDNamespace(metaPrefix);
        if (null == DMD_NS)
        {
            errorSet.add(OAI_DMD_ERROR + ":  " + metaPrefix);
            return errorSet;
        }

        String OREOAIPrefix = null;
        String DMDOAIPrefix = null;

        try {
            OREOAIPrefix = OAIHarvester.oaiResolveNamespaceToPrefix(oaiSource, getORENamespace().getURI());
            DMDOAIPrefix = OAIHarvester.oaiResolveNamespaceToPrefix(oaiSource, DMD_NS.getURI());
    	}
    	catch (Exception ex) {
            errorSet.add(OAI_ADDRESS_ERROR
                    + ": OAI did not respond to ListMetadataFormats query  ("
                    + ORE_NS.getPrefix() + ":" + OREOAIPrefix + " ; "
                    + DMD_NS.getPrefix() + ":" + DMDOAIPrefix + "):  "
                    + ex.getMessage());
            return errorSet;
    	}

    	if (testORE && OREOAIPrefix == null)
        {
            errorSet.add(OAI_ORE_ERROR + ": The OAI server does not support ORE dissemination");
        }
    	if (DMDOAIPrefix == null)
        {
            errorSet.add(OAI_DMD_ERROR + ": The OAI server does not support dissemination in this format");
        }

    	// Now scan the sets and make sure the one supplied is in the list
    	boolean foundSet = false;
    	try {
            //If we do not want to harvest from one set, then skip this.
    		if(!"all".equals(oaiSetId)){
                ListSets ls = new ListSets(oaiSource);

                // The only error we can really get here is "noSetHierarchy"
                if (ls.getErrors() != null && ls.getErrors().getLength() > 0) {
                    for (int i=0; i<ls.getErrors().getLength(); i++) {
                        String errorCode = ls.getErrors().item(i).getAttributes().getNamedItem("code").getTextContent();
                        errorSet.add(errorCode);
                    }
                }
                else {
                    // Drilling down to /OAI-PMH/ListSets/set
                    Document reply = db.build(ls.getDocument());
                    Element root = reply.getRootElement();
                    List<Element> sets= root.getChild("ListSets",OAI_NS).getChildren("set",OAI_NS);

                    for (Element set : sets)
                    {
                        String setSpec = set.getChildText("setSpec", OAI_NS);
                        if (setSpec.equals(oaiSetId)) {
                            foundSet = true;
                            break;
                        }
                    }

                    if (!foundSet) {
                        errorSet.add(OAI_SET_ERROR + ": The OAI server does not have a set with the specified setSpec");
                    }
                }
            }
    	}
        catch (RuntimeException re) {
            throw re;
        }
        catch (Exception e)
        {
            errorSet.add(OAI_ADDRESS_ERROR + ": OAI server could not be reached");
            return errorSet;
        }

        return errorSet;
    }

    /**
     * Start harvest scheduler.
     */
    public static synchronized void startNewScheduler() throws SQLException, AuthorizeException {
        Context c = new Context();
        HarvestedCollection.exists(c);
        c.complete();

        if (mainHarvestThread != null && harvester != null) {
                stopScheduler();
            }
    	harvester = new HarvestScheduler();
    	HarvestScheduler.interrupt = HarvestScheduler.HARVESTER_INTERRUPT_NONE;
    	mainHarvestThread = new Thread(harvester);
    	mainHarvestThread.start();
    }

    /**
     * Stop an active harvest scheduler.
     */
    public static synchronized void stopScheduler() throws SQLException, AuthorizeException {
        synchronized(HarvestScheduler.lock) {
                HarvestScheduler.interrupt = HarvestScheduler.HARVESTER_INTERRUPT_STOP;
                HarvestScheduler.lock.notify();
        }
        mainHarvestThread = null;
                harvester = null;
    }

	/**
	 * Pause an active harvest scheduler.
	 */
	public static void pauseScheduler() throws SQLException, AuthorizeException {
		synchronized(HarvestScheduler.lock) {
			HarvestScheduler.interrupt = HarvestScheduler.HARVESTER_INTERRUPT_PAUSE;
			HarvestScheduler.lock.notify();
		}
    }

	/**
	 * Resume a paused harvest scheduler.
	 */
	public static void resumeScheduler() throws SQLException, AuthorizeException {
		HarvestScheduler.interrupt = HarvestScheduler.HARVESTER_INTERRUPT_RESUME;
    }

	public static void resetScheduler() throws SQLException, AuthorizeException, IOException {
		Context context = new Context();
		List<Integer> cids = HarvestedCollection.findAll(context);
    	for (Integer cid : cids)
    	{
    		HarvestedCollection hc = HarvestedCollection.find(context, cid);
    		hc.setHarvestStartTime(null);
    		hc.setHarvestStatus(HarvestedCollection.STATUS_READY);
    		hc.update();
    	}
    	context.commit();
    }


	/**
	 * Exception class specifically assigned to recoverable errors that occur during harvesting. Throughout the harvest process, various exceptions
	 * are caught and turned into a HarvestingException. Uncaught exceptions are irrecoverable errors.
	 * @author alexey
	 */
	public static class HarvestingException extends Exception
	{
		public HarvestingException() {
	        super();
	    }

	    public HarvestingException(String message, Throwable t) {
	        super(message, t);
	    }

	    public HarvestingException(String message) {
	        super(message);
	    }

	    public HarvestingException(Throwable t) {
	        super(t);
	    }
	}

    /**
     * The class responsible for scheduling harvesting cycles are regular intervals.
     * @author alexey
     */
    public static class HarvestScheduler implements Runnable
    {
        private static EPerson harvestAdmin;

        private Context mainContext;

        public static final Object lock = new Object();

        private static Stack<HarvestThread> harvestThreads;

        private static Integer maxActiveThreads;

        protected static volatile Integer activeThreads = 0;

        public static final int HARVESTER_STATUS_RUNNING = 1;

        public static final int HARVESTER_STATUS_SLEEPING = 2;

        public static final int HARVESTER_STATUS_PAUSED = 3;

        public static final int HARVESTER_STATUS_STOPPED = 4;

        public static final int HARVESTER_INTERRUPT_NONE = 0;

        public static final int HARVESTER_INTERRUPT_PAUSE = 1;

        public static final int HARVESTER_INTERRUPT_STOP = 2;

        public static final int HARVESTER_INTERRUPT_RESUME = 3;

        public static final int HARVESTER_INTERRUPT_INSERT_THREAD = 4;

        public static final int HARVESTER_INTERRUPT_KILL_THREAD = 5;

        private static int status = HARVESTER_STATUS_STOPPED;

        private static int interrupt = HARVESTER_INTERRUPT_NONE;

        private static Integer interruptValue = 0;

        private static long minHeartbeat;

        private static long maxHeartbeat;

        public static boolean hasStatus(int statusToCheck) {
            return status == statusToCheck;
        }

        public static synchronized void setInterrupt(int newInterrupt) {
            interrupt = newInterrupt;
        }

        public static synchronized void setInterrupt(int newInterrupt, int newInterruptValue) {
            interrupt = newInterrupt;
            interruptValue = newInterruptValue;
        }

        public static String getStatus() {
            switch(status) {
            case HARVESTER_STATUS_RUNNING:
                switch(interrupt) {
                case HARVESTER_INTERRUPT_PAUSE: return("The scheduler is finishing active harvests before pausing. ");
                case HARVESTER_INTERRUPT_STOP: return("The scheduler is shutting down. ");
                }
                return("The scheduler is actively harvesting collections. ");
            case HARVESTER_STATUS_SLEEPING: return("The scheduler is waiting for collections to harvest. ");
            case HARVESTER_STATUS_PAUSED: return("The scheduler is paused. ");
            default: return("Automatic harvesting is not active. ");
            }
        }

        public HarvestScheduler() throws SQLException, AuthorizeException {
            mainContext = new Context();
            String harvestAdminParam = ConfigurationManager.getProperty("oai", "harvester.eperson");
            harvestAdmin = null;
            if (harvestAdminParam != null && harvestAdminParam.length() > 0)
            {
                harvestAdmin = EPerson.findByEmail(mainContext, harvestAdminParam);
            }

            harvestThreads = new Stack<HarvestThread>();

            maxActiveThreads = ConfigurationManager.getIntProperty("oai", "harvester.maxThreads");
            if (maxActiveThreads == 0)
            {
                maxActiveThreads = 3;
            }
            minHeartbeat = ConfigurationManager.getIntProperty("oai", "harvester.minHeartbeat") * 1000;
            if (minHeartbeat == 0)
            {
                minHeartbeat = 30000;
            }
            maxHeartbeat = ConfigurationManager.getIntProperty("oai", "harvester.maxHeartbeat") * 1000;
            if (maxHeartbeat == 0)
            {
                maxHeartbeat = 3600000;
            }
        }

        public void run() {
            scheduleLoop();
        }

        private void scheduleLoop() {
            long i=0;
            while(true)
            {
                try
                {
                    synchronized (HarvestScheduler.class) {
                        switch (interrupt)
                        {
                        case HARVESTER_INTERRUPT_NONE:
                            break;
                        case HARVESTER_INTERRUPT_INSERT_THREAD:
                            interrupt = HARVESTER_INTERRUPT_NONE;
                            addThread(interruptValue);
                            interruptValue = 0;
                            break;
                        case HARVESTER_INTERRUPT_PAUSE:
                            interrupt = HARVESTER_INTERRUPT_NONE;
                            status = HARVESTER_STATUS_PAUSED;
                        case HARVESTER_INTERRUPT_STOP:
                            interrupt = HARVESTER_INTERRUPT_NONE;
                            status = HARVESTER_STATUS_STOPPED;
                            return;
                        }
                    }

                    if (status == HARVESTER_STATUS_PAUSED) {
                        while(interrupt != HARVESTER_INTERRUPT_RESUME && interrupt != HARVESTER_INTERRUPT_STOP) {
                            Thread.sleep(1000);
                        }
                        if (interrupt != HARVESTER_INTERRUPT_STOP)
                        {
                            break;
                        }
                    }

                    status = HARVESTER_STATUS_RUNNING;

                    // Stage #1: if something is ready for harvest, push it onto the ready stack, mark it as "queued"
                    mainContext = new Context();
                    List<Integer> cids = HarvestedCollection.findReady(mainContext);
                    log.info("Collections ready for immediate harvest: " + cids.toString());

                    for (Integer cid : cids) {
                        addThread(cid);
                    }

                    // Stage #2: start up all the threads currently in the queue up to the maximum number
                    while (!harvestThreads.isEmpty()) {
                        synchronized(HarvestScheduler.class) {
                            activeThreads++;
                        }
                        Thread activeThread = new Thread(harvestThreads.pop());
                        activeThread.start();
                        log.info("Thread started: " + activeThread.toString());

                        /* Wait while the number of threads running is greater than or equal to max */
                        while (activeThreads >= maxActiveThreads) {
                            /* Wait a second */
                            Thread.sleep(1000);
                        }
                    }

                    // Finally, wait for the last few remaining threads to finish
                    // TODO: this step might be unnecessary. Theoretically a single very long harvest process
                    // could then lock out all the other ones from starting on their next iteration.
                    // FIXME: also, this might lead to a situation when a single thread getting stuck without
                    // throwing an exception would shut down the whole scheduler
                    while (activeThreads != 0) {
                            /* Wait a second */
                            Thread.sleep(1000);
                    }

                    // Commit everything
                    try {
                            mainContext.commit();
                            mainContext.complete();
                            log.info("Done with iteration " + i);
                    } catch (SQLException e) {
                            e.printStackTrace();
                            mainContext.abort();
                    }

                }
                catch (Exception e) {
                        log.error("Exception on iteration: " + i);
                        e.printStackTrace();
                }

                // Stage #3: figure out how long until the next iteration and wait
                try {
                    Context tempContext = new Context();
                    int nextCollectionId = HarvestedCollection.findOldestHarvest(tempContext);
                    HarvestedCollection hc = HarvestedCollection.find(tempContext, nextCollectionId);

                    int harvestInterval = ConfigurationManager.getIntProperty("oai", "harvester.harvestFrequency");
                    if (harvestInterval == 0)
                    {
                        harvestInterval = 720;
                    }

                    Date nextTime;
                    long nextHarvest = 0;
                    if (hc != null) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(hc.getHarvestDate());
                        calendar.add(Calendar.MINUTE, harvestInterval);
                        nextTime = calendar.getTime();
                        nextHarvest = nextTime.getTime() +  - new Date().getTime();
                    }

                    long upperBound = Math.min(nextHarvest,maxHeartbeat);
                    long delay = Math.max(upperBound, minHeartbeat) + 1000;


                    tempContext.complete();

                    status = HARVESTER_STATUS_SLEEPING;
                    synchronized(lock) {
                        lock.wait(delay);
                    }
                }
                catch (InterruptedException ie) {
                        log.warn("Interrupt: " + ie.getMessage());
                }
                catch (SQLException e) {
                        e.printStackTrace();
                }

                i++;
            }
        }


        /**
         * Adds a thread to the ready stack. Can also be called externally to queue up a collection
         * for harvesting before it is "due" for another cycle. This allows starting a harvest process
         * from the UI that still "plays nice" with these thread mechanics instead of making an
         * asynchronous call to runHarvest().
         */
        public static void addThread(int collecionID) throws SQLException, IOException, AuthorizeException {
            log.debug("****** Entered the addThread method. Active threads: " + harvestThreads.toString());
            Context subContext = new Context();
            subContext.setCurrentUser(harvestAdmin);

            HarvestedCollection hc = HarvestedCollection.find(subContext, collecionID);
            hc.setHarvestStatus(HarvestedCollection.STATUS_QUEUED);
            hc.update();
            subContext.commit();

            HarvestThread ht = new HarvestThread(subContext, hc);
            harvestThreads.push(ht);

            log.debug("****** Queued up a thread. Active threads: " + harvestThreads.toString());
            log.info("Thread queued up: " + ht.toString());
        }

    }

    /**
     * A harvester thread used to execute a single harvest cycle on a collection
     * @author alexey
     */
    private static class HarvestThread extends Thread {
        Context context;
        HarvestedCollection hc;


        HarvestThread(Context context, HarvestedCollection hc) throws SQLException {
                this.context = context;
                this.hc = hc;
        }

        public void run() {
                log.info("Thread for collection " + hc.getCollectionId() + " starts.");
                runHarvest();
        }

        private void runHarvest()
        {
            Collection dso = null;
            try {
                dso = Collection.find(context, hc.getCollectionId());
                OAIHarvester harvester = new OAIHarvester(context, dso, hc);
                harvester.runHarvest();
            }
            catch (RuntimeException e) {
                log.error("Runtime exception in thread: " + this.toString());
                log.error(e.getMessage() + " " + e.getCause());
                hc.setHarvestMessage("Runtime error occured while generating an OAI response");
                hc.setHarvestStatus(HarvestedCollection.STATUS_UNKNOWN_ERROR);
            }
            catch (Exception ex) {
                log.error("General exception in thread: " + this.toString());
                log.error(ex.getMessage() + " " + ex.getCause());
                hc.setHarvestMessage("Error occured while generating an OAI response");
                hc.setHarvestStatus(HarvestedCollection.STATUS_UNKNOWN_ERROR);
            }
            finally
            {
                try {
                    hc.update();
                    context.restoreAuthSystemState();
                    context.complete();
                }
                catch (RuntimeException e) {
                    log.error("Unexpected exception while recovering from a harvesting error: " + e.getMessage(), e);
                    context.abort();
                }
                catch (Exception e) {
                        log.error("Unexpected exception while recovering from a harvesting error: " + e.getMessage(), e);
                        context.abort();
                }

                synchronized (HarvestScheduler.class) {
                        HarvestScheduler.activeThreads--;
                }
            }

            log.info("Thread for collection " + hc.getCollectionId() + " completes.");
        }
    }

}
