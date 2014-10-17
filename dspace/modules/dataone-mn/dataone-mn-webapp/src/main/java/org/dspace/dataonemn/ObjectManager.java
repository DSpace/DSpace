package org.dspace.dataonemn;

import java.io.*;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;


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
import org.dspace.content.DCValue;

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

import org.dataone.service.types.v1.Identifier;
import org.dataone.ore.ResourceMapFactory;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.OREException;
import org.dspace.foresite.ORESerialiserException;

import java.net.URISyntaxException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class ObjectManager implements Constants {
    
    private static final Logger log = Logger.getLogger(ObjectManager.class);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ") {
        public StringBuffer format(Date date, StringBuffer toAppendTo, java.text.FieldPosition pos) {
            StringBuffer toFix = super.format(date, toAppendTo, pos);
            return toFix.insert(toFix.length()-2, ':');
        }
    };
    public static final int DEFAULT_START = 0;
    public static final int DEFAULT_COUNT = 20;
    
    protected Context myContext;
    protected String myFiles;
    protected String myPackages;
    
    public ObjectManager(Context aContext, String aFilesCollection, String aPackagesCollection) {
	myContext = aContext;
	myFiles = aFilesCollection;
        myPackages = aPackagesCollection;
    }
    
    public void printList(OutputStream aOutStream, boolean useTimestamps) throws SQLException, IOException {
	printList(DEFAULT_START, DEFAULT_COUNT, aOutStream, useTimestamps);
    }
    
    public void printList(Date aFrom, Date aTo, OutputStream aOutStream, boolean useTimestamps) throws SQLException, IOException {
	printList(DEFAULT_START, DEFAULT_COUNT, aFrom, aTo, null, aOutStream, useTimestamps);
    }
    
    public void printList(Date aFrom, Date aTo, String aObjFormat, OutputStream aOutStream, boolean useTimestamps)
	throws SQLException, IOException {
	printList(DEFAULT_START, DEFAULT_COUNT, aFrom, aTo, aObjFormat,
		  aOutStream, useTimestamps);
    }

    public void printList(int aStart, int aCount, OutputStream aOutStream, boolean useTimestamps)
                    throws SQLException, IOException {
        printList(aStart, aCount, null, null, null, aOutStream, useTimestamps);
    }

    public void printList(int aStart, int aCount, String aObjFormat,
			  OutputStream aOutStream, boolean useTimestamps) throws SQLException, IOException {
        printList(aStart, aCount, null, null, aObjFormat, aOutStream, useTimestamps);
    }

    public void printList(int aStart, int aCount, Date aFrom, Date aTo,
			  String aObjFormat, OutputStream aOutStream, boolean useTimestamps) throws SQLException,
                    IOException {
	log.debug("printing object list with start=" +aStart +
		  ", count=" + aCount +
		  ", from=" + aFrom +
		  ", to=" + aTo +
		  ", format=" + aObjFormat +
		  ", timestamps=" + useTimestamps);
        XMLSerializer serializer = new XMLSerializer(aOutStream);
        ListObjects list = new ListObjects();
        
        long totalDataFileElements;
        long totalDataPackageElements;
        try {
            if(aObjFormat != null && aObjFormat.equals(ORE_NAMESPACE)) {
                // Relationships requested, not files
                totalDataFileElements = 0l;
            } else if(aObjFormat != null && aObjFormat.equals(DRYAD_NAMESPACE)) {
                // Metadata format requested, do not pass object format to query
                totalDataFileElements = queryTotalDataFilesFromDatabase(aFrom, aTo, null);
            } else {
                totalDataFileElements = queryTotalDataFilesFromDatabase(aFrom, aTo, aObjFormat);
            }
            // This returns the number of matched objects in the database
            // We will double it
            if(aObjFormat == null) { totalDataFileElements *= 2; }
        } catch (NotFoundException ex) {
            log.error("Unable to query total files from database: " + ex);
            totalDataFileElements = 0l;
        }
        
        try {
            if(aObjFormat == null) {
                // no format specified, return two results for each row in the database
                totalDataPackageElements = queryTotalDataPackagesFromDatabase(aFrom, aTo) * 2;
            } else if(aObjFormat.equals(ORE_NAMESPACE) || aObjFormat.equals(DRYAD_NAMESPACE)) {
                // format specified is either the Resource Map or the Metadata format
                // either way, return one result for each row in the database
                totalDataPackageElements = queryTotalDataPackagesFromDatabase(aFrom, aTo);
            } else {
                // format is specified but not one of the metadata formats
                // Perhaps a mime type.  Do not return any packages
                totalDataPackageElements = 0l;
            }
        } catch (NotFoundException ex) {
            log.error("Unable to query total packages from datbase: " + ex);
            totalDataPackageElements = 0l;
        }

        myContext.turnOffAuthorisationSystem();

        log.debug("Setting start parameter to: " + aStart);
        list.setStart(aStart);
        long total = totalDataFileElements + totalDataPackageElements;
        log.debug("Setting total parameter to: " + Long.toString(total));
        list.setTotal(total);
        
        /*
         * The result list will contain the list of packages, followed
         * by the list of data files.  
         */
        
        int packageStart, packageCount, fileStart, fileCount;
        packageStart = aStart;
        if(aStart + aCount <= totalDataPackageElements) {
            // user has requested a range that is entirely packages
            packageCount = aCount;
            fileCount = 0;
            fileStart = 0;
        } else if(aStart < totalDataPackageElements && 
                (aStart + aCount) >= totalDataPackageElements) {
            // user has requested a range that starts in packages and
            // ends in files
            packageCount = (int) totalDataPackageElements - packageStart;
            fileStart = 0;
            fileCount = aCount - packageCount;
        } else {
            // user has requested a range that is entirely files
            packageStart = 0;
            packageCount = 0;
            fileStart = (int) (aStart - totalDataPackageElements);
            fileCount = aCount;
        }
        List<nu.xom.Element> packageElementList = buildDataPackagesList(packageStart, packageCount, aFrom, aTo, aObjFormat, useTimestamps);
        List<nu.xom.Element> fileElementList = buildDataFilesList(fileStart, fileCount, aFrom, aTo, aObjFormat, useTimestamps);
        
        log.debug("Setting count parameter to: " + Integer.toString(fileElementList.size() + packageElementList.size()));
        list.setCount(fileElementList.size() + packageElementList.size());
        
        serializer.writeStartTag(list);
        for(nu.xom.Element element : packageElementList) {
            serializer.write(element);
        }
        for(nu.xom.Element element : fileElementList) {
            serializer.write(element);
        }
        serializer.writeEndTag(list);
        serializer.flush();
        aOutStream.close();

        myContext.restoreAuthSystemState();
    }

    private List<nu.xom.Element> buildDataPackagesList(int aStart, int aCount, Date aFrom, Date aTo, String aObjFormat, boolean useTimestamps) 
    throws SQLException, IOException {
        List<nu.xom.Element> packageElementList = new ArrayList<nu.xom.Element>();
        if(aCount == 0) {
            return packageElementList;
        }
        
        /* Now assemble the list of packages */
        boolean countIsEven = aCount % 2 == 0 ? true : false;
        boolean startIsEven = aStart % 2 == 0 ? true : false;
                
        int offsetForDatabaseQuery = -1;
        int countForDatabaseQuery = -1;
            
        if(aObjFormat == null) {
            // object format not specified.  We need to retrieve half as much
            // data from the database and scale the offsets
            // 0 = metadata
            // 1 = resource map
            // 2 = metadata
            // 3 = resource map
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
                // starting with resource map but need to subtract 1 before dividing
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
        
        TableRowIterator iterator = queryDataPackagesDatabase(offsetForDatabaseQuery, countForDatabaseQuery, aFrom, aTo);
        while(iterator.hasNext()) {
            TableRow tr = iterator.next();
            String doi = tr.getStringColumn("doi");
	    String idTimestamp = "";
	    String idRemTimestamp = "";
            Date dateAvailable = tr.getDateColumn("date_available");
	    Date lastModifiedDate = tr.getDateColumn("last_modified");
            String lastModified = dateFormatter.format(lastModifiedDate);

	    if(useTimestamps) {
		idTimestamp =  "?ver=" + lastModified;
		idRemTimestamp = "&" + idTimestamp.substring(1);
	    }

	    log.debug("timestamps=" + useTimestamps + ", idTimestamp=" + idTimestamp);
	    
            log.debug("Building '" + doi + "' for mn list");
	    doi = normalizeDoi(doi);

            PackageInfo packageInfo = new PackageInfo(doi, idTimestamp);
            packageInfo.setModificationDate(lastModified);

            try {
                String xmlChecksum[] = getObjectChecksum(doi, idTimestamp); // metadata
                packageInfo.setXmlChecksum(xmlChecksum[0]);
                packageInfo.setXmlChecksumAlgo(xmlChecksum[1]);
            } catch (NotFoundException ex) {
                log.error("Error getting checksum for " + doi + idTimestamp, ex);
                packageInfo.setXmlChecksum(DEFAULT_CHECKSUM);
                packageInfo.setXmlChecksumAlgo(DEFAULT_CHECKSUM_ALGO);
            }
            
            try {
                String resourceMapChecksum[] = getObjectChecksum(doi + "?format=d1rem", idRemTimestamp);
                packageInfo.setResourceMapChecksum(resourceMapChecksum[0]);
                packageInfo.setResourceMapChecksumAlgo(resourceMapChecksum[1]);
            } catch (NotFoundException ex) {
                log.error("Error getting checksum for " + doi + "?format=d1rem" + idRemTimestamp, ex);
                packageInfo.setResourceMapChecksum(DEFAULT_CHECKSUM);
                packageInfo.setResourceMapChecksumAlgo(DEFAULT_CHECKSUM_ALGO);
            }
            
            try {
                long xmlSize = getObjectSize(doi, idTimestamp);
                packageInfo.setXmlSize(xmlSize);
            } catch (NotFoundException ex) {
                log.error("Error getting size for " + doi + idTimestamp, ex);
            }
            try {
                long resourceMapSize = getObjectSize(doi + "?format=d1rem", idRemTimestamp);
                packageInfo.setResourceMapSize(resourceMapSize);
            } catch (NotFoundException ex) {
                log.error("Error getting size for " + doi + "?format=d1rem" + idRemTimestamp, ex);
            }

            nu.xom.Element[] infoElements = packageInfo.createInfoElements();
            if(aObjFormat == null) {
                // object format not specified, add both metadata resource element
                packageElementList.add(infoElements[0]); // the metadata element
                packageElementList.add(infoElements[1]); // the resource map element
            } else if(aObjFormat.equals(DRYAD_NAMESPACE)) {
                packageElementList.add(infoElements[0]); // just the metadata element
            } else if(aObjFormat.equals(ORE_NAMESPACE)) {
                packageElementList.add(infoElements[1]); // just the resource map element
            }
        }
        // After assembling the list, check if we need to trim the start/end
        if(aObjFormat == null) {
            if(!startIsEven) {
                // start was odd, so remove the first element
                if(packageElementList.size() > 0){
                    packageElementList = packageElementList.subList(1, packageElementList.size());
                }
            }
            // now just trim the list to the size of the count if needed
            if(packageElementList.size() > aCount) {
                packageElementList = packageElementList.subList(0, aCount);
            }
        }
        return packageElementList;
    }

    private List<nu.xom.Element> buildDataFilesList(int aStart, int aCount, Date aFrom, Date aTo, String aObjFormat, boolean useTimestamps) 
    throws SQLException, IOException {
        List<nu.xom.Element> fileElementList = new ArrayList<nu.xom.Element>();
        if(aCount == 0) {
            return fileElementList;
        }
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
        boolean countIsEven = aCount % 2 == 0 ? true : false;
        boolean startIsEven = aStart % 2 == 0 ? true : false;
                
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
        TableRowIterator iterator;
        if(aObjFormat != null && aObjFormat.equals(DRYAD_NAMESPACE)) {
            // Metadata format requested, do not pass object format to query
            iterator = queryDataFilesDatabase(offsetForDatabaseQuery, countForDatabaseQuery, aFrom, aTo, null);
        } else {
            iterator = queryDataFilesDatabase(offsetForDatabaseQuery, countForDatabaseQuery, aFrom, aTo, aObjFormat);
        }
        while(iterator.hasNext()) {
            TableRow tr = iterator.next();
            String doi = tr.getStringColumn("doi");
            String format = tr.getStringColumn("format");
            String checksum = tr.getStringColumn("checksum");
            String checksumAlgorithm = tr.getStringColumn("checksum_algorithm");
            Date dateAvailable = tr.getDateColumn("date_available");
	    Date lastModifiedDate = tr.getDateColumn("last_modified");
            String lastModified = dateFormatter.format(lastModifiedDate);
	    long size = tr.getLongColumn("size_bytes");

            log.debug("Building '" + doi + "' for mn list");
	    doi = normalizeDoi(doi);
	    String idTimestamp = "";
	    if(useTimestamps) {
		idTimestamp =  "?ver=" + lastModified;
	    }    

            ObjectInfo bitstreamInfo = new ObjectInfo(doi, idTimestamp);
            bitstreamInfo.setChecksum(checksumAlgorithm, checksum);
            bitstreamInfo.setSize(size);
            bitstreamInfo.setLastModified(lastModified);
            bitstreamInfo.setObjectFormat(format);

            try {
                String xmlChecksum[] = getObjectChecksum(doi, idTimestamp);
                bitstreamInfo.setXMLChecksum(xmlChecksum[0], xmlChecksum[1]);
            } catch (NotFoundException ex) {
                log.error("Unable to find object to generate XML checksum", ex);
                bitstreamInfo.setXMLChecksum(DEFAULT_CHECKSUM, DEFAULT_CHECKSUM_ALGO);
            }
            try {
                long xmlSize = getObjectSize(doi, idTimestamp);
                bitstreamInfo.setXMLSize(xmlSize);
            } catch (NotFoundException ex) {
                log.error("Unable to find object to calculate XML size", ex);
            }

            nu.xom.Element[] infoElements =  bitstreamInfo.createInfoElements();
            if(aObjFormat == null) {
                // object format not specified, add both  metadata and bitstream
                fileElementList.add(infoElements[0]); // the metadata
                fileElementList.add(infoElements[1]); // the bitstream
            } else if(aObjFormat.equals(DRYAD_NAMESPACE)) {
                fileElementList.add(infoElements[0]); // just the metadata
            } else {
                fileElementList.add(infoElements[1]); // just the bitstream
            }
        }
        
        // After assembling the list, check if we need to trim the start/end
        if(aObjFormat == null) {
            if(!startIsEven) {
                // start was odd, so remove the first element
                if(fileElementList.size() > 0){
                    fileElementList = fileElementList.subList(1, fileElementList.size());
                    // could also just remove the 0th element
                }
            }
            // now just trim the list to the size of the count if needed
            if(fileElementList.size() > aCount) {
                fileElementList = fileElementList.subList(0, aCount);
            }
        }
        return fileElementList;       
    }



    
    public long getObjectSize(String aID, String idTimestamp)
	throws NotFoundException, IOException, SQLException {
	long size = 0;
	Item item = getDSpaceItem(aID);

        if(aID.contains("format=d1rem")) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            getResourceMap(aID, idTimestamp, outputStream);
            size = outputStream.size();
        } else if(aID.endsWith("/bitstream")) {
	    size = getOrigBitstream(item).getSize();
        } else {
	    log.debug("crosswalking with " + DRYAD_CROSSWALK);
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
	}
        return size;
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
    
    private long queryTotalDataPackagesFromDatabase(Date fromDate, Date toDate)
    throws SQLException, NotFoundException {
        //start and count will be ignored
        TableRowIterator it = queryDataPackagesDatabase(true, 0, 0, fromDate, toDate);
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

    private TableRowIterator queryDataPackagesDatabase(int start, int count, Date fromDate, Date toDate) 
    throws SQLException {
        return queryDataPackagesDatabase(false, start, count, fromDate, toDate);
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
        Collection c = (Collection) HandleManager.resolveToObject(myContext, myFiles);
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
	    queryBuilder.append("  it.last_modified::timestamp AS last_modified, ");
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
        queryBuilder.append("  NOT it.withdrawn = true AND");
        queryBuilder.append("  it.in_archive = true AND ");
        queryBuilder.append("  mv.metadata_field_id = ? AND "); 
        bindParameters.add(dateAvailableFieldId);
        queryBuilder.append("  md.metadata_field_id = ? AND "); 
        bindParameters.add(dcIdentifierFieldId);
        queryBuilder.append("  md.place = 1 AND "); 
        queryBuilder.append("  bun.name = ? AND ");
        bindParameters.add(org.dspace.core.Constants.DEFAULT_BUNDLE_NAME);
        queryBuilder.append("  bit.description IS DISTINCT FROM 'dc_readme' AND ");
        if(objFormat != null) {
            // limit bundle format to the provided objFormat
            log.info("Requested objFormat: " + objFormat);
            queryBuilder.append("  bfr.mimetype = ? AND ");
            bindParameters.add(objFormat);
        }

        if(fromDate != null) {
            Timestamp fromTimestamp = new java.sql.Timestamp(fromDate.getTime());
            log.info("queryDataFilesDatabase: Requested fromDate: " + fromTimestamp.toString());
            // Postgres-specific, casts text_value to a timestamp
            queryBuilder.append("  mv.text_value::timestamp > ? AND ");
            bindParameters.add(fromTimestamp);
        }

        if(toDate != null) {
            Timestamp toTimestamp = new java.sql.Timestamp(toDate.getTime());
            log.info("queryDataFilesDatabase: Requested toDate: " + toTimestamp.toString());
            // Postgres-specific, casts text_value to a timestamp
            queryBuilder.append("  mv.text_value::timestamp < ? AND "); // bind to toDate
            bindParameters.add(toTimestamp);
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
    }

    private TableRowIterator queryDataPackagesDatabase(boolean countTotal, int start, int count, Date fromDate, Date toDate)
            throws SQLException {
        Collection c = (Collection) HandleManager.resolveToObject(myContext, myPackages);
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
            queryBuilder.append("  mv.text_value::timestamp AS date_available, ");
	        queryBuilder.append("  it.last_modified::timestamp AS last_modified ");
        }        
        queryBuilder.append("FROM ");
        queryBuilder.append("  item AS it ");
        queryBuilder.append("  JOIN collection2item as c2i using (item_id) ");
        queryBuilder.append("  JOIN collection as col using (collection_id) ");
        queryBuilder.append("  JOIN metadatavalue AS mv using (item_id) ");
        queryBuilder.append("  JOIN metadatavalue AS md using (item_id) ");
        queryBuilder.append("WHERE ");
        queryBuilder.append("  NOT it.withdrawn = true AND ");
        queryBuilder.append("  it.in_archive = true AND ");
        queryBuilder.append("  mv.metadata_field_id = ? AND ");
        bindParameters.add(dateAvailableFieldId);
        queryBuilder.append("  md.metadata_field_id = ? AND ");
        bindParameters.add(dcIdentifierFieldId);
        queryBuilder.append("  md.place = 1 AND ");
        if(fromDate != null) {
            Timestamp fromTimestamp = new java.sql.Timestamp(fromDate.getTime());
            log.info("queryDataPackagesDatabase: Requested fromDate: " + fromTimestamp.toString());
            // Postgres-specific, casts text_value to a timestamp
            queryBuilder.append("  mv.text_value::timestamp > ? AND ");
            bindParameters.add(fromTimestamp);
        }

        if(toDate != null) {
            Timestamp toTimestamp = new java.sql.Timestamp(toDate.getTime());
            log.info("queryDataPackagesDatabase: Requested toDate: " + toTimestamp.toString());
            // Postgres-specific, casts text_value to a timestamp
            queryBuilder.append("  mv.text_value::timestamp < ? AND "); // bind to toDate
            bindParameters.add(toTimestamp);
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
        return DatabaseManager.query(myContext, queryBuilder.toString(), bindParameters.toArray());
        
    }
    /**
       Retrieve a DSpace item by identifier. If the identifier includes the "/bitstream" suffix, returns the Item
       containing the bistream.
    **/
    public Item getDSpaceItem(String aID) throws IOException, SQLException, NotFoundException {
	log.debug("Retrieving DSpace item " + aID);

	// correct for systems that accidentally remove the second slash of the identifier
	if(aID.startsWith("http:/d")) {
	    aID = "http://d" + aID.substring("http:/d".length());
	}
	
	DOIIdentifierProvider doiService = new DSpace().getSingletonService(DOIIdentifierProvider.class);
	Item item = null;
	
	try {

	    if(aID.endsWith("/bitstream")) {
		int bitsIndex = aID.indexOf("/bitstream");
		String shortID = aID.substring(0,bitsIndex);
		aID = shortID;
	    }
	    if(aID.contains("?format=d1rem")) {
		int bitsIndex = aID.indexOf("?format=d1rem");
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
    public String[] getObjectChecksum(String aID, String idTimestamp)
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
		if(aID.contains("?format=d1rem")) {
                    getResourceMap(aID, idTimestamp, outputStream);
                } else {
                    getMetadataObject(aID, idTimestamp, outputStream);
                }
		md.update(outputStream.toByteArray());
		checksumAlgo = DEFAULT_CHECKSUM_ALGO;
		digest = md.digest();
		
		for (int index = 0; index < digest.length; index++) {
                    String byteString = Integer.toHexString(0xFF & digest[index]);
                    // Integer.toHexString does not add leading zeroes
                    if(byteString.length() < 2) {
                        hexString.append("0");
                    }
		    hexString.append(byteString);
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
    public void getMetadataObject(String aID, String idTimestamp, OutputStream aOutputStream)
	throws IOException, SQLException, NotFoundException {
	
	log.debug("Retrieving metadata for " + aID);
	
	try {
	    Item item = getDSpaceItem(aID);
	    log.debug(" (DSO_ID: " + item.getID() + ") -- " + item.getHandle());
	    log.debug("crosswalking with " + DRYAD_CROSSWALK);
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
		theID = normalizeDoi(theID);
		idElem.setText(theID);
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

    /**
     * Write an ORE resource map to the output stream.
     **/
    public void getResourceMap(String aID, String idTimestamp, OutputStream aOutputStream)
	throws IOException, SQLException, NotFoundException {
	
	log.debug("Retrieving resource map for " + aID + " with timestamp " + idTimestamp);
	
	try {
	    Item item = getDSpaceItem(aID);
	    log.debug(" (DSO_ID: " + item.getID() + ") -- " + item.getHandle());

	    // DOI
	    String doi = "[DOI not found]";
	    DCValue[] vals = item.getMetadata("dc.identifier");
	    if (vals.length == 0) {
		log.error("Object has no dc.identifier available " + aID);
	    } else {
		for(int i = 0; i < vals.length; i++) {
		    if (vals[i].value.startsWith("doi:") || vals[i].value.startsWith("http://doi")) {
			doi = normalizeDoi(vals[i].value);
			break;
		    }
		}
	    }

	    // DataFiles
	    DCValue[] dataFiles = item.getMetadata("dc.relation.haspart");
	    
	    ////////// generate a resource map
	    
	    // the ORE object's id
	    Identifier resourceMapId = new Identifier();
	    resourceMapId.setValue(doi + "?format=d1rem" + idTimestamp);
	    // the science metadata id
	    Identifier dataPackageId = new Identifier();
            // idTimestamp is passed in as the second query parameter,
            // and is prefixed with &.  In the dataPackageId, it is the first
            // query parameter, so the prefix should be ?
	    dataPackageId.setValue(doi + idTimestamp.replace('&','?'));
	    // data file identifiers
	    List<Identifier> dataIds = new ArrayList<Identifier>();
	    for(int i=0; i < dataFiles.length; i++) {
		String dataFileIdString  = normalizeDoi(dataFiles[i].value);
                Item fileItem = getDSpaceItem(dataFileIdString);
                // Do not include files that are embargoed (dc.date.available not present)
                if(fileItem.getMetadata("dc.date.available").length == 0) {
                    continue;
                }
		String dataFileTimestamp = "";
		if(idTimestamp.length() > 0) {
		    // get the timestamp for this file
		    Date fileModDate = fileItem.getLastModified();
		    String fileModString = dateFormatter.format(fileModDate);
		    dataFileTimestamp = "?ver=" + fileModString;
		}
		Identifier dataFileId = new Identifier();
		dataFileId.setValue(dataFileIdString + dataFileTimestamp);
		dataIds.add(dataFileId);
		Identifier dataFileBitstreamId = new Identifier();
		dataFileBitstreamId.setValue(dataFileIdString + "/bitstream");
		dataIds.add(dataFileBitstreamId);
	    }
	    
	    // associate the metadata and data identifiers
	    Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
	    idMap.put(dataPackageId, dataIds);
	    // generate the resource map
	    ResourceMapFactory rmf = ResourceMapFactory.getInstance();
	    ResourceMap resourceMap = rmf.createResourceMap(resourceMapId, idMap);
	    Date itemModDate = item.getLastModified();
	    resourceMap.setModified(itemModDate);

	    // serialize it as RDF/XML
	    String rdfXml = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);
            // Reorder the RDF/XML to a predictable order
            SAXBuilder builder = new SAXBuilder();
            try {
                Document d = builder.build(new StringReader(rdfXml));
                Iterator it = d.getRootElement().getChildren().iterator();
                List<Element> children = new ArrayList<Element>();
                while(it.hasNext()) {
                    Element element = (Element)it.next();
                    children.add(element);
                }
                d.getRootElement().removeContent();
                Collections.sort(children, new Comparator<Element> () {
                    @Override
                    public int compare(Element t, Element t1) {
                        return t.getAttributes().toString().compareTo(t1.getAttributes().toString());
                    }
                });
                for(Element el : children) {
                    d.getRootElement().addContent(el);
                }
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                rdfXml = outputter.outputString(d);
            } catch (JDOMException ex) {
                log.error("Exception parsing rdfXml", ex);
            }

	    PrintWriter writer = new PrintWriter(
						 new BufferedWriter(new OutputStreamWriter(aOutputStream)));
	    writer.print(rdfXml);
	    writer.flush();
	    aOutputStream.close();
	} catch (OREException e) {
	    log.error("ORE problem!", e);
	} catch (ORESerialiserException e) {
	    log.error("Serilizing problem!", e);
	} catch (URISyntaxException e) {
	    log.error("URI problem!", e);
	} catch (MalformedURLException details) {
	    log.error("Malformed URL!", details);
	}
	
    }

    private String normalizeDoi(String doi) {
	if (doi == null || doi.length() == 0) {
	    log.error("Attempt to normalize non-existant DOI");
	    return "";
	}
	if(doi.startsWith("doi:")) {
	    doi = "http://dx.doi.org/" + doi.substring("doi:".length());
	}
	if(doi.startsWith("10.")) {
	    doi = "http://dx.doi.org/" + doi;
	}
	return doi;
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
