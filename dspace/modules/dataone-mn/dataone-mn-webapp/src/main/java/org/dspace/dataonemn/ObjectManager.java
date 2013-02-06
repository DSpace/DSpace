package org.dspace.dataonemn;

import java.io.*;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

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

import org.dataone.service.types.v1.Identifier;
import org.dataone.ore.ResourceMapFactory;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.OREException;
import org.dspace.foresite.ORESerialiserException;

import java.net.URISyntaxException;

public class ObjectManager implements Constants {
    
    private static final Logger log = Logger.getLogger(ObjectManager.class);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public static final int DEFAULT_START = 0;
    public static final int DEFAULT_COUNT = 20;

    protected Context myContext;
    protected String myData;
    protected String mySolrServer;
    
    public ObjectManager(Context aContext, String aCollection,
			 String aSolrServer) {
	myContext = aContext;
	myData = aCollection;
	mySolrServer = aSolrServer;
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
		int counter = 0;
		int count;
		int start;

		myContext.turnOffAuthorisationSystem();

		log.debug("Setting start parameter to: " + aStart);
		log.debug("Setting count parameter to: " + aCount);

		list.setStart(aStart);
		list.setCount(aCount);

		// We split our single records into two records for DataONE MN...
		if (aObjFormat == null) {
			start = startIsEven ? aStart / 2 : (aStart - 1) / 2;
			count = countIsEven ? (startIsEven ? aCount / 2 : (aCount + 2) / 2)
					: (startIsEven ? (aCount + 1) / 2 : (aCount + 3) / 2);
		}
		// unless we are filtering by objFormat which spits out one per item
		else {
			start = aStart;
			count = aCount * 2;
		}

		try {
			SolrServer server = new CommonsHttpSolrServer(mySolrServer);
			SolrQuery query = new SolrQuery();

			query.setQuery(buildQuery(aFrom, aTo, aObjFormat));
			query.setStart(new Integer(start));
			query.setRows(new Integer(count));

			QueryResponse solrResponse = server.query(query);
			SolrDocumentList docs = solrResponse.getResults();
			Iterator<SolrDocument> iterator = docs.iterator();
			int total = (int) (aObjFormat == null ? docs.getNumFound() * 2
					: docs.getNumFound()) - 1;

			if (log.isDebugEnabled()) {
				log.debug("Setting total parameter to: "
						+ Integer.toString(total));
			}

			list.setTotal(total >= 0 ? total : 0);
			serializer.writeStartTag(list);

			// Iterate through all the data files in the data file collection
			while (iterator.hasNext()) {
				SolrDocument doc = iterator.next();
				String doi = (String) doc.getFieldValue("doi");
				Integer id = (Integer) doc.getFieldValue("dsid");
				Date date = (Date) doc.getFieldValue("updated");
				String format = (String) doc.getFieldValue("format");

				log.debug("Building '" + doi + "' for mn list");

				// convert DOI to http form if necessary
				if (doi.startsWith("doi:")) {
				    doi = "http://dx.doi.org/" + doi.substring("doi:".length());
				    log.debug("converted DOI to http form. It is now " + doi);
				}
				
				ObjectInfo objInfo = new ObjectInfo(doi);
				Item item = Item.find(myContext, id.intValue());
				String lastMod = dateFormatter.format(date);

				objInfo.setObjectFormat(format);
				
				try {
				    String[] checksumDetails = getObjectChecksum(doi);
				    
				    if (checksumDetails != null && checksumDetails.length == 2) {
					objInfo.setXMLChecksum(checksumDetails[0],
							       checksumDetails[1]);
				    }
				    
				    objInfo.setXMLSize(getObjectSize(doi));
				}
				catch (NotFoundException e) {
				    log.error("Unable to calculate checksum for " + doi, e);
				    
				}
				objInfo.setLastModified(lastMod);

				
				Bundle[] bundles = item.getBundles("ORIGINAL");

				log.debug("Getting bitstreams for " + item.getHandle());
				
				if (bundles.length > 0) {
					for (Bitstream bitstream : bundles[0].getBitstreams()) {
						String name = bitstream.getName();

						log.debug("Checking '" + name + "' bitstream");
						
						if (!name.equalsIgnoreCase("readme.txt")
								&& !name.equalsIgnoreCase("readme")
								&& !name.equalsIgnoreCase("readme.txt.txt")) {
						    log.debug("Getting bitstream information from: "
										+ name);
						    
						    String algorithm = bitstream.getChecksumAlgorithm();
						    String checksum = bitstream.getChecksum();
						    
						    objInfo.setChecksum(algorithm, checksum);
						    objInfo.setSize(bitstream.getSize());
						}
					}
				}

				log.debug("Writing " + doi + " to XML");

				nu.xom.Element[] parts = objInfo.createInfoElements();
				
				if (counter != 0 || startIsEven) {
					if (aObjFormat == null
							|| aObjFormat.equals(DRYAD_NAMESPACE)) {
						serializer.write(parts[0]);
					}
				}

				if (startIsEven || (counter + 1 < count)) {
					if (countIsEven
							|| ((startIsEven && (counter + 1 < count)) || !startIsEven
									&& (counter + 2 < count))) {
						if (aObjFormat == null
								|| !aObjFormat.equals(DRYAD_NAMESPACE)) {
							serializer.write(parts[1]);
						}
					}
				}

				serializer.flush();
				counter += 1;
			}

			serializer.writeEndTag(list);
			serializer.flush();
			aOutStream.close();
		}
		catch (SolrServerException details) {
		    log.warn(details.getMessage(), details);
		    throw new RuntimeException(details);
		}
		catch (MalformedURLException details) {
		    log.warn(details.getMessage(), details);
		    throw new RuntimeException(details);
		}

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

	private String buildQuery(Date aFrom, Date aTo, String aObjFormat) {
		StringBuilder query = new StringBuilder();
		String qString;

		if (aObjFormat == null && aFrom == null && aTo == null) {
			query.append("doi:[* TO *]");
		}
		else {
			SimpleDateFormat date = new SimpleDateFormat(
					"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

			if (aObjFormat != null) {
				if (aObjFormat.equals(DRYAD_NAMESPACE)) {
					query.append("doi:[* TO *]");
				}
				else {
					query.append("format:\"").append(aObjFormat).append("\"");
				}
			}

			if (aFrom != null && aTo != null) {
				String from = date.format(aFrom);
				String to = date.format(aTo);
				query.append(" updated:[" + from + " TO " + to + "]");
			}
			else if (aFrom == null && aTo != null) {
				query.append(" updated:[* TO " + date.format(aTo));
			}
			else if (aFrom != null && aTo == null) {
				query.append(" updated:[" + date.format(aFrom) + " TO NOW]");
			}
		}

		qString = query.toString().trim();

		log.debug("Solr query: " + qString);
	
		return qString;
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
	    if(aID.endsWith("/d1rem")) {
		int bitsIndex = aID.indexOf("/d1rem");
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

    /**
     * Write an ORE resource map to the output stream.
     **/
    public void getResourceMap(String aID, OutputStream aOutputStream)
	throws IOException, SQLException, NotFoundException {
	
	log.debug("Retrieving resource map for " + aID);
	
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
			doi = vals[i].value;
			break;
		    }
		}
	    }

	    // DataFiles
	    DCValue[] dataFiles = item.getMetadata("dc.relation.haspart");

	    ////////// generate a resource map
	    
	    // the ORE object's id
	    Identifier resourceMapId = new Identifier();
	    resourceMapId.setValue(doi + "/rem");
	    // the science metadata id
	    Identifier dataPackageId = new Identifier();
	    dataPackageId.setValue(doi);
	    // data file identifiers
	    List<Identifier> dataIds = new ArrayList<Identifier>();
	    for(int i=0; i < dataFiles.length; i++) {
		String dataIdString = dataFiles[i].value;
		Identifier dataFileId = new Identifier();
		dataFileId.setValue(dataIdString);
		dataIds.add(dataFileId);
		Identifier dataFileBitstreamId = new Identifier();
		dataFileBitstreamId.setValue(dataIdString + "/bitstream");
		dataIds.add(dataFileBitstreamId);
	    }
	    
	    // associate the metadata and data identifiers
	    Map<Identifier, List<Identifier>> idMap = new HashMap<Identifier, List<Identifier>>();
	    idMap.put(dataPackageId, dataIds);
	    // generate the resource map
	    ResourceMapFactory rmf = ResourceMapFactory.getInstance();
	    ResourceMap resourceMap = rmf.createResourceMap(resourceMapId, idMap);

	    // serialize it as RDF/XML
	    String rdfXml = ResourceMapFactory.getInstance().serializeResourceMap(resourceMap);

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
