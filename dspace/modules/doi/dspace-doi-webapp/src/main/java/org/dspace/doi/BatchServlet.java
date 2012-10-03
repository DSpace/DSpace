package org.dspace.doi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
@Deprecated
public class BatchServlet extends HttpServlet {

	private static final long serialVersionUID = -5339147244488049030L;

	private static final Logger LOGGER = LoggerFactory
			.getLogger(BatchServlet.class);

	private String myHostname;

	private String myHostPort;

	private Minter myMinter;

	private String mySolr;

	private String myData;
	
	private String myDump;

	@Override
	protected void doGet(HttpServletRequest aRequest,
			HttpServletResponse aResponse) throws ServletException, IOException {
		aResponse.setContentType("text/html; charset=UTF-8");
		
		PrintWriter toBrowser = aResponse.getWriter();
		ItemIterator iterator = null;
		Context context = null;
		Collection collection;
		int counter = 0;

		try {
			context = new Context();
			collection = (Collection) HandleManager.resolveToObject(context,
					myData);
			iterator = collection.getAllItems();

			context.turnOffAuthorisationSystem();

			toBrowser.print("<div style=\"margin-bottom: 5px\">");
			toBrowser.print(collection.countItems());
			toBrowser.print("</div>");

			while (iterator.hasNext()) {
				Item item = iterator.next();

				// Skip withdrawn items
				if (item.isWithdrawn()) {
					continue;
				}

				DCValue[] id = item.getMetadata("dc.identifier");

				toBrowser.print("<div>");

				if (id.length == 1) {
					String idValue = id[0].value;

					if (idValue == null) {
						toBrowser.print("DSpace item: " + item.getID()
								+ " (null value)");
					}
					else {
						toBrowser.print(idValue);

						if (!isFileDOI(idValue)) {
							toBrowser.print(" (not file DOI)");
						}
						else {
							toBrowser.print(" - " + index(item, idValue));

							// TODO: Update "isPartOf" and "hasPart" rels
						}
					}

					counter += 1;
				}                
//				else if (id.length == 0) {
//					try {
//
//                        DOI doi = myMinter.calculateDOI(item.getHandle());
//
//						if (doi == null || doi.toString().equals("null")) {
//							toBrowser.print("DSpace item: " + item.getID()
//									+ " (null from minter)");
//						}
//						else {
//							toBrowser.print(doi.toString());
//							toBrowser.print(" (from minter) - ");
//							toBrowser.print(index(item, doi.toString()));
//						}
//					}
//					catch (RuntimeException details) {
//						toBrowser.print("DSpace item: " + item.getID()
//								+ " (problem with " + item.getHandle() + ") - "
//								+ details.getMessage());
//					}
//				}
				else if (id.length > 1) {
					toBrowser.print(" <span>");
					toBrowser.print(id.length);
					toBrowser.print("</span>");
				}

				toBrowser.print("</div>");
				toBrowser.flush();
			}
		}
		catch (SQLException details) {
			throw new ServletException(details);
		}
		catch (MalformedURLException details) {
			throw new ServletException(details); // config error
		}
		catch (IOException details) {
			throw new ServletException(details);
		}
		catch (SolrServerException details) {
			throw new ServletException(details);
		}
		finally {
			if (iterator != null) {
				iterator.close();
			}

			if (context != null) {
				context.restoreAuthSystemState();

				try {
					context.complete();
				}
				catch (SQLException details) {
					// at this point, just ignore...
				}
			}

			toBrowser.print("<div style=\"margin: 5px\"/>");
			toBrowser.printf("<div>Total: " + counter + "</div>");
			toBrowser.close();
		}
		
		File dumpFile = new File(myDump);
		FileOutputStream dumpStream = new FileOutputStream(dumpFile);
		myMinter.dump(dumpStream);
	}

