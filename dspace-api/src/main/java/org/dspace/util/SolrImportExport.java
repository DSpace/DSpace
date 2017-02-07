/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import org.apache.commons.cli.*;
import org.apache.commons.cli.ParseException;
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
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.luke.FieldFlag;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.FacetParams;
import org.dspace.core.ConfigurationManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.text.*;
import java.util.*;

/**
 * Utility class to export, clear and import Solr indexes.
 * @author Andrea Schweer schweer@waikato.ac.nz for the LCoNZ Institutional Research Repositories
 */
public class SolrImportExport
{

	private static final ThreadLocal<DateFormat> SOLR_DATE_FORMAT;
	private static final ThreadLocal<DateFormat> SOLR_DATE_FORMAT_NO_MS;
	private static final ThreadLocal<DateFormat> EXPORT_DATE_FORMAT;
	private static final String EXPORT_SEP = "_export_";

	static
	{
		SOLR_DATE_FORMAT = new ThreadLocal<DateFormat>(){
			@Override
			protected DateFormat initialValue() {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				return simpleDateFormat;
			}
		  };
		SOLR_DATE_FORMAT_NO_MS = new ThreadLocal<DateFormat>(){
					@Override
					protected DateFormat initialValue() {
						return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
					}
				  };
		EXPORT_DATE_FORMAT = new ThreadLocal<DateFormat>() {
			@Override
			protected DateFormat initialValue() {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
				simpleDateFormat.setTimeZone(TimeZone.getDefault());
				return simpleDateFormat;
			}
		};
	}

	private static final String ACTION_OPTION = "a";
	private static final String CLEAR_OPTION = "c";
	private static final String OVERWRITE_OPTION = "f";
	private static final String DIRECTORY_OPTION = "d";
	private static final String HELP_OPTION = "h";
	private static final String INDEX_NAME_OPTION = "i";
	private static final String KEEP_OPTION = "k";
	private static final String LAST_OPTION = "l";

	public static final int ROWS_PER_FILE = 10_000;
	
	private static final String MULTIPLE_VALUES_SPLITTER = ",";

	private static final Logger log = Logger.getLogger(SolrImportExport.class);

