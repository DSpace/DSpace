/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.app;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.xoai.XOAIDatabaseManager;
import org.dspace.xoai.solr.DSpaceSolrSearch;
import org.dspace.xoai.solr.DSpaceSolrServer;
import org.dspace.xoai.solr.exceptions.DSpaceSolrException;
import org.dspace.xoai.solr.exceptions.DSpaceSolrIndexerException;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
@SuppressWarnings("deprecation")
public class SolrIndexer {
	private static Logger log = LogManager.getLogger(SolrIndexer.class);
	private Context _context;
	private boolean _optimize;
	private boolean _verbose;

	private static List<String> getFileFormats(Item item) {
		List<String> formats = new ArrayList<String>();
		try {
			for (Bundle b : item.getBundles("ORIGINAL")) {
				for (Bitstream bs : b.getBitstreams()) {
					if (!formats.contains(bs.getFormat().getMIMEType())) {
						formats.add(bs.getFormat().getMIMEType());
					}
				}
			}
		} catch (SQLException ex) {
			log.error(ex.getMessage(), ex);
		}
		return formats;
	}

	public SolrIndexer(Context ctx, boolean optimize, boolean verb) {
		_context = ctx;
		_optimize = optimize;
		_verbose = verb;
	}

	private void println(String line) {
		System.out.println(line);
	}

	public void index() throws DSpaceSolrIndexerException {
		try {
			SolrQuery solrParams = new SolrQuery("*:*")
					.addField("item.lastmodified")
					.addSortField("item.lastmodified", ORDER.desc).setRows(1);

			SolrDocumentList results = DSpaceSolrSearch.query(solrParams);
			if (results.getNumFound() == 0) {
				System.out
						.println("There are no indexed documents, using full import.");
				this.indexAll();
			} else
				this.index((Date) results.get(0).getFieldValue(
						"item.lastmodified"));
			if (_optimize) {
				println("Optimizing Index");
				DSpaceSolrServer.getServer().optimize();
				println("Index optimized");
			}
			cleanCache();
		} catch (DSpaceSolrException ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		} catch (SolrServerException ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		}
	}

	private void index(Date last) throws DSpaceSolrIndexerException {
		System.out
				.println("Incremental import of information. Searching for documents modified after: "
						+ last.toString());
		try {
			TableRowIterator iterator = DatabaseManager
					.query(_context,
							"SELECT item_id FROM item WHERE in_archive=TRUE AND last_modified > ?",
							new java.sql.Date(last.getTime()));
			this.index(iterator);
		} catch (SQLException ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		}
	}

	private void indexAll() throws DSpaceSolrIndexerException {
		System.out.println("Full import of information.");
		try {
			TableRowIterator iterator = DatabaseManager.query(_context,
					"SELECT item_id FROM item WHERE in_archive=TRUE");
			this.index(iterator);
		} catch (SQLException ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		}
	}

	private void index(TableRowIterator iterator)
			throws DSpaceSolrIndexerException {
		try {
			List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
			while (iterator.hasNext()) {
				try {
					docs.add(this.index(Item.find(_context, iterator.next()
							.getIntColumn("item_id"))));
					_context.clearCache();
				} catch (SQLException ex) {
					log.error(ex.getMessage(), ex);
				}
			}
			SolrServer server = DSpaceSolrServer.getServer();
			server.add(docs);
			server.commit();
		} catch (SQLException ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		} catch (SolrServerException ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		}
	}

	private SolrInputDocument index(Item item) throws SQLException {
		SolrInputDocument doc = new SolrInputDocument();
		doc.addField("item.id", item.getID());
		boolean pub = this.isPublic(item);
		doc.addField("item.public", pub);
		String handle = item.getHandle();
		doc.addField("item.handle", handle);
		doc.addField("item.lastmodified", item.getLastModified());
		doc.addField("item.submitter", item.getSubmitter().getEmail());
		doc.addField("item.deleted", item.isWithdrawn() ? "true" : "false");
		for (Collection col : item.getCollections())
			doc.addField("item.collections",
					"col_" + col.getHandle().replace("/", "_"));
		for (Community com : XOAIDatabaseManager.flatParentCommunities(item))
			doc.addField("item.communities",
					"com_" + com.getHandle().replace("/", "_"));

		DCValue[] allData = item.getMetadata(Item.ANY, Item.ANY, Item.ANY,
				Item.ANY);
		for (DCValue dc : allData) {
			String key = "metadata." + dc.schema + "." + dc.element;
			if (dc.qualifier != null) {
				key += "." + dc.qualifier;
			}
			doc.addField(key, dc.value);
		}

		for (String f : getFileFormats(item)) {
			doc.addField("metadata.dc.format.mimetype", f);
		}

		if (_verbose) {
			println("Item with Handle " + handle);
		}
		return doc;
	}