	/**
	 * Initializes the DSpace context, so we have access to the DSpace objects.
	 * Requires the location of the dspace.cfg file to be set in the web.xml.
	 **/
	public void init() throws ServletException {
		ServletContext context = this.getServletContext();
		String configFileName = context.getInitParameter("dspace.config");
		File aConfig = new File(configFileName);

		if (aConfig != null) {
			if (aConfig.exists() && aConfig.canRead() && aConfig.isFile()) {
				ConfigurationManager.loadConfig(aConfig.getAbsolutePath());

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("DSpace config loaded from " + aConfig);
				}
			}
			else if (!aConfig.exists()) {
				throw new RuntimeException(aConfig.getAbsolutePath()
						+ " doesn't exist");
			}
			else if (!aConfig.canRead()) {
				throw new RuntimeException("Can't read the dspace.cfg file");
			}
			else if (!aConfig.isFile()) {
				throw new RuntimeException("Err, dspace.cfg isn't a file?");
			}

			File configFile = new File(configFileName);
			myDump = configFile.getParent() + "/../doi-minter/dumpedDOIs.txt";
			
			// use dryad.url, constant, or dspace.url, relative to instance(?)
			myHostname = ConfigurationManager.getProperty("dspace.url");
			myHostPort = ConfigurationManager.getProperty("dspace.port");
			myMinter = new Minter(configFile);
		}

		myData = ConfigurationManager.getProperty("stats.datafiles.coll");
		mySolr = ConfigurationManager.getProperty("solr.dryad.server");

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Connected to Solr server: " + mySolr);
		}
	}

	private String index(Item aItem, String aDOI) throws MalformedURLException,
			IOException, SolrServerException {
		try {
			SolrServer server = new CommonsHttpSolrServer(mySolr);
			SolrInputDocument doc = new SolrInputDocument();

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Indexing " + aItem.getHandle() + " at " + mySolr);
			}

			// Use date.available because that means it is not embargoed
			DCValue[] date = aItem.getMetadata("dc.date.available");

			if (date.length == 0) {
				return "not indexed: no dc.date.available";
			}

			doc.addField("dsid", aItem.getID());
			doc.addField("doi", aDOI);
			doc.addField("url", buildURL(aItem.getHandle()));
			doc.addField("coll", myData);
			doc.addField("indexed", "NOW");
			doc.addField("updated", date[0].value);

			try {
				String[] values = getBitstreamFormat(aItem);

				doc.addField("ext", values[0]);
				doc.addField("format", values[1]);
			}
			catch (Exception details) {
				doc.addField("ext", "unknown");
				doc.addField("format", "unknown");
			}

			UpdateResponse response = server.add(doc);

			server.commit();

			return response.getStatus() == 0 ? "indexed" : "not indexed";
		}
		catch (Exception details) {
			return details.getMessage();
		}
	}

	private String buildURL(String aHandle) {
		// non-localhost instances should be proxied by apache at port 80
		return (myHostname.equals("http://localhost") ? myHostname + ":"
				+ myHostPort : myHostname)
				+ "/handle/" + aHandle;
	}

	private boolean isFileDOI(String aDOI) {
		int slashCount = 0;

		for (int index = 0; index < aDOI.length(); index++) {
			if (aDOI.charAt(index) == '/') {
				slashCount += 1;
			}
		}

		return slashCount > 1;
	}

	private String[] getBitstreamFormat(Item aItem) throws SQLException,
			RuntimeException {
		Bundle[] bundles = aItem.getBundles("ORIGINAL");

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Getting bitstreams for " + aItem.getHandle());
		}

		if (bundles.length > 0) {
			for (Bitstream bitstream : bundles[0].getBitstreams()) {
				String name = bitstream.getName();

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Checking '" + name + "' bitstream");
				}

				// The system has created this in various forms over time
				if (!name.equalsIgnoreCase("readme.txt")
						&& !name.equalsIgnoreCase("readme")
						&& !name.equalsIgnoreCase("readme.txt.txt")) {
					int slashIndex = name.lastIndexOf(".") + 1;
					String extension = "missing";

					if (slashIndex > 0) {
						extension = name.substring(slashIndex);
					}

					return new String[] { extension,
							bitstream.getFormat().getMIMEType() };
				}
			}
		}

		throw new RuntimeException("No bitstream for " + aItem.getHandle()
				+ " found");
	}
}
