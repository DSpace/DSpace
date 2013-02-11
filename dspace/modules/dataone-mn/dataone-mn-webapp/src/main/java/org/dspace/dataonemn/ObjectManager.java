package org.dspace.dataonemn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.handle.HandleManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

public class ObjectManager implements Constants {
    
    private static final Logger log = Logger.getLogger(ObjectManager.class);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static final int DEFAULT_START = 0;
    public static final int DEFAULT_COUNT = 20;

    protected Context myContext;
    protected String myData;
    
    public ObjectManager(Context aContext, String aCollection) {
	myContext = aContext;
	myData = aCollection;
    }

    public void printList(OutputStream aOutStream) throws SQLException, IOException {
	printList(DEFAULT_START, DEFAULT_COUNT, aOutStream);
    }

    public void printList(Date aFrom, Date aTo, OutputStream aOutStream) throws SQLException, IOException {
	printList(DEFAULT_START, DEFAULT_COUNT, aFrom, aTo, null, aOutStream);
    }

    public void printList(Date aFrom, Date aTo, String aObjFormat, OutputStream aOutStream)
	throws SQLException, IOException {
	printList(DEFAULT_START, DEFAULT_COUNT, aFrom, aTo, aObjFormat,
		  aOutStream);
    }

    public void printList(int aStart, int aCount, OutputStream aOutStream)
                    throws SQLException, IOException {
        printList(aStart, aCount, null, null, null, aOutStream);
    }

    public void printList(int aStart, int aCount, String aObjFormat,
                    OutputStream aOutStream) throws SQLException, IOException {
        printList(aStart, aCount, null, null, aObjFormat, aOutStream);
    }