	/**
	 * Entry point for command-line invocation
	 * @param args command-line arguments; see help for description
	 * @throws ParseException if the command-line arguments cannot be parsed
	 */
	public static void main(String[] args) throws ParseException
	{
		CommandLineParser parser = new PosixParser();
		Options options = makeOptions();

		try
		{
			CommandLine line = parser.parse(options, args);
			if (line.hasOption(HELP_OPTION))
			{
				printHelpAndExit(options, 0);
			}

			String[] indexNames = {"statistics"};
			if (line.hasOption(INDEX_NAME_OPTION))
			{
			    indexNames = line.getOptionValues(INDEX_NAME_OPTION);
			}
			else
			{
			    System.err.println("No index name provided, defaulting to \"statistics\".");
			}
			

			String directoryName = makeDirectoryName(line.getOptionValue(DIRECTORY_OPTION));

			String action = line.getOptionValue(ACTION_OPTION, "export");
			if ("import".equals(action))
			{
				for (String indexName : indexNames)
				{
					File importDir = new File(directoryName);
					if (!importDir.exists() || !importDir.canRead())
					{
						System.err.println("Import directory " + directoryName
								                   + " doesn't exist or is not readable by the current user. Not importing index "
								                   + indexName);
						continue; // skip this index
					}
					try
					{
						String solrUrl = makeSolrUrl(indexName);
						boolean clear = line.hasOption(CLEAR_OPTION);
						importIndex(indexName, importDir, solrUrl, clear);
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
					String lastValue = line.getOptionValue(LAST_OPTION);
					File exportDir = new File(directoryName);
					if (exportDir.exists() && !exportDir.canWrite())
					{
						System.err.println("Export directory " + directoryName
								                   + " is not writable by the current user. Not exporting index "
								                   + indexName);
						continue;
					}

					if (!exportDir.exists())
					{
						boolean created = exportDir.mkdirs();
						if (!created)
						{
							System.err.println("Export directory " + directoryName
									                   + " could not be created. Not exporting index " + indexName);
						}
						continue;
					}

					try
					{
						String solrUrl = makeSolrUrl(indexName);
						String timeField = makeTimeField(indexName);
						exportIndex(indexName, exportDir, solrUrl, timeField, lastValue, line.hasOption(OVERWRITE_OPTION));
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
					try {
						boolean keepExport = line.hasOption(KEEP_OPTION);
						boolean overwrite = line.hasOption(OVERWRITE_OPTION);
						reindex(indexName, directoryName, keepExport, overwrite);
					} catch (IOException | SolrServerException | SolrImportExportException e) {
						e.printStackTrace();
					}
				}
			}
			else
			{
				System.err.println("Unknown action " + action + "; must be import, export or reindex.");
				printHelpAndExit(options, 1);
			}
		}
		catch (ParseException e)
		{
			System.err.println("Cannot read command options");
			printHelpAndExit(options, 1);
		}
	}

	private static Options makeOptions() {
		Options options = new Options();
		options.addOption(ACTION_OPTION, "action", true, "The action to perform: import, export or reindex. Default: export.");
		options.addOption(CLEAR_OPTION, "clear", false, "When importing, also clear the index first. Ignored when action is export or reindex.");
		options.addOption(OVERWRITE_OPTION, "force-overwrite", false, "When exporting or re-indexing, allow overwrite of existing export files");
		options.addOption(DIRECTORY_OPTION, "directory", true,
				                 "The absolute path for the directory to use for import or export. If omitted, [dspace]/solr-export is used.");
		options.addOption(HELP_OPTION, "help", false, "Get help on options for this command.");
		options.addOption(INDEX_NAME_OPTION, "index-name", true,
				                 "The names of the indexes to process. At least one is required. Available indexes are: authority, statistics.");
		options.addOption(KEEP_OPTION, "keep", false, "When reindexing, keep the contents of the data export directory." +
				                                              " By default, the contents of this directory will be deleted once the reindex has finished." +
				                                              " Ignored when action is export or import.");
		options.addOption(LAST_OPTION, "last", true, "When exporting, export records from the last [timeperiod] only." +
				                                             " This can be one of: 'd' (beginning of yesterday through to now);" +
				                                             " 'm' (beginning of the previous month through to end of the previous month);" +
				                                             " a number, in which case the last [number] of days are exported, through to now (use 0 for today's data)." +
															 " Date calculation is done in UTC. If omitted, all documents are exported.");
		return options;
	}

	/**
	 * Reindexes the specified core
	 *
	 * @param indexName the name of the core to reindex
	 * @param exportDirName the name of the directory to use for export. If this directory doesn't exist, it will be created.
	 * @param keepExport whether to keep the contents of the exportDir after the reindex. If keepExport is false and the
	 *                      export directory was created by this method, the export directory will be deleted at the end of the reimport.
     * @param overwrite allow export files to be overwritten during re-index
	 */
	private static void reindex(String indexName, String exportDirName, boolean keepExport, boolean overwrite)
			throws IOException, SolrServerException, SolrImportExportException {
		String tempIndexName = indexName + "-temp";

		String origSolrUrl = makeSolrUrl(indexName);
		String baseSolrUrl = StringUtils.substringBeforeLast(origSolrUrl, "/"); // need to get non-core solr URL
		String tempSolrUrl = baseSolrUrl + "/" + tempIndexName;

		//The configuration details for the statistics shards reside within the "statistics" folder
		String instanceIndexName = indexName.startsWith("statistics-") ? "statistics" : indexName;

		String solrInstanceDir = ConfigurationManager.getProperty("dspace.dir") + File.separator + "solr" + File.separator + instanceIndexName;
		// the [dspace]/solr/[indexName]/conf directory needs to be available on the local machine for this to work
		// -- we need access to the schema.xml and solrconfig.xml file, plus files referenced from there
		// if this directory can't be found, output an error message and skip this index
		File solrInstance = new File(solrInstanceDir);
		if (!solrInstance.exists() || !solrInstance.canRead() || !solrInstance.isDirectory())
		{
			throw new SolrImportExportException("Directory " + solrInstanceDir + "/conf/ doesn't exist or isn't readable." +
					                   " The reindexing process requires the Solr configuration directory for this index to be present on the local machine" +
					                   " even if Solr is running on a different host. Not reindexing index " + indexName);
		}

		String timeField = makeTimeField(indexName);

		// Ensure the export directory exists and is writable
		File exportDir = new File(exportDirName);
		boolean createdExportDir = exportDir.mkdirs();
		if (!createdExportDir && !exportDir.exists())
		{
			throw new SolrImportExportException("Could not create export directory " + exportDirName);
		}
		if (!exportDir.canWrite())
		{
			throw new SolrImportExportException("Can't write to export directory " + exportDirName);
		}

		try
		{
			HttpSolrServer adminSolr = new HttpSolrServer(baseSolrUrl);

			// try to find out size of core and compare with free space in export directory
			CoreAdminResponse status = CoreAdminRequest.getStatus(indexName, adminSolr);
			Object coreSizeObj = status.getCoreStatus(indexName).get("sizeInBytes");
			long coreSize = coreSizeObj != null ? Long.valueOf(coreSizeObj.toString()) : -1;
			long usableExportSpace = exportDir.getUsableSpace();
			if (coreSize >= 0 && usableExportSpace < coreSize)
			{
				System.err.println("Not enough space in export directory " + exportDirName
						                   + "; need at least as much space as the index ("
						                   + FileUtils.byteCountToDisplaySize(coreSize)
						                   + ") but usable space in export directory is only "
						                   + FileUtils.byteCountToDisplaySize(usableExportSpace)
						                   + ". Not continuing with reindex, please use the " + DIRECTORY_OPTION
						                   + " option to specify an alternative export directy with sufficient space.");
				return;
			}

			// Create a temp directory to store temporary core data
			File tempDataDir = new File(ConfigurationManager.getProperty("dspace.dir") + File.separator + "temp" + File.separator + "solr-data");
			boolean createdTempDataDir = tempDataDir.mkdirs();
			if (!createdTempDataDir && !tempDataDir.exists())
			{
				throw new SolrImportExportException("Could not create temporary data directory " + tempDataDir.getCanonicalPath());
			}
			if (!tempDataDir.canWrite())
			{
				throw new SolrImportExportException("Can't write to temporary data directory " + tempDataDir.getCanonicalPath());
			}

			try
			{
				// create a temporary core to hold documents coming in during the reindex
				CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
				createRequest.setInstanceDir(solrInstanceDir);
				createRequest.setDataDir(tempDataDir.getCanonicalPath());
				createRequest.setCoreName(tempIndexName);

				createRequest.process(adminSolr).getStatus();
			}
			catch (SolrServerException e)
			{
				// try to continue -- it may just be that the core already existed from a previous, failed attempt
				System.err.println("Caught exception when trying to create temporary core: " + e.getMessage() + "; trying to recover.");
				e.printStackTrace(System.err);
			}

			// swap actual core with temporary one
			CoreAdminRequest swapRequest = new CoreAdminRequest();
			swapRequest.setCoreName(indexName);
			swapRequest.setOtherCoreName(tempIndexName);
			swapRequest.setAction(CoreAdminParams.CoreAdminAction.SWAP);
			swapRequest.process(adminSolr);

			try
			{
				// export from the actual core (from temp core name, actual data dir)
				exportIndex(indexName, exportDir, tempSolrUrl, timeField, overwrite);

				// clear actual core (temp core name, clearing actual data dir) & import
				importIndex(indexName, exportDir, tempSolrUrl, true);
			}
			catch (Exception e)
			{
				// we ran into some problems with the export/import -- keep going to try and restore the solr cores
				System.err.println("Encountered problem during reindex: " + e.getMessage() + ", will attempt to restore Solr cores");
				e.printStackTrace(System.err);
			}

			// commit changes
			HttpSolrServer origSolr = new HttpSolrServer(origSolrUrl);
			origSolr.commit();

			// swap back (statistics now going to actual core name in actual data dir)
			swapRequest = new CoreAdminRequest();
			swapRequest.setCoreName(tempIndexName);
			swapRequest.setOtherCoreName(indexName);
			swapRequest.setAction(CoreAdminParams.CoreAdminAction.SWAP);
			swapRequest.process(adminSolr);

			// export all docs from now-temp core into export directory -- this won't cause name collisions with the actual export
			// because the core name for the temporary export has -temp in it while the actual core doesn't
			exportIndex(tempIndexName, exportDir, tempSolrUrl, timeField, overwrite);
			// ...and import them into the now-again-actual core *without* clearing
			importIndex(tempIndexName, exportDir, origSolrUrl, false);

			// commit changes
			origSolr.commit();

			// unload now-temp core (temp core name)
			CoreAdminRequest.unloadCore(tempIndexName, false, false, adminSolr);

			// clean up temporary data dir if this method created it
			if (createdTempDataDir && tempDataDir.exists())
			{
				FileUtils.deleteDirectory(tempDataDir);
			}
		}
		finally
		{
			// clean up export dir if appropriate
			if (!keepExport && createdExportDir && exportDir.exists())
			{
				FileUtils.deleteDirectory(exportDir);
			}
		}
	}

	/**
	 * Exports all documents in the given index to the specified target directory in batches of #ROWS_PER_FILE.
	 * See #makeExportFilename for the file names that are generated.
	 *
	 * @param indexName The index to export.
	 * @param toDir The target directory for the export. Will be created if it doesn't exist yet. The directory must be writeable.
	 * @param solrUrl The solr URL for the index to export. Must not be null.
	 * @param timeField The time field to use for sorting the export. Must not be null.
	 * @param overwrite If set, allow export files to be overwritten
	 * @throws SolrServerException if there is a problem with exporting the index.
	 * @throws IOException if there is a problem creating the files or communicating with Solr.
	 * @throws SolrImportExportException if there is a problem in communicating with Solr.
	 */
	public static void exportIndex(String indexName, File toDir, String solrUrl, String timeField, boolean overwrite)
			throws SolrServerException, SolrImportExportException, IOException {
		exportIndex(indexName, toDir, solrUrl, timeField, null, overwrite);
	}

	/**
	 * Import previously exported documents (or externally created CSV files that have the appropriate structure) into the specified index.
	 * @param indexName the index to import.
	 * @param fromDir the source directory. Must exist and be readable.
	 *                   The importer will look for files whose name starts with <pre>indexName</pre>
	 *                   and ends with .csv (to match what is generated by #makeExportFilename).
	 * @param solrUrl The solr URL for the index to export. Must not be null.
	 * @param clear if true, clear the index before importing.
	 * @param overwrite if true, skip _version_ field on import to disable Solr's optimistic concurrency functionality
	 * @throws IOException if there is a problem reading the files or communicating with Solr.
	 * @throws SolrServerException if there is a problem reading the files or communicating with Solr.
	 * @throws SolrImportExportException if there is a problem communicating with Solr.
	 */
	public static void importIndex(final String indexName, File fromDir, String solrUrl, boolean clear)
			throws IOException, SolrServerException, SolrImportExportException
	{
		if (StringUtils.isBlank(solrUrl))
		{
			throw new SolrImportExportException("Could not construct solr URL for index" + indexName + ", aborting export.");
		}

		if (!fromDir.exists() || !fromDir.canRead())
		{
			throw new SolrImportExportException("Source directory " + fromDir
					                                    + " doesn't exist or isn't readable, aborting export of index "
					                                    + indexName);
		}

		HttpSolrServer solr = new HttpSolrServer(solrUrl);

		// must get multivalue fields before clearing
		List<String> multivaluedFields = getMultiValuedFields(solr);

		if (clear)
		{
			clearIndex(solrUrl);
		}

		File[] files = fromDir.listFiles(new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				return name.startsWith(indexName + EXPORT_SEP) && name.endsWith(".csv");
			}
		});

		if (files == null || files.length == 0)
		{
			log.warn("No export files found in directory " + fromDir.getCanonicalPath() + " for index " + indexName);
			return;
		}

		Arrays.sort(files);

		for (File file : files)
		{
			log.info("Importing file " + file.getCanonicalPath());
			ContentStreamUpdateRequest contentStreamUpdateRequest = new ContentStreamUpdateRequest("/update/csv");
			contentStreamUpdateRequest.setParam("skip", "_version_");
			for (String mvField : multivaluedFields) {
				contentStreamUpdateRequest.setParam("f." + mvField + ".split", "true");
				contentStreamUpdateRequest.setParam("f." + mvField + ".separator", MULTIPLE_VALUES_SPLITTER);
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
	 * Exports documents from the given index to the specified target directory in batches of #ROWS_PER_FILE, starting at fromWhen (or all documents).
	 * See #makeExportFilename for the file names that are generated.
	 *
	 * @param indexName The index to export.
	 * @param toDir The target directory for the export. Will be created if it doesn't exist yet. The directory must be writeable.
	 * @param solrUrl The solr URL for the index to export. Must not be null.
	 * @param timeField The time field to use for sorting the export. Must not be null.
	 * @param fromWhen Optionally, from when to export. See options for allowed values. If null or empty, all documents will be exported.
	 * @param overwrite If set, allow export files to be overwritten
	 * @throws SolrServerException if there is a problem with exporting the index.
	 * @throws IOException if there is a problem creating the files or communicating with Solr.
	 * @throws SolrImportExportException if there is a problem in communicating with Solr.
	 */
	public static void exportIndex(String indexName, File toDir, String solrUrl, String timeField, String fromWhen, boolean overwrite)
			throws SolrServerException, IOException, SolrImportExportException
	{
	    log.info(String.format("Export Index [%s] to [%s] using [%s] Time Field[%s] FromWhen[%s]", indexName, toDir, solrUrl, timeField, fromWhen));
	    if (StringUtils.isBlank(solrUrl))
		{
			throw new SolrImportExportException("Could not construct solr URL for index" + indexName + ", aborting export.");
		}

		if (!toDir.exists() || !toDir.canWrite())
		{
			throw new SolrImportExportException("Target directory " + toDir
					                                    + " doesn't exist or is not writable, aborting export of index "
					                                    + indexName);
		}

		HttpSolrServer solr = new HttpSolrServer(solrUrl);

		SolrQuery query = new SolrQuery("*:*");
		if (StringUtils.isNotBlank(fromWhen))
		{
			String lastValueFilter = makeFilterQuery(timeField, fromWhen);
			if (StringUtils.isNotBlank(lastValueFilter))
			{
				query.addFilterQuery(lastValueFilter);
			}
		}

		query.setRows(0);
		query.setGetFieldStatistics(timeField);
		Map<String, FieldStatsInfo> fieldInfo = solr.query(query).getFieldStatsInfo();
		if (fieldInfo == null || !fieldInfo.containsKey(timeField)) {
			log.warn(String.format("Queried [%s].  No fieldInfo found while exporting index [%s] time field [%s] from [%s]. Export cancelled.",
				solrUrl, indexName, timeField, fromWhen));
			return;
		}
		FieldStatsInfo timeFieldInfo = fieldInfo.get(timeField);
		if (timeFieldInfo == null || timeFieldInfo.getMin() == null) {
			log.warn(String.format("Queried [%s].  No earliest date found while exporting index [%s] time field [%s] from [%s]. Export cancelled.",
				solrUrl, indexName, timeField, fromWhen));
			return;
		}
		Date earliestTimestamp = (Date) timeFieldInfo.getMin();

		query.setGetFieldStatistics(false);
		query.clearSorts();
		query.setRows(0);
		query.setFacet(true);
		query.add(FacetParams.FACET_RANGE, timeField);
		query.add(FacetParams.FACET_RANGE_START, SOLR_DATE_FORMAT.get().format(earliestTimestamp) + "/MONTH");
		query.add(FacetParams.FACET_RANGE_END, "NOW/MONTH+1MONTH");
		query.add(FacetParams.FACET_RANGE_GAP, "+1MONTH");
		query.setFacetMinCount(1);

		List<RangeFacet.Count> monthFacets = solr.query(query).getFacetRanges().get(0).getCounts();

		for (RangeFacet.Count monthFacet : monthFacets) {
			Date monthStartDate;
			String monthStart = monthFacet.getValue();
			try
			{
				monthStartDate = SOLR_DATE_FORMAT_NO_MS.get().parse(monthStart);
			}
			catch (java.text.ParseException e)
			{
				throw new SolrImportExportException("Could not read start of month batch as date: " + monthStart, e);
			}
			int docsThisMonth = monthFacet.getCount();

			SolrQuery monthQuery = new SolrQuery("*:*");
			monthQuery.setRows(ROWS_PER_FILE);
			monthQuery.set("wt", "csv");
			monthQuery.set("fl", "*");
			monthQuery.setParam("csv.mv.separator", MULTIPLE_VALUES_SPLITTER);
		
			monthQuery.addFilterQuery(timeField + ":[" +monthStart + " TO " + monthStart + "+1MONTH]");

			for (int i = 0; i < docsThisMonth; i+= ROWS_PER_FILE)
			{
				monthQuery.setStart(i);
				URL url = new URL(solrUrl + "/select?" + monthQuery.toString());

				File file = new File(toDir.getCanonicalPath(), makeExportFilename(indexName, monthStartDate, docsThisMonth, i));
				if (file.createNewFile() || overwrite)
				{
					FileUtils.copyURLToFile(url, file);
					String message = String.format("Solr export to file [%s] complete.  Export for Index [%s] Month [%s] Batch [%d] Num Docs [%d]", 
					    file.getCanonicalPath(), indexName, monthStart, i, docsThisMonth);
					    log.info(message);
				}
				else if (file.exists())
				{
				    String message = String.format("Solr export file [%s] already exists.  Export failed for Index [%s] Month [%s] Batch [%d] Num Docs [%d]", 
				        file.getCanonicalPath(), indexName, monthStart, i, docsThisMonth);
				    throw new SolrImportExportException(message);
				}
				else
				{
				    String message = String.format("Cannot create solr export file [%s].  Export failed for Index [%s] Month [%s] Batch [%d] Num Docs [%d]", 
				        file.getCanonicalPath(), indexName, monthStart, i, docsThisMonth);
				    throw new SolrImportExportException(message);
				}
			}
		}
	}

	/**
	 * Return a filter query that represents the export date range passed in as lastValue
	 * @param timeField the time field to use for the date range
	 * @param lastValue the requested date range, see options for acceptable values
	 * @return a filter query representing the date range, or null if no suitable date range can be created.
	 */
	private static String makeFilterQuery(String timeField, String lastValue) {
		if ("m".equals(lastValue))
		{
			// export data from the previous month
			return timeField + ":[NOW/MONTH-1MONTH TO NOW/MONTH]";
		}

		int days;
		if ("d".equals(lastValue))
		{
			days = 1;
		}
		else
		{
			// other acceptable value: a number, specifying how many days back to export
			days = Integer.valueOf(lastValue); // TODO check value?
		}
		return timeField + ":[NOW/DAY-" + days + "DAYS TO " + SOLR_DATE_FORMAT.get().format(new Date()) + "]";
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

	/**
	 * Creates a filename for the export batch.
	 *
	 * @param indexName The name of the index being exported.
	 * @param exportStart The start timestamp of the export
	 * @param totalRecords The total number of records in the export.
	 * @param index The index of the current batch.
	 * @return A file name that is appropriate to use for exporting the batch of data described by the parameters.
	 */
	private static String makeExportFilename(String indexName, Date exportStart, long totalRecords, int index)
	{
		String exportFileNumber = "";
		if (totalRecords > ROWS_PER_FILE) {
			exportFileNumber = StringUtils.leftPad("" + (index / ROWS_PER_FILE), (int) Math.ceil(Math.log10(totalRecords / ROWS_PER_FILE)), "0");
		}
		return indexName
			+ EXPORT_SEP
			+ EXPORT_DATE_FORMAT.get().format(exportStart)
			+ (StringUtils.isNotBlank(exportFileNumber) ? "_" + exportFileNumber : "")
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
		System.out.println("\n\nCommand Defaults");
		System.out.println("\tsolr-export-statistics  [-a export]  [-i statistics]");
		System.out.println("\tsolr-import-statistics  [-a import]  [-i statistics]");
		System.out.println("\tsolr-reindex-statistics [-a reindex] [-i statistics]");
		System.exit(exitCode);
	}
}