	private boolean isPublic(Item item) {
		try {
			AuthorizeManager.authorizeAction(_context, item, Constants.READ);
			for (Bundle b : item.getBundles())
				AuthorizeManager.authorizeAction(_context, b, Constants.READ);
			return true;
		} catch (AuthorizeException ex) {
			log.debug(ex.getMessage());
		} catch (SQLException ex) {
			log.error(ex.getMessage());
		}
		return false;
	}

	public static void main(String[] argv) {
		try {
			CommandLineParser parser = new PosixParser();
			Options options = new Options();
			options.addOption("i", "index", false, "Index database items");
			options.addOption("c", "clear", false, "Clear index");
			options.addOption("o", "optimize", false,
					"Optimize index at the end of the operation");
			options.addOption("v", "verbose", false, "Verbose output");
			options.addOption("p", "purge-cache", false,
					"Purges cached OAI responses");
			options.addOption("h", "help", false, "Shows some help");
			CommandLine line = parser.parse(options, argv);

			if (line.hasOption('h')) {
				usage();
			} else {
				System.out.println("XOAI import app started");
				long start = System.currentTimeMillis();

				if (line.hasOption('c'))
					clearIndex();

				if (line.hasOption('p'))
					cleanCache();

				if (line.hasOption('i')) {
					Context ctx = new Context();
					SolrIndexer indexer = new SolrIndexer(ctx,
							line.hasOption('o'), line.hasOption('v'));

					indexer.index();
				}

				System.out.println("XOAI import ended. It took "
						+ ((System.currentTimeMillis() - start) / 1000)
						+ " seconds.");
			}
		} catch (Throwable ex) {
			if (!searchForReason(ex)) {
				ex.printStackTrace();
			}
			log.error(ex.getMessage(), ex);
		}
	}

	private static boolean getKnownExplanation(Throwable t) {
		if (t instanceof ConnectException) {
			System.err.println("Solr server ("
					+ ConfigurationManager.getProperty("xoai","solr.url")
					+ ") is down, turn it on.");
			return true;
		}

		return false;
	}

	private static boolean searchForReason(Throwable t) {
		if (getKnownExplanation(t))
			return true;
		if (t.getCause() != null)
			return searchForReason(t.getCause());
		return false;
	}

	private static void clearIndex() throws DSpaceSolrIndexerException {
		try {
			cleanCache();
			System.out.println("Clearing index");
			DSpaceSolrServer.getServer().deleteByQuery("*.*");
			System.out.println("Index cleared");
		} catch (SolrServerException ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new DSpaceSolrIndexerException(ex.getMessage(), ex);
		}
	}

	private static void cleanCache() {
		System.out.println("Purging cached OAI responses.");
		String dir = ConfigurationManager.getProperty("dspace.dir");
		if (!dir.endsWith("/"))
			dir += "/";
		dir += "var/xoai";

		File directory = new File(dir);
		if (directory.exists()) {
			// log.info("Directory "+dir+" exists");
			// Get all files in directory
			File[] files = directory.listFiles();
			for (File file : files) {
				// Delete each file

				if (!file.delete()) {
					// Failed to delete file
					System.out.println("Failed to delete cache file: " + file);
				}
			}
		} else
			System.out.println("Directory " + dir
					+ " doesn't exist, must run ant init_configs");
	}

	private static void usage() {
		System.out.println("Parameters:");
		System.out.println("    -i Index database items");
		System.out.println("    -o Optimize index after operation");
		System.out.println("    -c Clear index");
		System.out.println("    -v Verbose output");
		System.out.println("    -p Purges cached OAI responses");
		System.out.println("    -h Shows this text");
	}
}