    public void printList(int aStart, int aCount, Date aFrom, Date aTo,
                    String aObjFormat, OutputStream aOutStream) throws SQLException,
                    IOException {
        XMLSerializer serializer = new XMLSerializer(aOutStream);
        boolean countIsEven = aCount % 2 == 0 ? true : false;
        boolean startIsEven = aStart % 2 == 0 ? true : false;
        ListObjects list = new ListObjects();
        long total;
        try {
            if(aObjFormat != null && aObjFormat.equals(DRYAD_NAMESPACE)) {
                // Metadata format requested, do not pass object format to query
                total = queryTotalDataFilesFromDatabase(aFrom, aTo, null);
            } else {
                total = queryTotalDataFilesFromDatabase(aFrom, aTo, aObjFormat);
            }
            // This returns the number of matched objects in the database
            // We will double it
            if(aObjFormat == null) { total *= 2; }
        } catch (NotFoundException ex) {
            log.error("Unable to query total from database: " + ex);
            total = 0;
        }

        myContext.turnOffAuthorisationSystem();

        log.debug("Setting start parameter to: " + aStart);
        list.setStart(aStart);
        log.debug("Setting total parameter to: " + Long.toString(total));
        list.setTotal(total);
        
        /*
         * This will list items in the Dryad Data Files collection.
         *
         * When the object format is not specified, we query the database
         * for all items having bitstreams in the Dryad Data Files collection.
         * Two results (<ObjectInfo> elements) are produced from each result
         * row from the database.  One result represents XML metadata 
         * of the item and the other represents the bitstream of the item.
         * In this case, the count and offset parameters are shifted/scaled 
         * as necessary to accommodate this separation and produce expected
         * listings
         * 
         * When the object format is specified, there is no count/offset 
         * discrepancy.  If the specified format is the dryad metadata format,
         * the format parameter is not passed to the database query, and the
         * bitstream results are left out of the response.
         * 
         * If the specified format is not the dryad metadata format, the format
         * parameter IS passed to the database query, and the XML metadata results
         * are left out of the response.
         */
        int offsetForDatabaseQuery = -1;
        int countForDatabaseQuery = -1;
            
        if(aObjFormat == null) {
            // object format not specified.  We need to retrieve half as much
            // data from the database and scale the offsets
            // 0 = metadata
            // 1 = bitstream
            // 2 = metadata
            // 3 = bitstream
            // ...
            
            if(startIsEven && countIsEven) {
                // both start and count are even, divide each by two
                offsetForDatabaseQuery = aStart / 2;
                countForDatabaseQuery = aCount / 2;
            } else if(startIsEven && !countIsEven) {
                // start is even but count is odd
                // the first item should be metadata but the last item will be metadata
                offsetForDatabaseQuery = aStart / 2;
                countForDatabaseQuery = (aCount + 1) / 2;
            } else if(!startIsEven && countIsEven) {
                // start is odd and count is even
                // starting with bitstream but need to subtract 1 before dividing
                offsetForDatabaseQuery = (aStart - 1) / 2;
                // also need to fetch an additional row from DB because of the offset
                countForDatabaseQuery = (aCount + 2) / 2;
            } else if(!startIsEven && !countIsEven) {
                // start is odd and count is odd
                offsetForDatabaseQuery = (aStart - 1) / 2;
                countForDatabaseQuery = (aCount + 1) / 2;
            }
            // after fetching from database, remember to chop first and/or last if needed
        } else {
            // objFormat is not null.  
            countForDatabaseQuery = aCount;
            offsetForDatabaseQuery = aStart;
        }
        TableRowIterator iterator = null;
        if(aObjFormat != null && aObjFormat.equals(DRYAD_NAMESPACE)) {
            // Metadata format requested, do not pass object format to query
            iterator = queryDataFilesDatabase(offsetForDatabaseQuery, countForDatabaseQuery, aFrom, aTo, null);
        } else {
            iterator = queryDataFilesDatabase(offsetForDatabaseQuery, countForDatabaseQuery, aFrom, aTo, aObjFormat);
        }
        List<nu.xom.Element> elementList = new ArrayList<nu.xom.Element>();
        while(iterator.hasNext()) {
            TableRow tr = iterator.next();
            String doi = tr.getStringColumn("doi");
            String format = tr.getStringColumn("format");
            String checksum = tr.getStringColumn("checksum");
            String checksumAlgorithm = tr.getStringColumn("checksum_algorithm");
            Date dateAvailable = tr.getDateColumn("date_available");
            long size = tr.getLongColumn("size_bytes");

            log.debug("Building '" + doi + "' for mn list");
            // need one for the bitstream and one for the metadata
            // convert DOI to http form if necessary
            if (doi.startsWith("doi:")) {
                doi = "http://dx.doi.org/" + doi.substring("doi:".length());
                log.debug("converted DOI to http form. It is now " + doi);
            }

            String lastModified = dateFormatter.format(dateAvailable);

            ObjectInfo bitstreamInfo = new ObjectInfo(doi);
            bitstreamInfo.setChecksum(checksumAlgorithm, checksum);
            bitstreamInfo.setSize(size);
            bitstreamInfo.setLastModified(lastModified);
            bitstreamInfo.setObjectFormat(format);

            nu.xom.Element[] infoElements =  bitstreamInfo.createInfoElements();
            if(aObjFormat == null) {
                // object format not specified, add both  metadata and bitstream
                elementList.add(infoElements[0]); // the metadata
                elementList.add(infoElements[1]); // the bitstream
            } else if(aObjFormat.equals(DRYAD_NAMESPACE)) {
                elementList.add(infoElements[0]); // just the metadata
            } else {
                elementList.add(infoElements[1]); // just the bitstream
            }
        };
        
        // After assembling the list, check if we need to trim the start/end of 
        // the list
        if(aObjFormat == null) {
            if(!startIsEven) {
                // start was odd, so remove the first element
                if(elementList.size() > 0){
                    elementList = elementList.subList(1, elementList.size());
                    // could also just remove the 0th element
                }
            }
            // now just trim the list to the size of the count if needed
            if(elementList.size() > aCount) {
                elementList = elementList.subList(0, aCount);
            }
        }
        
        log.debug("Setting count parameter to: " + Integer.toString(elementList.size()));
        list.setCount(elementList.size()); 
        serializer.writeStartTag(list);
        for(nu.xom.Element element : elementList) {
            serializer.write(element);
        }
        serializer.writeEndTag(list);
        serializer.flush();
        aOutStream.close();

        myContext.restoreAuthSystemState();
    }

