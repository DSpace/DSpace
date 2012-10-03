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

	protected Item getDSpaceItem(String aDataOneDOI) throws NotFoundException {
		String[] parts = parseIDFormat(aDataOneDOI);
		return getDSpaceItem(parts[0], parts[1]);
	}

	protected Item getDSpaceItem(String aID, String aFormat)
			throws NotFoundException {
	    log.debug(aID + " requested in " + aFormat + " format");
		
	    DOIIdentifierProvider doiService = new DSpace().getSingletonService(DOIIdentifierProvider.class);
	    Item item = null;
	    try {
		item = (Item) doiService.resolve(myContext, aID, new String[] {});
	    } catch (IdentifierNotFoundException e) {
		log.error("Unable to locate item " + aID, e);
		throw new NotFoundException(aID, e);
	    } catch (IdentifierNotResolvableException e) {
		log.error("Unable to locate item " + aID, e);
		throw new NotFoundException(aID, e);
	    }
	    
	    if (item == null) {
		log.error("Unable to locate item " + aID);
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
		Item item = getDSpaceItem(aID, aFormat);
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
	 * Retrieve an object identified by guid from the node. The response will
	 * contain the bytes of the indicated object (note that the indicated object
	 * may be metadata). If the object does not exist on the node servicing the
	 * request, Exceptions.NotFound will be raised even if the object exists on
	 * another node in the DataONE system.
	 **/
	public void getObject(String aID, String aFormat, OutputStream aOutputStream)
			throws IOException, SQLException, NotFoundException {
		try {
			DOIIdentifierProvider doiService = new DSpace().getSingletonService(DOIIdentifierProvider.class);
            Item item = null;
            try {
                item = (Item) doiService.resolve(myContext, aID, new String[] {});
            } catch (IdentifierNotFoundException e) {
                throw new NotFoundException(aID);
            } catch (IdentifierNotResolvableException e) {
                throw new NotFoundException(aID);
            }
            Format ppFormat = Format.getPrettyFormat();

			if (aFormat.equals("dap")) {
			    log.debug("Retrieving metadata for " + aID
				      + " (DSO_ID: " + item.getID() + ") -- "
				      + item.getHandle());
			    

				DisseminationCrosswalk xWalk = (DisseminationCrosswalk) PluginManager
						.getNamedPlugin(DisseminationCrosswalk.class,
								DRYAD_CROSSWALK);
				try {
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
					
					// add the MN identifier suffix for metadata records
					if (idElem != null) {
						idElem.setText(idElem.getText() + "/dap");
					}

					new XMLOutputter(ppFormat).output(result, aOutputStream);
					aOutputStream.close();
				}
				catch (AuthorizeException details) {
				    // We've disabled authorization for this context, so this should never happen
				    log.warn("Shouldn't see this exception!", details);
				}
				catch (CrosswalkException details) {
					log.error(details.getMessage(), details);

					// programming error
					throw new RuntimeException(details);
				}
			}
			else {
				Bundle[] bundles = item.getBundles("ORIGINAL");
				boolean found = false;

				if (bundles.length == 0) {
				    log.debug("Didn't find any original bundles for "
					      + item.getHandle());
				

					throw new NotFoundException(aFormat + "data bundle for "
							+ item.getHandle() + " not found");
				}

				log.debug("Retrieving scientific data for " + aID);
				
				for (Bitstream bitstream : bundles[0].getBitstreams()) {
					String name = bitstream.getName();

					if (!name.equalsIgnoreCase("readme.txt")
							&& !name.equalsIgnoreCase("readme.txt.txt")
							&& name.endsWith(aFormat)) {
						try {
						    log.debug("Retrieving bitstream " + name);
						    
							writeBitstream(bitstream.retrieve(), aOutputStream);
							found = true;
						}
						catch (AuthorizeException details) {
							// we've disabled authorization; everyone welcome!
						}
					}
				}

				if (!found) {
					throw new NotFoundException(aID + "/" + aFormat
							+ " wasn't found");
				}
			}
		}
		catch (MalformedURLException details) {
			// throw RuntimeException?
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
