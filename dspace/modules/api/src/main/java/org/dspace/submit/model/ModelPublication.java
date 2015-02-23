package org.dspace.submit.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import org.dspace.core.ConfigurationManager;
import org.dspace.submit.bean.PublicationBean;
import org.w3c.dom.*;
import javax.xml.parsers.*;

//import org.w3c.dom.bootstrap.*;
//import org.w3c.dom.ls.*;

import org.apache.commons.lang.*;



/**
 * Model for the process of modifying a publication object. The publication is stored as a set of files.
 *
 * @author Amol Bapat
 * @author Ryan Scherle
 *
 */

public class ModelPublication
{
	private static Logger log = Logger.getLogger(ModelPublication.class);

	/**
	 * Returns the directory that this bean is stored in.
	 */
	private static File getFullPublicationDir(PublicationBean bean)
	{
		File baseDir = new File(ConfigurationManager.getProperty("submit.journals.dir"));
		File publicationDir = new File(baseDir, bean.getPublicationDir());
		log.debug("publication directory is: " + publicationDir.toString());
		return publicationDir;
	}

    static List<String>  xmlTagNameList = Arrays.asList(
        "Journal","ISSN", "Manuscript","Article_Title",
        "Article_Type","Author","Email","Corresponding_Author",
        "keyword", "Abstract", "Article_Status", "Citation_Title",
        "Citation_Authors", "Publication_DOI"
    );

	/**
	 * Updates the files in the publication directory based on the contents of the bean.
	 * Existing files are overwritten, but no files are deleted.
	 */
	public static PublicationBean updateSaveFiles(PublicationBean bean)
	{
		File publicationDir = getFullPublicationDir(bean);
		log.assertLog(publicationDir.exists(), publicationDir + " does not exist");

		if(bean.isMetadataFromJournal())
		{
			File journalStatus = new File(publicationDir + "metadataFromJournal");
			if(!(journalStatus.exists()))
			{
				try {
					journalStatus.createNewFile();
				} catch (IOException e) {
					log.error("Unable to write metadataFromJournal file", e);
				}
			}
		}

		createDCRecord(bean);
		createDWCRecord(bean);
		createContentsFile(bean);

		return bean;
	}

