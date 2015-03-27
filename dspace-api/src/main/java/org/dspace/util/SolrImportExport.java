/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.luke.FieldFlag;
import org.apache.solr.common.params.CoreAdminParams;
import org.dspace.core.ConfigurationManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to export, clear and import Solr indexes.
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ Institutional Research Repositories
 */
public class SolrImportExport
{

	public static final String HELP_OPTION = "h";
	public static final String INDEX_NAME_OPTION = "i";
	public static final String ACTION_OPTION = "a";
	public static final String DIRECTORY_OPTION = "d";
	private static final String CLEAR_OPTION = "c";

	public static final int ROWS_PER_FILE = 10_000;

	private static final Logger log = Logger.getLogger(SolrImportExport.class);

	/**
	 * Entry point for command-line invocation
	 * @param args command-line arguments; see help for description
	 * @throws ParseException if the command-line arguments cannot be parsed
	 */
	public static void main(String[] args) throws ParseException
	{
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption(HELP_OPTION, "help", false, "Get help on options for this command.");
		options.addOption(INDEX_NAME_OPTION, "index-name", true,
				                 "The names of the indexes to process. At least one is required. Available indexes are: authority, statistics.");
		options.addOption(ACTION_OPTION, "action", true,
				                 "The action to perform: import or export. Default: export.");
		options.addOption(CLEAR_OPTION, "clear", false, "When importing, also clear the index first. Ignored when action is export.");
		options.addOption(DIRECTORY_OPTION, "directory", true,
				                 "The absolute path for the directory to use for import or export. If none is given, [dspace]/solr-export is used.");

		try
		{
			CommandLine line = parser.parse(options, args);
			if (line.hasOption(HELP_OPTION))
			{
				printHelpAndExit(options, 0);
			}

			if (!line.hasOption(INDEX_NAME_OPTION))
			{
				System.err.println("This command requires the index-name option but none was present.");
				printHelpAndExit(options, 1);
			}
			String[] indexNames = line.getOptionValues(INDEX_NAME_OPTION);

			String action = line.getOptionValue(ACTION_OPTION, "export");
			if ("import".equals(action))
			{
				for (String indexName : indexNames)
				{
					try
					{
						String fromDir = makeDirectoryName(line.getOptionValue(DIRECTORY_OPTION));
						String solrUrl = makeSolrUrl(indexName);
						importIndex(indexName, fromDir, line.hasOption(CLEAR_OPTION), solrUrl);
					}
					catch (IOException | SolrServerException | SolrImportExportException e)
					{
						System.err.println("Problem encountered while trying to import index " + indexName + ".");
						e.printStackTrace(System.err);
					}
				}
			}
			else if ("export".equals(action))
			{
				for (String indexName : indexNames)
				{
					try
					{
						String toDir = makeDirectoryName(line.getOptionValue(DIRECTORY_OPTION));
						String solrUrl = makeSolrUrl(indexName);
						String timeField = makeTimeField(indexName);
						exportIndex(indexName, toDir, solrUrl, timeField);
					}
					catch (SolrServerException | IOException | SolrImportExportException e)
					{
						System.err.println("Problem encountered while trying to export index " + indexName + ".");
						e.printStackTrace(System.err);
					}
				}
			}
			else if ("reindex".equals(action))
			{
				for (String indexName : indexNames)
				{
					String tempIndexName = indexName + "-temp";

					String origSolrUrl = makeSolrUrl(indexName);
					String baseSolrUrl = StringUtils.substringBeforeLast(origSolrUrl, "/"); // need to get non-core solr URL
					String tempSolrUrl = baseSolrUrl + "/" + tempIndexName;

					String solrInstanceDir = ConfigurationManager.getProperty("dspace.dir") + File.separator + "solr" + File.separator + indexName;

					String exportDir = makeDirectoryName(line.getOptionValue(DIRECTORY_OPTION)); // TODO optionally, keep the export?
					String timeField = makeTimeField(indexName);

					// Create a temp directory to store temporary core data
					File tempDataDir = new File(ConfigurationManager.getProperty("dspace.dir") + File.separator + "temp" + File.separator + "solr-data");
					tempDataDir.mkdirs();

					// create a temporary core to hold documents coming in during the reindex
					try {
						HttpSolrServer adminSolr = new HttpSolrServer(baseSolrUrl);
						CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
						createRequest.setInstanceDir(solrInstanceDir);
						createRequest.setDataDir(tempDataDir.getCanonicalPath());
						createRequest.setCoreName(tempIndexName);
						createRequest.process(adminSolr);

						// swap actual core with temporary one
						CoreAdminRequest swapRequest = new CoreAdminRequest();
						swapRequest.setCoreName(indexName);
						swapRequest.setOtherCoreName(tempIndexName);
						swapRequest.setAction(CoreAdminParams.CoreAdminAction.SWAP);
						swapRequest.process(adminSolr);

						// export from the actual core (from temp core name, actual data dir)
						exportIndex(indexName, exportDir, tempSolrUrl, timeField);

						// clear actual core (temp core name, clearing actual data dir) & import
						importIndex(indexName, exportDir, true, tempSolrUrl);

						// commit changes
						HttpSolrServer origSolr = new HttpSolrServer(origSolrUrl);
						origSolr.commit();

						// swap back (statistics now going to actual core name in actual data dir)
						swapRequest = new CoreAdminRequest();
						swapRequest.setCoreName(tempIndexName);
						swapRequest.setOtherCoreName(indexName);
						swapRequest.setAction(CoreAdminParams.CoreAdminAction.SWAP);
						swapRequest.process(adminSolr);

						// export all docs from now-temp core...
						exportIndex(tempIndexName, exportDir, tempSolrUrl, timeField);
						// ...and import them into the now-again-actual core *without* clearing
						importIndex(tempIndexName, exportDir, false, origSolrUrl);

						// commit changes
						origSolr.commit();

						// unload now-temp core (temp core name)
						CoreAdminRequest.unloadCore(tempIndexName, true, true, adminSolr);

						// clean up
						FileUtils.deleteDirectory(tempDataDir);
					} catch (SolrServerException | IOException | SolrImportExportException e) {
						System.err.println("Problem encountered while trying to reimport index " + indexName + ".");
						e.printStackTrace(System.err);
					}
					// TODO increase resiliency / improve error handling
				}
			}
			else
			{
				System.err.println("Unknown action " + action + "; must be import, export or clear.");
				printHelpAndExit(options, 1);
			}
		}
		catch (ParseException e)
		{
			System.err.println("Cannot read command options");
			printHelpAndExit(options, 1);
		}
	}

