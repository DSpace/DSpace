package org.dspace.dataonemn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

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

public abstract class AbstractObjectManager implements Constants {

    private static final Logger log = Logger.getLogger(AbstractObjectManager.class);
    
    protected Context myContext;
    protected String myData;
    protected String mySolrServer;
    
    protected AbstractObjectManager(Context aContext, String aCollection,
				    String aSolrServer) {
	myContext = aContext;
	myData = aCollection;
	mySolrServer = aSolrServer;
    }

    /**
       Retrieve a DSpace item by identifier.
    **/
    public Item getDSpaceItem(String aID) throws IOException, SQLException, NotFoundException {
	log.debug("Retrieving DSpace item " + aID);
	
	DOIIdentifierProvider doiService = new DSpace().getSingletonService(DOIIdentifierProvider.class);
	Item item = null;
	try {
	    item = (Item) doiService.resolve(myContext, aID, new String[] {});
	} catch (IdentifierNotFoundException e) {
	    throw new NotFoundException(aID);
	} catch (IdentifierNotResolvableException e) {
	    throw new NotFoundException(aID);
	}
	
	if(item == null) {
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
       Retrieve the first bitstream in a bundle of a particular format. The bitstream must be in a bundle
       marked "ORIGINAL". Bitstreams for "readme" files are ignored.
    **/
    protected Bitstream getOrigBitstream(Item aItem, String aFormat)
	throws SQLException, NotFoundException {
	Bundle[] bundles = aItem.getBundles("ORIGINAL");
	
	log.debug("Getting bitstreams for " + aItem.getHandle());
	
	if (bundles.length > 0) {
	    for (Bitstream bitstream : bundles[0].getBitstreams()) {
		String name = bitstream.getName();
		log.debug("Checking '" + name + "' bitstream");
		
		if (!name.equalsIgnoreCase("readme.txt")
		    && !name.equalsIgnoreCase("readme.txt.txt")
		    && (name.endsWith(aFormat) || aFormat.equals("*"))) {
		    log.debug("Getting bitstream info from: " + name);
		    return bitstream;
		}
	    }
	}
	
	throw new NotFoundException("No bitstream for " + aFormat + " found");
    }

    /**
       Retrieve the first bitstream in a bundle. The bitstream must be in a bundle
       marked "ORIGINAL". Bitstreams for "readme" files are ignored.
    **/
    public Bitstream getFirstBitstream(Item item) {
	Bundle[] bundles = item.getBundles("ORIGINAL");
	if (bundles.length == 0) {
	    log.error("Didn't find any original bundles for " + item.getHandle());
	    throw new NotFoundException("data bundle for " + item.getHandle() + " not found");
	}
	log.debug("This object has " + bundles.length + " bundles");
	
	Bitstream[] bitstreams = bundles[0].getBitstreams();
	boolean found = false;
	for(int i = 0; i < bitstreams.length && !found; i++) {
	    Bitstream bitstream = bitstreams[i];
	    String name = bitstream.getName();
	    
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
    }

    
    public String[] parseIDFormat(String aDataOneDOI) {
	int lastSlashIndex = aDataOneDOI.lastIndexOf("/");
	String format = aDataOneDOI.substring(lastSlashIndex + 1);
	String name = aDataOneDOI.substring(0, lastSlashIndex);
	
	return new String[] { name, format };
    }

	/**
	 * Returns an array with checksum and algorithm used.
	 * 
	 * @param aID The DOI of the object we want to retrieve
	 * @param aFormat The format of the DSpace item we want ("dap" or object)
	 * @return An array with checksum and algorithm used.
	 * @throws NotFoundException If the requested ID was not found
	 * @throws SQLException If there was trouble interacting with DSpace
	 * @throws IOException If there is trouble reading or writing data
	 */
	public String[] getObjectChecksum(String aID, String aFormat)
			throws NotFoundException, SQLException, IOException {
		Item item = getDSpaceItem(aID);
		String checksumAlgo;
		String checksum;

		if (!aFormat.equals("dap")) {
			Bitstream bitStream = getOrigBitstream(item, aFormat);
			checksum = bitStream.getChecksum();
			checksumAlgo = bitStream.getChecksumAlgorithm();
		}
		else {
			try {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				MessageDigest md = MessageDigest
						.getInstance(DEFAULT_CHECKSUM_ALGO);
				StringBuffer hexString = new StringBuffer();
				byte[] digest;

				getObject(aID, aFormat, outputStream);
				md.update(outputStream.toByteArray());
				checksumAlgo = DEFAULT_CHECKSUM_ALGO;
				digest = md.digest();

				for (int index = 0; index < digest.length; index++) {
					hexString.append(Integer.toHexString(0xFF & digest[index]));
				}

				checksum = hexString.toString();

				log.debug("Calculated XML checksum (" + checksum
					  + ") for " + aID);
			}
			catch (NoSuchAlgorithmException details) {
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
	    
	    DisseminationCrosswalk xWalk = (DisseminationCrosswalk) PluginManager
		.getNamedPlugin(DisseminationCrosswalk.class,
				    DRYAD_CROSSWALK);

	    if (!xWalk.canDisseminate(item)) {
		log.warn("xWalk says item cannot be disseminated: "
			 + item.getHandle());
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

    
    private void writeBitstream(InputStream aInputStream,
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
	}
	catch (SQLException e) {
	    log.error("unable to complete DSpace context", e);

	    // don't pass on the exception because this isn't an error in responding to a DataONE request,
	    // it's an internal error in shutting down resources.
	}
    }
    
}