	public long getObjectSize(String aID)
	throws NotFoundException, IOException, SQLException {
	    long size = 0;
	    Item item = getDSpaceItem(aID);

	    if(!aID.endsWith("/bitstream")) {
	        DisseminationCrosswalk xWalk = (DisseminationCrosswalk) PluginManager
	        .getNamedPlugin(DisseminationCrosswalk.class,
	                DRYAD_CROSSWALK);
	        try {
	            Element result = xWalk.disseminateElement(item);
	            Format ppFormat = Format.getPrettyFormat();
	            StringWriter writer = new StringWriter();
	            Namespace dryadNS = result.getNamespace();
	            Element wrapper = result.getChild("DryadMetadata", dryadNS);

	            if (wrapper != null) {
	                result = wrapper.getChild("DryadDataFile", dryadNS);
	            }

	            new XMLOutputter(ppFormat).output(result, writer);

	            size = (long) writer.toString().length();
	        }
	        catch (AuthorizeException details) {
	            log.error("Authorization problem", details);
	            throw new RuntimeException(details);
	        }
	        catch (CrosswalkException details) {
	            log.error("Unable to crosswalk metadata", details);
	            throw new RuntimeException(details);
	        }
	    } else {
	        size = getOrigBitstream(item).getSize();
	    }

	    return size;
	}
        