	/**
	 *
	 * @param indexName the index to import.
	 * @param fromDir the source directory.
	 *                   The importer will look for files whose name starts with <pre>indexName</pre>
	 *                   and ends with .csv (to match what is generated by #makeExportFilename).
	 * @param clear if true, clear the index before importing.
	 * @param solrUrl The solr URL for the index to export. Must not be null.
	 * @throws IOException if there is a problem reading the files or communicating with Solr.
	 * @throws SolrServerException if there is a problem reading the files or communicating with Solr.
	 * @throws SolrImportExportException if there is a problem communicating with Solr.
	 */
	public static void importIndex(final String indexName, String fromDir, boolean clear, String solrUrl) throws IOException, SolrServerException, SolrImportExportException {
		if (StringUtils.isBlank(solrUrl))
		{
			throw new SolrImportExportException("Could not construct solr URL for index" + indexName + ", aborting export.");
		}

		HttpSolrServer solr = new HttpSolrServer(solrUrl);

		// must get multivalue fields before clearing
		List<String> multivaluedFields = getMultiValuedFields(solr);

		if (clear)
		{
			clearIndex(solrUrl);
		}

		File sourceDir = new File(fromDir);
		File[] files = sourceDir.listFiles(new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				return name.startsWith(indexName) && name.endsWith(".csv");
			}
		});

		if (files == null || files.length == 0)
		{
			log.warn("No export files found in directory " + fromDir + " for index " + indexName);
			return;
		}

		for (File file : files)
		{
			log.info("Importing file " + file.getCanonicalPath());
			ContentStreamUpdateRequest contentStreamUpdateRequest = new ContentStreamUpdateRequest("/update/csv");
			if (clear)
			{
				contentStreamUpdateRequest.setParam("skip", "_version_");
			}
			for (String mvField : multivaluedFields) {
				contentStreamUpdateRequest.setParam("f." + mvField + ".split", "true");
				contentStreamUpdateRequest.setParam("f." + mvField + ".escape", "\\");
			}
			contentStreamUpdateRequest.setParam("stream.contentType", "text/csv;charset=utf-8");
			contentStreamUpdateRequest.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
			contentStreamUpdateRequest.addFile(file, "text/csv;charset=utf-8");

			solr.request(contentStreamUpdateRequest);
		}

		solr.commit(true, true);
	}

	/**
	 * Determine the names of all multi-valued fields from the data in the index.
	 * @param solr the solr server to query.
	 * @return A list containing all multi-valued fields, or an empty list if none are found / there aren't any.
	 */
	private static List<String> getMultiValuedFields(HttpSolrServer solr)
	{
		List<String> result = new ArrayList<>();
		try
		{
			LukeRequest request = new LukeRequest();
			// this needs to be a non-schema request, otherwise we'll miss dynamic fields
			LukeResponse response = request.process(solr);
			Map<String, LukeResponse.FieldInfo> fields = response.getFieldInfo();
			for (LukeResponse.FieldInfo info : fields.values())
			{
				if (info.getSchema().contains(FieldFlag.MULTI_VALUED.getAbbreviation() + ""))
				{
					result.add(info.getName());
				}
			}
		}
		catch (IOException | SolrServerException e)
		{
			log.fatal("Cannot determine which fields are multi valued: " + e.getMessage(), e);
		}
		return result;
	}

	/**
	 * Remove all documents from the Solr index with the given URL, then commit and optimise the index.
	 *
	 * @throws IOException if there is a problem in communicating with Solr.
	 * @throws SolrServerException if there is a problem in communicating with Solr.
	 * @param solrUrl URL of the Solr core to clear.
	 */
	public static void clearIndex(String solrUrl) throws IOException, SolrServerException
	{
		HttpSolrServer solr = new HttpSolrServer(solrUrl);
		solr.deleteByQuery("*:*");
		solr.commit();
		solr.optimize();
	}

	/**
	 * Exports all documents in the given index to the specified target directory in batches of #ROWS_PER_FILE.
	 * See #makeExportFilename for the file names that are generated.
	 *
	 * @param indexName The index to export.
	 * @param toDir The target directory for the export. Will be created if it doesn't exist yet. The directory must be writeable.
	 * @param solrUrl The solr URL for the index to export. Must not be null.
	 * @param timeField The time field to use for sorting the export. Must not be null.
	 * @throws SolrServerException if there is a problem with exporting the index.
	 * @throws IOException if there is a problem creating the files or communicating with Solr.
	 * @throws SolrImportExportException if there is a problem in communicating with Solr.
	 */
	public static void exportIndex(String indexName, String toDir, String solrUrl, String timeField) throws SolrServerException, IOException, SolrImportExportException
	{
		if (StringUtils.isBlank(solrUrl))
		{
			throw new SolrImportExportException("Could not construct solr URL for index" + indexName + ", aborting export.");
		}

		File targetDir = new File(toDir);
		if (!targetDir.exists())
		{
			//noinspection ResultOfMethodCallIgnored
			targetDir.mkdirs();
		}
		if (!targetDir.exists())
		{
			throw new SolrImportExportException("Could not create target directory " + toDir + ", aborting export of index ");
		}

		SolrQuery query = new SolrQuery("*:*");

		HttpSolrServer solr = new HttpSolrServer(solrUrl);
		SolrDocumentList results = solr.query(query).getResults();
		long totalRecords = results.getNumFound();

		query.setRows(ROWS_PER_FILE);
		query.set("wt", "csv");
		query.set("fl", "*");
		query.setSort(timeField, SolrQuery.ORDER.asc);
		// TODO introduce offset
		for (int i = 0; i < totalRecords; i+= ROWS_PER_FILE) {
			query.setStart(i);
			URL url = new URL(solrUrl + "/select?" + query.toString());

			File file = new File(targetDir.getCanonicalPath(), makeExportFilename(indexName, totalRecords, i));
			if (file.createNewFile())
			{
				FileUtils.copyURLToFile(url, file);
				log.info("Exported batch " + i + " to " + file.getCanonicalPath());
			}
			else
			{
				log.error("Could not create file " + file.getCanonicalPath() + " while exporting index " + indexName + ", batch " + i);
			}
		}
	}

	/**
	 * Return the specified directory name or fall back to a default value.
	 *
	 * @param directoryValue a specific directory name. Optional.
	 * @return directoryValue if given as a non-blank string. A default directory otherwise.
	 */
	private static String makeDirectoryName(String directoryValue)
	{
		if (StringUtils.isNotBlank(directoryValue))
		{
			return directoryValue;
		}
		return ConfigurationManager.getProperty("dspace.dir") + File.separator + "solr-export" + File.separator;
	}

	private static String makeExportFilename(String indexName, long totalRecords, int index)
	{
        String exportFileNumber = "";
		if (totalRecords > ROWS_PER_FILE) {
			exportFileNumber = StringUtils.leftPad("" + (index / ROWS_PER_FILE), (int) Math.ceil(Math.log10(totalRecords / ROWS_PER_FILE)), "0");
		}
		return indexName
				       + "_export_"
				       + exportFileNumber
				       + ".csv";
	}

	/**
	 * Returns the full URL for the specified index name.
	 *
	 * @param indexName the index name whose Solr URL is required. If the index name starts with
	 *                     &quot;statistics&quot; or is &quot;authority&quot;, the Solr base URL will be looked up
	 *                     in the corresponding DSpace configuration file. Otherwise, it will fall back to a default.
	 * @return the full URL to the Solr index, as a String.
	 */
	private static String makeSolrUrl(String indexName)
	{
		if (indexName.startsWith("statistics"))
		{
			// TODO account for year shards properly?
			return ConfigurationManager.getProperty("solr-statistics", "server") + indexName.replaceFirst("statistics", "");
		}
		else if ("authority".equals(indexName))
		{
			return ConfigurationManager.getProperty("solr.authority.server");
		}
		return "http://localhost:8080/solr/" + indexName; // TODO better default?
	}

	/**
	 * Returns a time field for the specified index name that is suitable for incremental export.
	 *
	 * @param indexName the index name whose Solr URL is required.
	 * @return the name of the time field, or null if no suitable field can be determined.
	 */
	private static String makeTimeField(String indexName)
	{
		if (indexName.startsWith("statistics"))
		{
			return "time";
		}
		else if ("authority".equals(indexName))
		{
			return "last_modified_date";
		}
		return null; // TODO some sort of default?
	}

	/**
	 * A utility method to print out all available command-line options and exit given the specified code.
	 *
	 * @param options the supported options.
	 * @param exitCode the exit code to use. The method will call System#exit(int) with the given code.
	 */
	private static void printHelpAndExit(Options options, int exitCode)
	{
		HelpFormatter myhelp = new HelpFormatter();
		myhelp.printHelp(SolrImportExport.class.getSimpleName() + "\n", options);
		System.exit(exitCode);
	}
}
