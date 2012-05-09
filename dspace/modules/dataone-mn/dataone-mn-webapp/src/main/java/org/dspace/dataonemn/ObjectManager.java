package org.dspace.dataonemn;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

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

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.DateFormat;

public class ObjectManager extends AbstractObjectManager {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(ObjectManager.class);

	public static final int DEFAULT_START = 0;
	public static final int DEFAULT_COUNT = 20;

	public ObjectManager(Context aContext, String aCollection,
			String aSolrServer) {
		super(aContext, aCollection, aSolrServer);
	}

	public void printList(OutputStream aOutStream) throws SQLException,
			IOException {
		printList(DEFAULT_START, DEFAULT_COUNT, aOutStream);
	}

	public void printList(Date aFrom, Date aTo, OutputStream aOutStream)
			throws SQLException, IOException {
		printList(DEFAULT_START, DEFAULT_COUNT, aFrom, aTo, null, aOutStream);
	}

	public void printList(Date aFrom, Date aTo, String aObjFormat,
			OutputStream aOutStream) throws SQLException, IOException {
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

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Setting start parameter to: " + aStart);
			LOGGER.debug("Setting count parameter to: " + aCount);
		}

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

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Setting total parameter to: "
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
				String ext = (String) doc.getFieldValue("ext");

				LOGGER.debug("Retrieving '" + doi + "' for mn list");
		
				ObjectInfo objInfo = new ObjectInfo(doi);
				Item item = Item.find(myContext, id.intValue());
				String lastMod = DateFormat.getDateTimeInstance().format(date);

				objInfo.setLastModified(lastMod);
				objInfo.setObjectFormat(format);
				objInfo.setFormatExtension(ext);

				try {
					String[] checksumDetails = getObjectChecksum(doi, "dap");

					if (checksumDetails != null && checksumDetails.length == 2) {
						objInfo.setXMLChecksum(checksumDetails[0],
								checksumDetails[1]);
					}
				}
				catch (NotFoundException details) {
				    LOGGER.error("Should not happen: "
						 + details.getMessage());
				    
				}

				Bundle[] bundles = item.getBundles("ORIGINAL");

				LOGGER.debug("Getting bitstreams for " + item.getHandle());
				
				if (bundles.length > 0) {
					for (Bitstream bitstream : bundles[0].getBitstreams()) {
						String name = bitstream.getName();

						LOGGER.debug("Checking '" + name + "' bitstream");
						
						if (!name.equalsIgnoreCase("readme.txt")
								&& !name.equalsIgnoreCase("readme")
								&& !name.equalsIgnoreCase("readme.txt.txt")) {
						    LOGGER.debug("Getting bitstream information from: "
										+ name);
						    
						    String algorithm = bitstream.getChecksumAlgorithm();
						    String checksum = bitstream.getChecksum();
						    
						    objInfo.setChecksum(algorithm, checksum);
						    objInfo.setSize(bitstream.getSize());
						}
					}
				}

				LOGGER.debug("Writing " + doi + " to XML");

				nu.xom.Element[] parts = objInfo.split();

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
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn(details.getMessage(), details);
			}

			throw new RuntimeException(details);
		}
		catch (MalformedURLException details) {
			if (LOGGER.isWarnEnabled()) {
				LOGGER.warn(details.getMessage(), details);
			}

			throw new RuntimeException(details);
		}

		myContext.restoreAuthSystemState();
	}

	public long getObjectSize(String aID, String aFormat)
			throws NotFoundException, IOException, SQLException {
		Item item = getDSpaceItem(aID, aFormat);

		if (!aFormat.equals("dap")) {
			return getOrigBitstream(item, aFormat).getSize();
		}
		else {
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

				return (long) writer.toString().length();
			}
			catch (AuthorizeException details) {
				if (LOGGER.isWarnEnabled()) {
					LOGGER.warn("Shouldn't see this exception!");
				}

				throw new RuntimeException(details);
			}
			catch (CrosswalkException details) {
				LOGGER.error(details.getMessage(), details);

				throw new RuntimeException(details);
			}
		}
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

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Solr query: " + qString);
		}

		return qString;
	}
}