        public String[] generateXMLChecksum(String aID)
	throws NotFoundException, SQLException, IOException {
	Item item = getDSpaceItem(aID);
	String checksumAlgo = "";
	String checksum = "";
	
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MessageDigest md = MessageDigest.getInstance(DEFAULT_CHECKSUM_ALGO);
            StringBuffer hexString = new StringBuffer();
            byte[] digest;

            getMetadataObject(aID, outputStream);
            md.update(outputStream.toByteArray());
            checksumAlgo = DEFAULT_CHECKSUM_ALGO;
            digest = md.digest();

            for (int index = 0; index < digest.length; index++) {
                hexString.append(Integer.toHexString(0xFF & digest[index]));
            }

            checksum = hexString.toString();

            log.debug("Calculated XML checksum (" + checksum + ") for " + aID);
        } catch (NoSuchAlgorithmException details) {
            log.error("unexpected checksum algorithm", details);
            throw new RuntimeException(details);
        }
    return new String[] { checksum, checksumAlgo };
    }
    

    
    private long queryTotalDataFilesFromDatabase(Date fromDate, Date toDate, String objFormat)
    throws SQLException, NotFoundException {
        //start and count will be ignored
        TableRowIterator it = queryDataFilesDatabase(true, 0, 0, fromDate, toDate, objFormat);
        if(it.hasNext()) {
            return it.next().getLongColumn("total");
        } else {
            throw new NotFoundException("Unable to get total from database query");
        }
    }
    
    private TableRowIterator queryDataFilesDatabase(int start, int count, Date fromDate, Date toDate, String objFormat) 
    throws SQLException {
        return queryDataFilesDatabase(false, start, count, fromDate, toDate, objFormat);
        
    }
    
    private int getDateAvailableFieldID() 
    throws SQLException {
        String dateMetadataFieldIDquery = "SELECT f.metadata_field_id FROM "
                + "metadatafieldregistry f, metadataschemaregistry s "
                + "WHERE f.metadata_schema_id = s.metadata_schema_id "
                + "AND s.short_id = ? "
                + "AND f.element = ? "
                + "AND f.qualifier = ?";
        TableRow tr = DatabaseManager.querySingle(myContext, dateMetadataFieldIDquery, "dc", "date", "available");
        int dateAvailableFieldId = tr.getIntColumn("metadata_field_id");

        log.info("dc.date.available: metadata_field_id " + dateAvailableFieldId); // should be 12
        return dateAvailableFieldId;
    }
    
    private int getDCIdentifierFieldID()
    throws SQLException {
        String dcIdentifierFieldIDQuery = "SELECT f.metadata_field_id FROM "
                + "metadatafieldregistry f, metadataschemaregistry s "
                + "WHERE f.metadata_schema_id = s.metadata_schema_id "
                + "AND s.short_id = ? "
                + "AND f.element = ? "
                + "AND f.qualifier is null";                
        TableRow tr = DatabaseManager.querySingle(myContext, dcIdentifierFieldIDQuery, "dc", "identifier");
        int dcIdentifierFieldId = tr.getIntColumn("metadata_field_id");

        log.info("dc.identifier: metadata_field_id " + dcIdentifierFieldId); // should be 17
        return dcIdentifierFieldId;
    }    
    // pass countTotal as true to return the count instead of item data
    private TableRowIterator queryDataFilesDatabase(boolean countTotal, int start, int count, Date fromDate, Date toDate, String objFormat) 
    throws SQLException {
        try {
            // need special handling on object format.  If it is passed in as the metadata format
            // we need to ignore it in the database query
            
            Collection c = (Collection) HandleManager.resolveToObject(myContext, myData);
            int dateAvailableFieldId = getDateAvailableFieldID();
            int dcIdentifierFieldId = getDCIdentifierFieldID();

            StringBuilder queryBuilder = new StringBuilder();
            // build up bind paramaters 
            List<Object> bindParameters = new ArrayList<Object>();
            queryBuilder.append("SELECT ");
            if(countTotal) {
                queryBuilder.append("  count(*) AS total ");
            } else {
                queryBuilder.append("  md.text_value AS doi, "); 
                queryBuilder.append("  bfr.mimetype AS format, "); 
                queryBuilder.append("  bit.checksum, "); 
                queryBuilder.append("  bit.checksum_algorithm, "); 
                queryBuilder.append("  mv.text_value::timestamp AS date_available,  "); 
                queryBuilder.append("  bit.size_bytes "); 
            }
            queryBuilder.append("FROM  "); 
            queryBuilder.append("  item AS it ");
            queryBuilder.append("  JOIN collection2item as c2i using (item_id) ");
            queryBuilder.append("  JOIN collection as col using (collection_id) ");
            queryBuilder.append("  JOIN metadatavalue AS mv using (item_id) "); 
            queryBuilder.append("  JOIN metadatavalue AS md using (item_id) "); 
            queryBuilder.append("  JOIN item2bundle AS i2b using (item_id) "); 
            queryBuilder.append("  JOIN bundle AS bun using (bundle_id) "); 
            queryBuilder.append("  JOIN bundle2bitstream as b2b using (bundle_id) "); 
            queryBuilder.append("  JOIN bitstream as bit using (bitstream_id) "); 
            queryBuilder.append("  LEFT JOIN bitstreamformatregistry as bfr using (bitstream_format_id) "); 
            queryBuilder.append("WHERE "); 
            queryBuilder.append("  mv.metadata_field_id = ? AND "); 
            bindParameters.add(dateAvailableFieldId);
            queryBuilder.append("  md.metadata_field_id = ? AND "); 
            bindParameters.add(dcIdentifierFieldId);
            queryBuilder.append("  md.place = 1 AND "); 
            queryBuilder.append("  bun.name = ? AND ");
            queryBuilder.append("  lower(bit.name) NOT IN ('readme.txt','readme.txt.txt') AND ");
            bindParameters.add(org.dspace.core.Constants.DEFAULT_BUNDLE_NAME);
            if(objFormat != null) {
                // limit bundle format to the provided objFormat
                log.info("Requested objFormat: " + objFormat);
                queryBuilder.append("  bfr.mimetype = ? AND ");
                bindParameters.add(objFormat);
            }

            if(fromDate != null) {
                log.info("Requested fromDate: " + fromDate);
                // Postgres-specific, casts text_value to a timestamp
                queryBuilder.append("  mv.text_value::timestamp > ? AND ");
                bindParameters.add(new java.sql.Date(fromDate.getTime()));
            }

            if(toDate != null) {
                log.info("Requested toDate: " + toDate);
                // Postgres-specific, casts text_value to a timestamp
                queryBuilder.append("  mv.text_value::timestamp < ? AND "); // bind to toDate
                bindParameters.add(new java.sql.Date(toDate.getTime()));
            }
            queryBuilder.append("  col.collection_id = ? "); 
            bindParameters.add(c.getID()); 
            
            if(!countTotal) {
                queryBuilder.append("ORDER BY date_available ASC, doi ASC ");
                queryBuilder.append("LIMIT ? "); 
                bindParameters.add(count);
                queryBuilder.append("OFFSET ? "); 
                bindParameters.add(start);
            }
            // sample query for casting text_value to timestamp, postgres-specific
            // select * from metadatavalue where metadata_field_id = 12 
            // and text_value::timestamp > to_timestamp('2009-06-01','YYYY-MM-DD') 
            // and text_value::timestamp < to_timestamp('2009-07-01','YYYY-MM-DD')
            // limit 10;
            return DatabaseManager.query(myContext, queryBuilder.toString(), bindParameters.toArray());
        } catch (SQLException ex) {
            log.error("SQL Exception: " + ex);
            throw ex;
        }
    }

    /**
       Retrieve a DSpace item by identifier. If the identifier includes the "/bitstream" suffix, returns the Item
       containing the bistream.
    **/
    public Item getDSpaceItem(String aID) throws IOException, SQLException, NotFoundException {
	log.debug("Retrieving DSpace item " + aID);
	
	DOIIdentifierProvider doiService = new DSpace().getSingletonService(DOIIdentifierProvider.class);
	Item item = null;
	
	try {

	    if(aID.endsWith("/bitstream")) {
		int bitsIndex = aID.indexOf("/bitstream");
		String shortID = aID.substring(0,bitsIndex);
		aID = shortID;
	    }
	    item = (Item) doiService.resolve(myContext, aID, new String[] {});
	} catch (IdentifierNotFoundException e) {
	    log.error(aID + " not found!");
	    throw new NotFoundException(aID);
	} catch (IdentifierNotResolvableException e) {
	    log.error(aID + " not resolvable!");
	    throw new NotFoundException(aID);
	}
	
	if(item == null) {
	    log.error(aID + " is null!");
	    throw new NotFoundException(aID);
	}
	
	return item;
    }
    
    
    protected String getNameExt(String aName) {
	int suffixIndex = aName.lastIndexOf(".") + 1;
	
	if (suffixIndex != -1 && suffixIndex < aName.length()) {
	    return  aName.substring(suffixIndex);
	}
	
	return "*";
    }
    
    /**
       Retrieve the first bitstream in a bundle. The bitstream must be in a bundle
       marked "ORIGINAL". Bitstreams for "readme" files are ignored.
    **/
    protected Bitstream getOrigBitstream(Item aItem)
	throws SQLException, NotFoundException {
	Bundle[] bundles = aItem.getBundles("ORIGINAL");
	
	log.debug("Getting bitstreams for " + aItem.getHandle());
	
	if (bundles.length > 0) {
	    for (Bitstream bitstream : bundles[0].getBitstreams()) {
		String name = bitstream.getName();
		log.debug("Checking '" + name + "' bitstream");
		
		if (!name.equalsIgnoreCase("readme.txt")
		    && !name.equalsIgnoreCase("readme.txt.txt")) {
		    log.debug("Getting bitstream info from: " + name);
		    return bitstream;
		}
	    }
	}
	
	throw new NotFoundException("No bitstream for " + aItem.getHandle() + " found");
    }
    
    /**
       Retrieve the first bitstream in a bundle. The bitstream must be in a bundle
       marked "ORIGINAL". Bitstreams for "readme" files are ignored.
    **/
    public Bitstream getFirstBitstream(Item item) throws SQLException, NotFoundException {
	Bitstream result = null;
	
	Bundle[] bundles = item.getBundles("ORIGINAL");
	if (bundles.length == 0) {
	    log.error("Didn't find any original bundles for " + item.getHandle());
	    throw new NotFoundException("data bundle for " + item.getHandle() + " not found");
	}
	log.debug("This object has " + bundles.length + " bundles");
	
	Bitstream[] bitstreams = bundles[0].getBitstreams();
	boolean found = false;
	for(int i = 0; i < bitstreams.length && !found; i++) {
	    result = bitstreams[i];
	    String name = result.getName();
	    
	    if (!name.equalsIgnoreCase("readme.txt")
		&& !name.equalsIgnoreCase("readme.txt.txt")) {
		log.debug("Retrieving bitstream " + name);
		found = true;
	    }
	}	    
	if (!found) {
	    log.error("unable to locate a valid bitstream within the first bundle of " + item.getHandle());
	    throw new NotFoundException(item.getHandle() + " -- first bitstream wasn't found");
	}
	
	return result;
    }
    
    /**
     * Returns an array with checksum and algorithm used.
     * 
     * @param aID The DOI of the object we want to retrieve
     * @return An array with checksum and algorithm used.
     * @throws NotFoundException If the requested ID was not found
     * @throws SQLException If there was trouble interacting with DSpace
     * @throws IOException If there is trouble reading or writing data
     */
    public String[] getObjectChecksum(String aID)
	throws NotFoundException, SQLException, IOException {
	Item item = getDSpaceItem(aID);
	String checksumAlgo = "";
	String checksum = "";
	
	if (aID.endsWith("/bitstream")) {
	    Bitstream bitStream = getOrigBitstream(item);
	    checksum = bitStream.getChecksum();
	    checksumAlgo = bitStream.getChecksumAlgorithm();
	} else {
	    try {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		MessageDigest md = MessageDigest.getInstance(DEFAULT_CHECKSUM_ALGO);
		StringBuffer hexString = new StringBuffer();
		byte[] digest;
		
		getMetadataObject(aID, outputStream);
		md.update(outputStream.toByteArray());
		checksumAlgo = DEFAULT_CHECKSUM_ALGO;
		digest = md.digest();
		
		for (int index = 0; index < digest.length; index++) {
		    hexString.append(Integer.toHexString(0xFF & digest[index]));
		}
		
		checksum = hexString.toString();
		
		log.debug("Calculated XML checksum (" + checksum + ") for " + aID);
	    } catch (NoSuchAlgorithmException details) {
		log.error("unexpected checksum algorithm", details);
		throw new RuntimeException(details);
	    }
	}
	return new String[] { checksum, checksumAlgo };
    }
    
    /**
     * Write a metadata object to the output stream.
     **/
    public void getMetadataObject(String aID, OutputStream aOutputStream)
	throws IOException, SQLException, NotFoundException {
	
	log.debug("Retrieving metadata for " + aID);
	
	try {
	    Item item = getDSpaceItem(aID);
	    log.debug(" (DSO_ID: " + item.getID() + ") -- " + item.getHandle());
	    DisseminationCrosswalk xWalk =
		(DisseminationCrosswalk) PluginManager.getNamedPlugin(DisseminationCrosswalk.class, DRYAD_CROSSWALK);
	    
	    if (!xWalk.canDisseminate(item)) {
		log.warn("xWalk says item cannot be disseminated: " + item.getHandle());
	    }
	    
	    Element result = xWalk.disseminateElement(item);
	    Namespace dcTermsNS = Namespace.getNamespace(DC_TERMS_NAMESPACE);
	    Namespace dryadNS = result.getNamespace();
	    Element file = result.getChild("DryadDataFile", dryadNS);
	    Element idElem;
	    
	    if (file != null) {
		result = file;
	    }
	    
	    idElem = result.getChild("identifier", dcTermsNS);
	    
	    // adjust the identifier to be a full DOI if it isn't one already
	    if (idElem != null) {
		String theID = idElem.getText();
		if(theID.startsWith("doi:")) {
		    theID = "http://dx.doi.org/" + theID.substring("doi:".length());
		    idElem.setText(theID);
		}
	    }
	    
	    Format ppFormat = Format.getPrettyFormat();
	    new XMLOutputter(ppFormat).output(result, aOutputStream);
	    aOutputStream.close();
	} catch (AuthorizeException details) {
	    // We've disabled authorization for this context, so this should never happen
	    log.warn("Shouldn't see this exception!", details);
	} catch (CrosswalkException details) {
	    log.error(details.getMessage(), details);
	    throw new RuntimeException(details);
	} catch (MalformedURLException details) {
	    log.error("Malformed URL!", details);
	}
	
    }
    
    public void writeBitstream(InputStream aInputStream,
			       OutputStream aOutputStream) throws IOException {
	BufferedInputStream iStream = new BufferedInputStream(aInputStream);
	ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
	BufferedOutputStream oStream = new BufferedOutputStream(aOutputStream);
	byte[] buffer = new byte[1024];
	int bytesRead = 0;
	
	while (true) {
	    bytesRead = iStream.read(buffer);
	    if (bytesRead == -1)
		break;
	    byteStream.write(buffer, 0, bytesRead);
	};
	
	oStream.write(byteStream.toByteArray());
	oStream.close();
	iStream.close();
    }
    
    public void completeContext() {
	try {
	    if (myContext != null) {
		myContext.complete();
	    }
	} catch (SQLException e) {
	    log.error("unable to complete DSpace context", e);
	    
	    // don't pass on the exception because this isn't an error in responding to a DataONE request,
	    // it's an internal error in shutting down resources.
	}
    }
}