	/**
	 * Creates a new Darwin Core file for the publication bean.
	 * If a Darwin Core file exists, it is overwritten.
	 */
	private static void createDWCRecord(PublicationBean bean)
	{
		if(bean.getTaxonomicNames() != null && bean.getTaxonomicNames().size() > 0)
		{
			log.debug("creating darwin core file");
			File dwc = new File(getFullPublicationDir(bean), "metadata_dwc.xml");
			try
			{
				dwc.createNewFile();
				FileOutputStream fstream = new FileOutputStream(dwc,false);
				BufferedWriter dwcRecord = new BufferedWriter(new OutputStreamWriter(fstream, "UTF-8"));
				dwcRecord.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
					"<dublin_core schema=\"dwc\"> \n");
				addDCmultipleElement(dwcRecord, "ScientificName", "", bean.getTaxonomicNames());
				dwcRecord.append("</dublin_core> \n");
				dwcRecord.flush();
				dwcRecord.close();
			}
			catch(IOException e)
			{
				log.error("Cannot create darwin core file", e);
				bean.setMessage("<b>Internal Error:</b> IO exception while creating dwc. Please notify the administrators at help@datadryad.org");
			}
		}
	}

	/**
	 * Creates a new Dublin Core file for the publication bean.
	 * If a Dublin Core file exists, it is overwritten.
	 */
	private static void createDCRecord(PublicationBean bean)
	{
		File dc = new File(getFullPublicationDir(bean), "dublin_core.xml");
		try
		{
			dc.createNewFile();
			FileOutputStream fstream = new FileOutputStream(dc,false);
			BufferedWriter dcRecord = new BufferedWriter(new OutputStreamWriter(fstream, "UTF-8"));

			dcRecord.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
							"<dublin_core schema=\"dc\"> \n");

			addDCmultipleElement(dcRecord,"contributor", "author", bean.getAuthors());
			addDCelement(dcRecord, "contributor", "correspondingAuthor", bean.getCorrespondingAuthor());
			addDCmultipleElement(dcRecord, "coverage", "spatial", bean.getCoverageSpatial());
			addDCmultipleElement(dcRecord, "coverage", "temporal", bean.getCoverageTemporal());
			addDCelement(dcRecord, "date", "issued", bean.getPublicationDate());
			addDCelement(dcRecord, "description", null, bean.getAbstract());
			//addDCelement(dcRecord, "identifier", "citation", bean.getFullCitation());
			addDCelement(dcRecord, "identifier", "manuscriptNumber", bean.getManuscriptNumber());
			addDCelement(dcRecord, "identifier", "uri", bean.getDOI());
			addDCelement(dcRecord, "publisher", null, bean.getPublisher());
			addDCelement(dcRecord, "relation", "ispartofseries", bean.getJournalName());
			if(bean.getJournalVolume() != null && bean.getJournalVolume().trim().length() > 0 &&
					bean.getJournalNumber() != null && bean.getJournalNumber().trim().length() > 0) {
				addDCelement(dcRecord, "relation", "ispartofseries", bean.getJournalVolume() + "(" + bean.getJournalNumber() + ")");
			}
			addDCelement(dcRecord, "relation", "ispartofseries", bean.getJournalISSN());
			addDCmultipleElement(dcRecord, "relation", "haspart", bean.getDatasetHandles());
			addDCmultipleElement(dcRecord, "subject", null, bean.getSubjectKeywords());
			addDCelement(dcRecord, "title", null, bean.getTitle());
			//addDCelement(dcRecord, "type", null, "Article");



			dcRecord.append("</dublin_core> \n");
			dcRecord.flush();
			dcRecord.close();
		}
		catch(IOException e)
		{
			log.error("IO exception while creating dc", e);
		}
	}


	/**
	 * Appends multiple DC elements to a string buffer.
	 */
	private static void addDCmultipleElement(BufferedWriter buf, String element,
									String qualifier, List<String> values)
	{
		for(int i=0; i<values.size(); i++)
		{
			String item = values.get(i);
			addDCelement(buf, element, qualifier, item);
		}
	}


	/**
	 * Appends a single DC element to a string buffer. If no qualifier is supplied (i.e., the
	 * qualifier is null or an empty string), no qualifier will be added to the element. If
	 * the value is empty (null or empty string), no DC element will be added to the buffer.
	 */
	private static void addDCelement(BufferedWriter buf, String element,
							String qualifier, String value) {
		if(value != null && value.trim().length()>0) {
			try {
				buf.append("<dcvalue element=\"" + element + "\"");
				if(qualifier == null || qualifier.length() == 0) {
					qualifier = "none";
				}
				buf.append(" qualifier=\"" + qualifier + "\"");
				buf.append(">");
				buf.append(StringEscapeUtils.escapeXml(value));
				buf.append("</dcvalue>\n");
			} catch(IOException e) {
				log.error("Unable to write to DC file", e);
			}
		}
	}


	/**
	 * Creates a contents file, which describes the contents of the publication directory.
	 * If a contents file exists, it is overwritten.
	 */
	private static void createContentsFile(PublicationBean bean)
	{
		File contents = new File(getFullPublicationDir(bean), "contents");
		try
		{
			// for now, the publications will not have bitstreams, so
			// the contents file can remain empty
			contents.createNewFile();
		}
		catch (IOException e)
		{
			log.error("unable to create file " + contents, e);
		}
	}

	/**
	 * Based on the contents of a publication bean, creates an initial working directory for the publication.
	 * If the working directory exists, build a new bean from it.
	 */
	public static PublicationBean initPublicationDir(PublicationBean pBean)
	{
		log.debug("initializing publication directory");

		// check if publication directory exists
		File baseDir = new File(ConfigurationManager.getProperty("submit.journals.dir"));
		String publicationDir = pBean.getManuscriptNumber() + "-pub/" + pBean.getManuscriptNumber();
		pBean.setPublicationDir(publicationDir);
		File fullPublicationDir = new File(baseDir, publicationDir);

		if(fullPublicationDir.exists())
		{
			// if so, initialize bean from it
			pBean = getDataFromSaveFiles(pBean.getManuscriptNumber());
		}
		else
		{
			// if not, create the directory and seed with appropriate files
			fullPublicationDir.mkdirs();
			updateSaveFiles(pBean);
		}

		return pBean;
	}

	/**
	 * Looks up datasets associated with this publication, and attaches them to the publication bean.
	 * Lookup is performed by reading dataset files from the filesystem.
	 */
	public static PublicationBean getAllDataSets(PublicationBean pbean)
	{
		File baseDir = new File(ConfigurationManager.getProperty("submit.journals.dir"));
		File dataDir = new File(baseDir,pbean.getManuscriptNumber() + "-data");
		List<String> dataSets = new ArrayList<String>();

		if (dataDir.exists()) {
			File[] dataSetDirs = dataDir.listFiles();
			for(File dataSetDir : dataSetDirs) {
				if (dataSetDir.isDirectory()) {
					// we have found a single dataset directory
					File dcFile = new File(dataSetDir, "dublin_core.xml");
					try {
						BufferedReader input = new BufferedReader(new InputStreamReader
			                     (new FileInputStream(dcFile),"UTF-8"));

						String inputLine = new String();
						while(inputLine != null) {
							inputLine = input.readLine();
							// if (inputLine.contains("description")) {
							// tag description is a comment box
							// should be title tag
							if (inputLine.contains("title")) {
								dataSets.add(extractElementContent(inputLine));
								break;
							}
						}
						input.close();
					}
					catch(FileNotFoundException e) {
						pbean.setMessage("Invalid manuscript number");
					}
					catch(IOException e) {
						log.error("IOException getting data sets", e);
						pbean.setMessage("<b>Internal Error:</b> Cannot read datasets. Please contact the Dryad administrators at help@datadryad.org");
					}
					catch(NullPointerException e) {
						log.error("NullPointerException getting data sets", e);
						pbean.setMessage("<b>Internal Error:</b> Cannot read datasets. Please contact the Dryad administrators at help@datadryad.org");
					}
				}
			}

		}
		pbean.setDatasetBeans(dataSets);
		return pbean;
	}

	/**
	 * Initialize a publication bean from the submission system's temporary data files.
	 */
	public static PublicationBean getDataFromSaveFiles(String manuscriptNumber)
	{
		PublicationBean pbean = new PublicationBean();

		String publicationDir = manuscriptNumber + "-pub/" + manuscriptNumber;
		pbean.setPublicationDir(publicationDir);
		File fullPublicationDir = new File(ConfigurationManager.getProperty("submit.journals.dir"), publicationDir);

		File journalStatus = new File(fullPublicationDir, "metadataFromJournal");
		if(journalStatus.exists())
		{
			pbean.setMetadataFromJournal(true);
		}

		File dcFile = new File(fullPublicationDir, "dublin_core.xml");
		try
		{
			BufferedReader input = new BufferedReader(new InputStreamReader
                    (new FileInputStream(dcFile),"UTF-8"));

			String inputLine = input.readLine();
			while(inputLine != null)
			{
				String value = extractElementContent(inputLine);
				if (inputLine.contains("\"author\""))
				{
					pbean.getAuthors().add(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"correspondingAuthor\""))
				{
					pbean.setCorrespondingAuthor(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"coverage\"") && inputLine.contains("\"spatial\""))
				{
					pbean.getCoverageSpatial().add(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"coverage\"") && inputLine.contains("\"temporal\""))
				{
					pbean.getCoverageTemporal().add(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"date\""))
				{
					pbean.setPublicationDate(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"description\""))
				{
					pbean.setAbstract(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"identifier\"") && inputLine.contains("manuscriptNumber"))
				{
					pbean.setManuscriptNumber(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"identifier\"") && inputLine.contains("\"citation\""))
				{
					pbean.setFullCitation(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"identifier\"") && inputLine.contains("\"uri\""))
				{
					pbean.setDOI(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"publisher\"") && inputLine.contains("\"manuscriptNumber\""))
				{
					pbean.setPublisher(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"relation\"") && inputLine.contains("\"ispartofseries\""))
				{
					if(value.contains("(") && value.contains(")") && (value.indexOf(")") - value.indexOf("(") < 3)) { // assume it is a volume and number, with the format 5 (4)
						// TODO: parse the value into volume and number
					} else if (Character.isDigit(value.charAt(0)) && value.contains("-")) { // assume it is an ISSN
						pbean.setJournalISSN(StringEscapeUtils.unescapeXml(value));
					} else {
						pbean.setJournalName(StringEscapeUtils.unescapeXml(value));
					}
				}
				else if (inputLine.contains("\"title\""))
				{
					pbean.setTitle(StringEscapeUtils.unescapeXml(value));
				}
				else if (inputLine.contains("\"subject\""))
				{
					pbean.getSubjectKeywords().add(StringEscapeUtils.unescapeXml(value));
				}
				else
				{
					log.debug("unexpected input line: \n" + inputLine);
				}
				inputLine = input.readLine();
			}
			input.close();
		}
		catch(FileNotFoundException ex)
		{
			log.error("unable to find manuscript file " + dcFile, ex);
			pbean.setMessage("Invalid manuscript number");
		}
		catch(IOException ex)
		{
			log.error("unable to read from manuscript file " + dcFile, ex);
			pbean.setMessage("<b>Internal Error:</b> Cannot read saved publication information. Please contact the Dryad administrators at help@datadryad.org");

		}

		// process darwin core file

		File taxonFile = new File(fullPublicationDir, "darwin_core.xml");
		if(taxonFile.exists()) {
			try {
				BufferedReader input = new BufferedReader(new InputStreamReader
	                    (new FileInputStream(taxonFile),"UTF-8"));
				String inputLine = input.readLine();
				while(inputLine != null) {
					String value = extractElementContent(inputLine);
					if (inputLine.contains("\"ScientificName\"")) {
						pbean.getTaxonomicNames().add(value);
					}
					inputLine = input.readLine();
				}
				input.close();
			}
			catch(FileNotFoundException ex) {
				log.error("unable to find darwin core file " + taxonFile + ", skipping", ex);
			}
			catch(IOException ex) {
				log.error("unable to read from taxon file " + taxonFile, ex);
				pbean.setMessage("<b>Internal Error:</b> Cannot read saved publication information (dwc). Please contact the Dryad administrators at help@datadryad.org");
			}
		}

		return pbean;
	}

	/**
	 * Initialize a publication bean from a publisher's data file.
	 */
	public static PublicationBean getDataFromPublisherFile(String manuscriptNumber, String journalID, String journalPath)
	{
		// TODO: This method has very rudimentary XML parsing. It would be much better to actually run it through the Java XML parser.

	    log.debug("getting data for manu " + manuscriptNumber + " from journal " + journalID + " at path " + journalPath);
	    
		// read data from file and add it to bean;
		PublicationBean pbean = new PublicationBean();
		pbean.setMetadataFromJournal(true);
		File f = new File(journalPath + File.separator + manuscriptNumber + ".xml");
		    // for a List object for a multipe node
			List<String> authors = new ArrayList<String>();
			List<String> keywords = new ArrayList<String>();


			// read the metadata xml by DOM level 3 LSParser
		    // InputStream in = new FileInputStream(f);

	        try {
	            /* DOM 3 parser case not work with JSE 1.5
	            DOMImplementationRegistry registry
	            = DOMImplementationRegistry.newInstance();
	            DOMImplementationLS domImpLS =
	            (DOMImplementationLS) registry.getDOMImplementation("LS 3.0");
	            LSInput input = domImpLS.createLSInput();
	            input.setEncoding("UTF-8");
	            input.setByteStream(in);
	            LSParser parser = domImpLS.createLSParser(
	                    DOMImplementationLS.MODE_SYNCHRONOUS, null);
	            */
	            DocumentBuilder builder =
	                DocumentBuilderFactory.newInstance().newDocumentBuilder();

	            Document doc = builder.parse(f);

	            Element root = doc.getDocumentElement();

	            for (String tag: xmlTagNameList){
	                NodeList nl = root.getElementsByTagName(tag);
	                log.debug(tag+"(how many nodes="+nl.getLength()+"):");
			
	                for (int i=0;i<nl.getLength();i++){
	                    String text = nl.item(i).getTextContent();
	                    log.debug(text);

			    if (tag.equals("Corresponding_Author")){
				pbean.setCorrespondingAuthor(StringEscapeUtils.unescapeXml(text));
			    }

	                    if (tag.equals("Article_Title")){
	                        pbean.setTitle(StringEscapeUtils.unescapeXml(text));
	                    }

			    if (tag.equals("Journal")){
				pbean.setJournalName(StringEscapeUtils.unescapeXml(text));
			    }

			    if (tag.equals("Manuscript")){
				    // Removing Timestamp.

				    //pbean.setManuscriptNumber(journalID +
					//		  "-" + StringEscapeUtils.unescapeXml(text) + "-" + System.currentTimeMillis());


                    pbean.setManuscriptNumber(journalID +"-" + StringEscapeUtils.unescapeXml(text));


			    }
			    if (tag.equals("ISSN")){
				pbean.setJournalISSN(StringEscapeUtils.unescapeXml(text));
			    }

			    if (tag.equals("Abstract")){
				pbean.setAbstract(StringEscapeUtils.unescapeXml(text));
			    }
			    
			    if (tag.equals("Author")){
				authors.add(StringEscapeUtils.unescapeXml(text).trim());
			    }
			    if (tag.equals("keyword")){
				keywords.add(StringEscapeUtils.unescapeXml(text).trim());
			    }
			    if(tag.equalsIgnoreCase("Email")){
				pbean.setEmail(StringEscapeUtils.unescapeXml(text).trim());
			    }
			    if(tag.equals("Article_Status")){
				String ttext = text.trim().toLowerCase();

                pbean.setStatus(ttext);


				if(ttext.equals("submitted") ||
				   ttext.equals("in review")   ||
				   ttext.equals("under review")  ||
				   ttext.equals("revision in review") ||
				   ttext.equals("revision under review")
				   ) {
				    pbean.setSkipReviewStep(false);
				} else if(ttext.equals("accepted") ||
					  ttext.startsWith("reject") ||
					  ttext.equals("open reject") ||
					  ttext.equals("transferred") ||
					  ttext.equals("needs revision")) {
				    pbean.setSkipReviewStep(true);
				} else {
				    log.error("unexpected article status " + text.trim());
				}
				 
			    }
                            if(tag.equals("Article_Type")) {
                                pbean.setArticleType(StringEscapeUtils.unescapeXml(text).trim());
                            }
                            if(tag.equals("Citation_Title")) {
                                pbean.setCitationTitle(StringEscapeUtils.unescapeXml(text).trim());
                            }
                            if(tag.equals("Citation_Authors")) {
                                pbean.setCitationAuthors(StringEscapeUtils.unescapeXml(text).trim());
                            }
                            if(tag.equals("Publication_DOI")) {
                                String doi = StringEscapeUtils.unescapeXml(text).trim();
                                pbean.setDOI(formatDOI(doi));
                            }

			}

		    }
		}
	        catch(FileNotFoundException ex)
	        {
	            log.error("unable to find manuscript file " + f, ex);
	            pbean.setMessage("Invalid manuscript number");
	        }
	        catch(IOException ex)
	        {
	            log.error("unable to read from manuscript file " + f, ex);
	            pbean.setMessage("Internal Error: Cannot import publication description. Please notify the Dryad administrators at help@datadryad.org");

	        }
	        catch (Exception e) {
	            log.fatal("Exception occurred while parsing:", e);
                pbean.setMessage("Internal Error: Cannot import publication description because it contains invalid xml. Please notify the Dryad administrators at help@datadryad.org");
	        }

			// set list of author names to the bean only
	        // when the entire file is read
			pbean.setAuthors(authors);
			if(keywords.size() > 0) {
				pbean.setSubjectKeywords(keywords);
			}


		return pbean;
	}

	/**
	 * Updates a publication bean (retrieved from the session) based on HTTP request parameters.
	 */
	public static PublicationBean updatePublicationBean(HttpServletRequest req)
	{
		log.debug("updating publication bean from form fields");
		HttpSession session = req.getSession();
		PublicationBean pBean = (PublicationBean) session.getAttribute("publicationBean");

		String title = req.getParameter("title");
		if (title != null && title.trim().length() > 0)
		{
			pBean.setTitle(title);
		}

		/// Checking if user has removed something
		int flagAuthorRemove = 999, flagSubjectKeywordRemove = 999,flagTaxonomicNameRemove = 999,flagCoverageSpatialRemove = 999,flagCoverageTemporalRemove = 999;
		String userAction = (String) req.getAttribute("message");
		if (userAction != null)
		{
			if (userAction.contains("Remove"))
			{
				log.debug("User Removed Something");
				if (userAction.contains("Remove author"))
				{
					log.debug("user removed author");
					log.debug("userAction = " +userAction);
					flagAuthorRemove = Integer.valueOf(userAction.substring(userAction.indexOf("$$")+2, userAction.length()));
				}
				else if (userAction.contains("Remove subjectKeyword"))
				{
					log.debug("user removed subject keyword");
					log.debug("userAction = " +userAction);
					flagSubjectKeywordRemove = Integer.valueOf(userAction.substring(userAction.indexOf("$$")+2, userAction.length()));
				}
				else if (userAction.contains("Remove taxonomicName"))
				{
					log.debug("user removed taxonomic Name");
					log.debug("userAction = " +userAction);
					flagTaxonomicNameRemove = Integer.valueOf(userAction.substring(userAction.indexOf("$$")+2, userAction.length()));
				}
				else if (userAction.contains("Remove coverageSpatial"))
				{
					log.debug("user removed coverage-spatial");
					log.debug("userAction = " +userAction);
					flagCoverageSpatialRemove = Integer.valueOf(userAction.substring(userAction.indexOf("$$")+2, userAction.length()));
				}
				else if (userAction.contains("Remove coverageTemporal"))
				{
					log.debug("user removed coverage-temporal");
					log.debug("userAction = " +userAction);
					flagCoverageTemporalRemove = Integer.valueOf(userAction.substring(userAction.indexOf("$$")+2, userAction.length()));
				}
			}
		}


		/// Authors
		List <String> authorNames = new ArrayList<String>();
		for(int i = 0; req.getParameter("author"+i)!=null; i++)
		{
			if (i != flagAuthorRemove) /// don't add this item to the list if user is removing it.
			{
				if (!req.getParameter("author"+i).equals(""))
				{
					authorNames.add(req.getParameter("author"+i));
				}
			}
		}
		String nameLast = req.getParameter("nameLast");
		String nameFirst = req.getParameter("nameFirst");

		if (nameLast != null && !nameLast.trim().equals("")
			&& nameFirst != null && !nameFirst.trim().equals(""))
		{
			authorNames.add(nameFirst.trim() + " " + nameLast.trim());
		}
		pBean.setAuthors(authorNames);
		/// End of Add/Remove code for Authors

		String pubAbstract = req.getParameter("abstract");
		pBean.setAbstract(pubAbstract);

		String doi = req.getParameter("doi");
		pBean.setDOI(formatDOI(doi));

		String journalName = req.getParameter("journalName");
		pBean.setJournalName(journalName);

		String publicationDate = req.getParameter("publicationDate");
		pBean.setPublicationDate(publicationDate);

		String journalVolume = req.getParameter("journalVolume");
		pBean.setJournalVolume(journalVolume);

		String journalNumber = req.getParameter("journalNumber");
		pBean.setJournalNumber(journalNumber);

		String publisher = req.getParameter("publisher");
		pBean.setPublisher(publisher);

		String fullCitation = req.getParameter("fullCitation");
		pBean.setFullCitation(fullCitation);

		String corrAuthorName = req.getParameter("corrAuthorName");
		if (corrAuthorName != null && corrAuthorName.trim().length() > 0)
		{
			pBean.setCorrespondingAuthor(corrAuthorName);
		}


		/// Subject Keywords
		List <String> subjectKeywords = new ArrayList<String>();
		for(int i = 0; req.getParameter("subject"+i)!=null; i++)
		{
			if (i != flagSubjectKeywordRemove) /// don't add this item to the list if user is removing it.
			{
				if (!req.getParameter("subject"+i).equals(""))
				{
					subjectKeywords.add(req.getParameter("subject"+i));
				}
			}
		}
		String newSubjectKeywords = req.getParameter("subjectKeywords");
		if (newSubjectKeywords != null && newSubjectKeywords.trim().length() > 0)
		{
			String[] keywordsArray = newSubjectKeywords.split(",|;");
			for(int i = 0; i < keywordsArray.length; i++) {
				subjectKeywords.add(keywordsArray[i].trim());
			}
		}
		pBean.setSubjectKeywords(subjectKeywords);
		/// End of Add/Remove code for Subject Keywords


		/// Taxonomic Names
		List <String> taxonomicNames = new ArrayList<String>();
		for(int i = 0; req.getParameter("taxo"+i)!=null; i++)
		{
			if (i != flagTaxonomicNameRemove) /// don't add this item to the list if user is removing it.
			{
				if (!req.getParameter("taxo"+i).equals(""))
				{
					taxonomicNames.add(req.getParameter("taxo"+i));
				}
			}
		}
		String newTaxonomicNames = req.getParameter("taxonomicNames");
		if (newTaxonomicNames != null && newTaxonomicNames.trim().length() > 0)
		{
			String[] taxonomicNamesArray = newTaxonomicNames.split(",|;");
			for(int i = 0; i < taxonomicNamesArray.length; i++) {
				taxonomicNames.add(taxonomicNamesArray[i].trim());
			}
		}
		pBean.setTaxonomicNames(taxonomicNames);
		/// End of Add/Remove code for Taxonomic names



		/// Coverage - Spatial
		List <String> coverageSpatial = new ArrayList<String>();
		for(int i = 0; req.getParameter("cS"+i)!=null; i++)
		{
			if (i != flagCoverageSpatialRemove) /// don't add this item to the list if user is removing it.
			{
				if (!req.getParameter("cS"+i).equals(""))
				{
					coverageSpatial.add(req.getParameter("cS"+i));
				}
			}
		}
		String newCoverageSpatial = req.getParameter("coverageSpatial");
		if (newCoverageSpatial != null && newCoverageSpatial.trim().length() > 0)
		{
			String[] coverageSpatialArray = newCoverageSpatial.split(",|;");
			for(int i = 0; i < coverageSpatialArray.length; i++) {
				coverageSpatial.add(coverageSpatialArray[i].trim());
			}
		}
		pBean.setCoverageSpatial(coverageSpatial);
		/// End of Add/Remove code for Coverage - Spatial


		/// Coverage - Temporal
		List <String> coverageTemporal = new ArrayList<String>();
		for(int i = 0; req.getParameter("cT"+i)!=null; i++)
		{
			if (i != flagCoverageTemporalRemove) /// don't add this item to the list if user is removing it.
			{
				if (!req.getParameter("cT"+i).equals(""))
				{
					coverageTemporal.add(req.getParameter("cT"+i));
				}
			}
		}
		String newCoverageTemporal = req.getParameter("coverageTemporal");
		if (newCoverageTemporal != null && newCoverageTemporal.trim().length() > 0)
		{
			String[] coverageTemporalArray = newCoverageTemporal.split(",|;");
			for(int i = 0; i < coverageTemporalArray.length; i++) {
				coverageTemporal.add(coverageTemporalArray[i].trim());
			}
		}
		pBean.setCoverageTemporal(coverageTemporal);
		/// End of Add/Remove code for Coverage - Temporal


		return pBean;
	}


	/**
	 * Takes a string that is one complete XML element, and returns the contents of the tag. Assumes that the
	 * opening tag is at the beginning of the line, and the ending tag is at the end of the line.
	 */
	private static String extractElementContent(String xmlString) {
		int endOfInitialTag = xmlString.indexOf('>');
		int startOfClosingTag = xmlString.lastIndexOf("</");

		String result = null;
		if((endOfInitialTag >= 0) && (startOfClosingTag >= endOfInitialTag))
		{
			result = xmlString.substring(endOfInitialTag + 1, startOfClosingTag);
		}
		return result;
	}


	/**
	 * Checks if all the required fields are present in the bean. If all required fields are
	 * included, null will be returned. Otherwise, a Vector of the missing fields will be returned.
	 */
	public static Vector checkPublicationBeanForRequiredFields (PublicationBean publicationBean)
	{
		Vector missingFields = new Vector();

		if (publicationBean.getTitle() == null || publicationBean.getTitle().trim().length() == 0) {
			missingFields.add("Title");
		}
		if (publicationBean.getAuthors() == null || publicationBean.getAuthors().isEmpty()) {
			missingFields.add("Authors");
		}
		if (publicationBean.getJournalName() == null || publicationBean.getJournalName().trim().length() == 0) {
			missingFields.add("Journal");
		}

		if(missingFields.size() > 0) {
			return missingFields;
		} else {
			return null;
		}
	}

        private static String formatDOI(String doi) {
            if (doi != null && doi.length() > 0) {
                    doi = doi.trim();
                    if (doi.startsWith("http://dx.doi.org/")) {
                            doi = doi.replace("http://dx.doi.org/", "doi:");
                    }
                    if (doi.startsWith("10.")) {
                            doi = "doi:" + doi;
                    }
            }
            return doi;
        }

}
