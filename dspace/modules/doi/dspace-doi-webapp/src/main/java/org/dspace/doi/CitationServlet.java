package org.dspace.doi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

@SuppressWarnings({ "serial" })
public class CitationServlet extends HttpServlet {

	private static final Logger LOGGER = Logger
			.getLogger(CitationServlet.class);

	// RIS requires CRLF to terminate lines; bibtex is ambivalent(?)
	private static final String EOL = "\r\n";

	private static final String ENCODING = "UTF-8";

	private static final String DOI_URL = "http://dx.doi.org/";

	@Override
	protected void doGet(HttpServletRequest aRequest,
			HttpServletResponse aResponse) throws ServletException, IOException {
		String path = aRequest.getContextPath() + aRequest.getServletPath();
		String data = aRequest.getRequestURI().substring(path.length() + 1);
		String referer = aRequest.getHeader("referer");
		Context context = null;

		try {
			int doiStart = data.indexOf('/') + 1;
			String format = data.substring(0, doiStart - 1);

			if (doiStart <= 0 || data.endsWith("/")) {
				throw new DOIFormatException("Invalid or missing DOI");
			}

			DOI doi = new DOI(data.substring(doiStart));
			String fileName = doi.getSuffix().replace('.', '_') + ".";
			String serverName = aRequest.getServerName();
			int port = aRequest.getServerPort();
			String url = aRequest.getScheme() + "://" + serverName;
			DSpaceObject dso;
			String dsID;

			context = new Context();

			if (port != 80) {
				url += (":" + port);
			}

			url += aRequest.getContextPath();

			context.turnOffAuthorisationSystem();

			try {
				dsID = getHandle(url, doi.toString());

				if (dsID == null) {
					throw new IOException("DOI (" + doi.toString()
							+ ") not found");
				}

				dso = PackageManager.resolveToDataPackage(context, dsID);
				fileName += format;

				// Currently supported citation formats
				if (format.equals("ris")) {
					write(aResponse, getRIS((Item) dso), fileName);
				}
				else if (format.equals("bib")) {
					write(aResponse, getBibTex((Item) dso), fileName);
				}
				else {
					aResponse.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,
							"Supplied citation format is not yet supported");
				}
			}
			catch (IOException details) {
				log(details.getMessage(), details);
				aResponse.sendError(HttpServletResponse.SC_NOT_FOUND,
						"Request DOI was not found in the system");
			}
		}
		catch (SQLException details) {
			throw new ServletException(details);
		}
		catch (SocketTimeoutException details) {
			LOGGER.warn("DOI Request Exception: " + details.getMessage(),
					details);
			
			aResponse.sendRedirect("/error?error=doi&subject="
					+ URLEncoder.encode(details.getMessage(), ENCODING)
					+ "&body=Automatically+generated+error+report+--+"
					+ URLEncoder.encode(details.getMessage(), ENCODING) + ": "
					+ URLEncoder.encode(referer, ENCODING));
		}
		catch (DOIFormatException details) {
			LOGGER.warn("DOI Format Exception: " + details.getMessage(),
					details);

			aResponse.sendRedirect("/error?error=doi&subject="
					+ URLEncoder.encode(details.getMessage(), ENCODING)
					+ "&body=Automatically+generated+error+report+--+"
					+ URLEncoder.encode(details.getMessage(), ENCODING) + ": "
					+ URLEncoder.encode(referer, ENCODING));
		}
		finally {
			if (context != null) {
				try {
					context.complete();
				}
				catch (SQLException details) {
					LOGGER.error(details.getMessage(), details);
				}
			}
		}
	}

	private String getRIS(Item aItem) {
		StringBuilder builder = new StringBuilder("TY  - DATA").append(EOL);
		String[] dateParts = DryadCitationHelper.getDate(aItem);
		String title = DryadCitationHelper.getTitle(aItem);
		String doi = DryadCitationHelper.getDOI(aItem);
		String abstrakt = DryadCitationHelper.getAbstract(aItem);
		String[] keywords = DryadCitationHelper.getKeywords(aItem);
		String journalName = DryadCitationHelper.getJournalName(aItem);

		if (doi != null) {
			builder.append("ID  - ").append(doi).append(EOL);
		}

		// Title for data package
		if (title != null) {
			builder.append("T1  - ").append(title).append(EOL);
		}

		// Authors for data package
		for (String author : DryadCitationHelper.getAuthors(aItem)) {
			builder.append("AU  - ").append(author).append(EOL);
		}

		// Date for data package
		if (dateParts.length > 0) {
			int count = 3;

			builder.append("Y1  - ");

			if (dateParts.length < 3) {
				count = dateParts.length;
			}

			for (int index = 0; index < count; index++) {
				builder.append(dateParts[index]).append("/");
			}

			for (; count < 3; count++) {
				builder.append('/');
			}

			builder.append(EOL);
		}

		if (abstrakt != null) {
			builder.append("N2  - ").append(abstrakt).append(EOL);
		}

		for (String keyword : keywords) {
			builder.append("KW  - ").append(keyword).append(EOL);
		}

		if (journalName != null) {
			builder.append("JF  - ").append(journalName).append(EOL);
		}

		builder.append("PB  - ").append("Dryad Digital Repository").append(EOL);

		if (doi != null) {
			builder.append("UR  - ");
			builder.append(DOI_URL + doi.substring(4, doi.length()));
			builder.append(EOL);

			builder.append("DO  - ").append(doi).append(EOL);
		}

		return builder.append("ER  - ").append(EOL).toString();
	}

	private String getBibTex(Item aItem) {
		// No standardized format for data so using 'misc' for now
		StringBuilder builder = new StringBuilder("@misc{");
		String doi = DryadCitationHelper.getDOI(aItem);
		String key = new DOI(doi).getSuffix().replace('.', '_');
		String[] authors = DryadCitationHelper.getAuthors(aItem);
		String year = DryadCitationHelper.getYear(aItem);
		String journalName = DryadCitationHelper.getJournalName(aItem);
		String title = DryadCitationHelper.getTitle(aItem);

		builder.append(key).append(',').append(EOL);

		if (title != null) {
			builder.append("  title = {").append(title).append("},");
			builder.append(EOL);
		}

		if (authors.length > 0) {
			builder.append("  author = {");

			// Bibtex needs the comma... do we want full names here?
			for (int index = 0; index < authors.length; index++) {
				if (index + 1 >= authors.length) { // last one
					builder.append(authors[index].replace(" ", ", "));
				}
				else if (index + 1 < authors.length) { // not last one
					builder.append(authors[index].replace(" ", ", "));
					builder.append(" and ");
				}
			}

			builder.append("},").append(EOL);
		}

		if (year != null) {
			builder.append("  year = {").append(year).append("},").append(EOL);
		}

		if (journalName != null) {
			builder.append("  journal = {").append(journalName).append("}");
			builder.append(EOL);
		}

		if (doi != null) {
			builder.append("  URL = {");
			builder.append(DOI_URL + doi.substring(4, doi.length()));
			builder.append("},");
			builder.append(EOL);

			builder.append("  doi = {");
			builder.append(doi);
			builder.append("},").append(EOL);
		}

		// this should always be last so we don't have to worry about a comma
		builder.append("  publisher = {Dryad Digital Repository}").append(EOL);
		return builder.append("}").append(EOL).toString();
	}

	private String getHandle(String aContextPath, String aDOI)
			throws IOException {
		String solrServer = ConfigurationManager
				.getProperty("solr.search.server");

		if (solrServer != null && !"".equals(solrServer)) {
			SolrServer server = new CommonsHttpSolrServer(solrServer);
			SolrQuery query = new SolrQuery();
			String handle;

			query.setQuery("\"" + aDOI + "\"");

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Solr query: " + query.toString());
			}

			try {
				String prefix = ConfigurationManager
						.getProperty("handle.prefix");
				QueryResponse solrResponse = server.query(query);
				SolrDocumentList docs = solrResponse.getResults();
				long docCount = docs.getNumFound();
				SolrDocument doc;

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Number of Solr responses" + docCount);
				}

				if (docCount > 0) {
					doc = docs.get(0);
					handle = (String) doc.getFirstValue("dc.identifier.uri");

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("dc.identifier.uri from solr: " + handle);
					}

					handle = handle.substring(handle.indexOf(prefix));
				}
				else {
					return null;
				}
			}
			catch (SolrServerException details) {
				LOGGER.error(details.getMessage(), details);
				handle = backupLookup(aContextPath, aDOI);
			}

			try {
				server.commit();
			}
			catch (SolrServerException details) {
				LOGGER.error(details.getMessage(), details);
			}

			if (handle == null) {
				LOGGER.warn("handle returned from solr search was null");

				handle = backupLookup(aContextPath, aDOI);
			}

			return handle;
		}
		else {
			return backupLookup(aContextPath, aDOI);
		}
	}

	private String backupLookup(String aContextPath, String aDOI)
			throws IOException {
		URL url = new URL(aContextPath + "?lookup=" + aDOI);
		URLConnection connection = url.openConnection();

		connection.setConnectTimeout(30000);
		connection.setReadTimeout(30000);

		InputStream inStream = connection.getInputStream();
		InputStreamReader isReader = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(isReader);
		StringBuilder stringBuilder = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
		}

		reader.close();
		return stringBuilder.toString();
	}

	private void write(HttpServletResponse aResponse, String aContent,
			String aFileName) throws IOException {
		aResponse.setContentType("text/plain;charset=utf-8");
		aResponse.setContentLength(aContent.length());
		aResponse.setHeader("Content-Disposition", "attachment; filename=\""
				+ aFileName + "\"");

		// It's all over but the writing...
		PrintWriter writer = aResponse.getWriter();
		writer.print(aContent);
		writer.close();
	}

	public void init() throws ServletException {
		ServletContext context = this.getServletContext();
		String configFileName = context.getInitParameter("dspace.config");
		File config = new File(configFileName);

		if (config != null) {
			if (config.exists() && config.canRead() && config.isFile()) {
				ConfigurationManager.loadConfig(config.getAbsolutePath());
			}
			else if (!config.exists()) {
				throw new ServletException(config.getAbsolutePath()
						+ " doesn't exist");
			}
			else if (!config.canRead()) {
				throw new ServletException("Can't read the dspace.cfg file");
			}
			else if (!config.isFile()) {
				throw new ServletException(
						"Err, seems like the dspace.cfg isn't a file?");
			}
		}
	}
}
