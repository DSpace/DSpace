package org.dspace.dataonemn;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import org.joda.time.format.DateTimeFormat;

import org.apache.log4j.Logger;

/**
 * This class accepts an HTTP request and passes off to the appropriate location
 * to perform an action. It is very lightweight, just for testing some initial
 * setup. It will eventually be merged into other code.
 * 
 * @author Ryan Scherle
 * @author Kevin S. Clarke
 **/
public class DataOneMN extends HttpServlet implements Constants {
    
    private static final long serialVersionUID = -3545762362447908735L;
    
    private static final Logger log = Logger.getLogger(DataOneMN.class);
    
    private static final String XML_CONTENT_TYPE = "application/xml; charset=UTF-8";
    
    private static final String TEXT_XML_CONTENT_TYPE = "text/xml; charset=UTF-8";
    
    private String myData;
    
    private String mySolr;
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Receives the HEAD HTTP call and passes off to the appropriate method.
     **/
    protected void doHead(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
	String reqPath = buildReqPath(request.getPathInfo());
	Context ctxt = null;
	
	log.debug("pathinfo=" + reqPath);
	
	try {
	    ctxt = new Context();
	    ctxt.ignoreAuthorization();
	    
	    log.debug("DSpace context initialized");
	    
	}
	catch (SQLException details) {
	    log.error("Unable to initialize DSpace", details);
	    
	    try {
		if (ctxt != null) {
		    ctxt.complete();
		}
	    }
	    catch (SQLException deets) {
		log.warn(deets.getMessage(), deets);
	    }
	    
	    throw new ServletException(details);
	}
	
	if (reqPath.startsWith("/object/")) {
	    	ObjectManager objManager = new ObjectManager(ctxt, myData, mySolr);
		describe(reqPath, response, objManager);
	}
	else {
	    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	    
	    try {
		if (ctxt != null) {
		    ctxt.complete();
		}
	    }
	    catch (SQLException details) {
		log.warn(details.getMessage(), details);
	    }
	}
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Receives the HEAD POST call and passes off to the appropriate method.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
	
	String reqPath = buildReqPath(request.getPathInfo());

	if (reqPath.startsWith("/")) {
	    synchronizationFailed(request, response);
	} else {   
	    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * We don't implement this yet.
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {
	response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Cleans up the URL request path.
     **/
    private String buildReqPath(String aPath) {
	String reqPath = aPath;
	
	// handle (and remove) the version indicator
	// TODO: throw an error for requests that do not have a version indicator -- need to notify potential users first
	if(reqPath.startsWith("/v1")) {
	    log.debug("version 1 detected, removing");
	    reqPath = reqPath.substring("/v1".length());
	}

	// remove any trailing slash, but not if the entire path is a slash
	if(reqPath.endsWith("/") && reqPath.length() > 1) {
	    reqPath = reqPath.substring(0, reqPath.length() - 1);
	}

	return reqPath;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Receives the GET HTTP call and passes off to the appropriate method.
     **/
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException {

	String reqPath = buildReqPath(request.getPathInfo());
	Context ctxt = null;
	
	log.debug("reqPath=" + reqPath);
	
	try {
	    ctxt = new Context();
	    ctxt.ignoreAuthorization();
	    
	    log.debug("DSpace context initialized");
	}
	catch (SQLException details) {
	    log.error("Unable to initialize DSpace", details);
	    
	    try {
		if (ctxt != null) {
		    ctxt.complete();
		}
	    }
	    catch (SQLException deets) {
		log.error(deets.getMessage(), deets);
	    }
	    
	    throw new ServletException(details);
	}
	
	if (reqPath.startsWith("/monitor/ping")) {
	    ObjectManager objManager = new ObjectManager(ctxt, myData, mySolr);
	    ping(response, objManager);
	}
	else if (reqPath.startsWith("/log")) {
	    getLogRecords(reqPath, response);
	}
	else if(reqPath.equals("") || reqPath.equals("/") || reqPath.equals("/node")) {
	    getCapabilities(response);
	} else if(reqPath.startsWith("/object")) {			
	    ObjectManager objManager = new ObjectManager(ctxt, myData, mySolr);
	    
	    try {
		if (reqPath.equals("/object")) {
		    listObjects(request, response, objManager);
		}
		else if (reqPath.startsWith("/object/")) {
		    getObject(reqPath, response, objManager);
		}
		else {
		    response.sendError(HttpServletResponse.SC_NOT_FOUND,
				    "Did you mean '/object' or '/object/doi:...'");
		}		
	    }
	    catch (StringIndexOutOfBoundsException details) {
		log.error("Passed request did not find a match", details);
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	    }
	    catch (Exception details) {
		log.error("UNEXPECTED EXCEPTION", details);
	    }
	    finally {
		try {
		    ctxt.complete();
		}
		catch (SQLException sqlDetails) {
		    log.error("Couldn't complete DSpace context");
		}
	    }
	}
	else if (reqPath.startsWith("/meta/")) {
	    SysMetaManager sysMetaMgr = new SysMetaManager(ctxt, myData, mySolr);
	    
	    getSystemMetadata(reqPath, response, sysMetaMgr);
	}
	else if (reqPath.startsWith("/checksum/")) {
	    ObjectManager objManager = new ObjectManager(ctxt, myData, mySolr);
	    getChecksum(reqPath, response, objManager);
	}
	else if (reqPath.startsWith("/isAuthorized/")) {
	    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
	else if (reqPath.startsWith("/accessRules/")) {
	    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
	else if (reqPath.startsWith("/error")) {
	    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
	else if (reqPath.startsWith("/monitor/object")) {
	    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
	else if (reqPath.startsWith("/monitor/event")) {
	    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
	else if (reqPath.startsWith("/monitor/status")) {
	    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
	else if (reqPath.startsWith("/replicate")) {
	    response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}
	else {
	    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
	}
	
	try {
	    if (ctxt != null && ctxt.isValid()) {
		ctxt.complete();
	    }
	}
	catch (SQLException details) {
	    log.error(details.getMessage(), details);
	}
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
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
		
		log.debug("DSpace config loaded from " + aConfig);
	    }
	    else if (!aConfig.exists()) {
		log.fatal("dspace.cfg file at " + aConfig.getAbsolutePath() + " doesn't exist");
		throw new RuntimeException(aConfig.getAbsolutePath() + " doesn't exist");
	    }
	    else if (!aConfig.canRead()) {
		log.fatal("dspace.cfg file at " + aConfig.getAbsolutePath() + " cannot be read");
		throw new RuntimeException("Can't read the dspace.cfg file");
	    }
	    else if (!aConfig.isFile()) {
		log.fatal("dspace.cfg file at " + aConfig.getAbsolutePath() + " is not a file!");
		throw new RuntimeException("Err, dspace.cfg isn't a file?");
	    }
	}
	
	myData = ConfigurationManager.getProperty("stats.datafiles.coll");
	mySolr = ConfigurationManager.getProperty("solr.dryad.server");
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    private int parseInt(HttpServletRequest request, String aParam, int aDefault) {
	String intString = request.getParameter(aParam);
	int intValue = aDefault;
	
	try {
	    if (intString != null) {
		intValue = Integer.parseInt(intString);
	    }
	}
	catch (NumberFormatException details) {
	    log.warn(aParam + " parameter not an int: " + intString);
	}
	
	return intValue;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       Parses a user-entered date. The date may appear in one of many common formats. If the date
       format is not recognized, null is returned.
    **/
    private Date parseDate(HttpServletRequest request, String aParam)
	throws ParseException {
	String date = request.getParameter(aParam);
	
	if (date == null) {
	    return null;
	}
	
	try {
	    return DateTimeFormat.fullDateTime().parseDateTime(date).toDate();
	}
	catch (IllegalArgumentException details) {}
	
	try {
	    return DateTimeFormat.forPattern("yyyyMMdd").parseDateTime(date).toDate();
	}
	catch (IllegalArgumentException details) {}
	
	try {
	    return DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(date).toDate();
	}
	catch (IllegalArgumentException details) {}
	
	try {
	    return DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSS").parseDateTime(date).toDate();
	}
	catch (IllegalArgumentException details) {}
	
	try {
	    return DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss.SSSS").parseDateTime(date).toDate();
	}
	catch (IllegalArgumentException details) {}
	
	try {
	    return DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSS+HH:mm").parseDateTime(date).toDate();
	}
	catch (IllegalArgumentException details) {}
	
	try {
	    return DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss.SSSS+HHmm").parseDateTime(date).toDate();
	}
	catch (IllegalArgumentException details) {}
	
	return null;
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       Performs a basic test that this Member Node is alive
    **/
    private void ping(HttpServletResponse response,  ObjectManager objManager) throws IOException {
	log.info("ping");

	// try to get a single object. If it fails, return an error.
	try {
	    OutputStream dummyOutput = new OutputStream() { public void write(int b) throws IOException {}};
	    objManager.getObject("doi:10.5061/dryad.20/1", "dap", dummyOutput);	    
	} catch (Exception e) {
	    // if there is any problem, respond with an error
	    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	    return;
	}
	
	// the basic test passed; return some simple information about this node
	response.setContentType(XML_CONTENT_TYPE);
	OutputStream out = response.getOutputStream();
	PrintWriter pw = new PrintWriter(out);
	
	// basic node description
	pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
		 "<d1:node xmlns:d1=\"http://ns.dataone.org/service/types/v1\" replicate=\"true\" synchronize=\"true\" type=\"mn\" state=\"up\"> \n" +
		 "<identifier>urn:node:DRYAD</identifier>\n" +
		 "<name>Dryad Digital Repository</name>\n" +
		 "<description>Dryad is an international repository of data underlying peer-reviewed articles in the basic and applied biosciences.</description>\n" +
		 "<baseURL>https://datadryad.org/mn</baseURL>\n");

    	// other random info
	pw.write("<ping success=\"true\"/>\n" +
		 "<subject>CN=urn:node:DRYAD, DC=dataone, DC=org</subject>\n" +
		 "<contactSubject>CN=METACAT1, DC=dataone, DC=org</contactSubject>\n");

	// close xml
	pw.write("</d1:node>\n");
    }

    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       Returns logs of this Member Node's activities.
    **/
    private void getLogRecords(String reqPath, HttpServletResponse response) throws IOException {
	log.info("getLogRecords()");
	response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       Responds with the capabilities of this Member Node.
    **/
    private void getCapabilities(HttpServletResponse response) throws IOException {
	log.info("getCapabilities()");
	response.setContentType(XML_CONTENT_TYPE);
	OutputStream out = response.getOutputStream();
	PrintWriter pw = new PrintWriter(out);

	// basic node description
	pw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
		 "<d1:node xmlns:d1=\"http://ns.dataone.org/service/types/v1\" replicate=\"true\" synchronize=\"true\" type=\"mn\" state=\"up\"> \n" +
		 "<identifier>urn:node:DRYAD</identifier>\n" +
		 "<name>Dryad Digital Repository</name>\n" +
		 "<description>Dryad is an international repository of data underlying peer-reviewed articles in the basic and applied biosciences.</description>\n" +
		 "<baseURL>https://datadryad.org/mn</baseURL>\n");

	// supported services 
	pw.write("<services>\n" +
		 "<service name=\"MNRead\" version=\"v1\" available=\"true\"/>\n" +
		 "<service name=\"MNCore\" version=\"v1\" available=\"true\"/>\n" +
		 "<service name=\"MNAuthorization\" version=\"v1\" available=\"false\"/>\n" +
		 "<service name=\"MNStorage\" version=\"v1\" available=\"false\"/>\n" +
		 "<service name=\"MNReplication\" version=\"v1\" available=\"false\"/>\n" +
		 "</services>\n");

	// synchronization
	String lastHarvestDate = "2012-03-06T14:57:39.851+00:00";
	pw.write("<synchronization>\n" +
		 "<schedule hour=\"*\" mday=\"*\" min=\"0/3\" mon=\"*\" sec=\"10\" wday=\"?\" year=\"*\"/>\n" +
		 "<lastHarvested>" + lastHarvestDate + "</lastHarvested>\n" +
		 "<lastCompleteHarvest>" + lastHarvestDate + "</lastCompleteHarvest>\n" +
		 "</synchronization>\n");

	// other random info
	pw.write("<ping success=\"true\"/>\n" +
		 "<subject>CN=urn:node:DRYAD, DC=dataone, DC=org</subject>\n" +
		 "<contactSubject>CN=METACAT1, DC=dataone, DC=org</contactSubject>\n");

	// close xml
	pw.write("</d1:node>\n");
    
	pw.close();
		   
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       Retrieve a particular object from this Member Node.
    **/
    private void getObject(String reqPath, HttpServletResponse response, ObjectManager objManager) throws IOException {
	log.info("getObject()");
	
	String id = reqPath.substring("/object/".length());
	int lastSlashIndex = id.lastIndexOf("/");
	String format = id.substring(lastSlashIndex + 1);
	String name = id.substring(0, lastSlashIndex);
	String fileName = name.startsWith("doi:") ? name.substring(4) : name;
	
	if (format.equals("dap")) {
	    response.setContentType(XML_CONTENT_TYPE);
	}
	else {
	    ServletContext context = getServletContext();
	    String mimeType = context.getMimeType("f." + format);
	    
	    if (mimeType == null || mimeType.equals("")) {
		mimeType = "application/octet-stream";
	    }
	    
	    log.debug("Setting data file MIME type to: "
		      + mimeType + " (this is configurable)");
			
	    // We need to check types supported here and add to it
	    response.setContentType(mimeType);
	    
	    // We want to download it if viewing in the browser
	    response.setHeader(
			    "Content-Disposition",
			    "attachment; filename=\""
			    + fileName.replaceAll("[\\/|\\.]", "_")
			    + "." + format + "\"");
	}
	
	try {
	    objManager.getObject(name, format,
				 response.getOutputStream());
	}
	catch (NotFoundException details) {
	    response.sendError(HttpServletResponse.SC_NOT_FOUND, name
			    + "." + format + " couldn't be found");
	} catch(Exception e) {
	    log.error("unable to getObject " + reqPath, e);

	    throw new IOException("unable to getObject" + reqPath, e);
	}
	
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       Returns the system metadata associated with an object.       
    **/
    private void getSystemMetadata(String reqPath, HttpServletResponse response, SysMetaManager sysMetaMgr) throws ServletException, IOException {
	log.info("getSystemMetadata()");
	String id = reqPath.substring("/meta/".length());
	
	response.setContentType(TEXT_XML_CONTENT_TYPE); // default for /meta
	
	try {
	    sysMetaMgr.getObjectMetadata(id, response.getOutputStream());
	}
	catch (NotFoundException details) {
	    response.sendError(HttpServletResponse.SC_NOT_FOUND, id
			    + " couldn't be found");
	}
	catch (SQLException details) {
	    throw new ServletException("unable to retrieve System Metadata for " + reqPath, details);
	}
	catch (SolrServerException details) {
	    throw new ServletException("unable to retrieve System Metadata for " + reqPath, details);
	}
	catch (IOException details) {
	    throw new ServletException("unable to retrieve System Metadata for " + reqPath, details);
	}
	catch (StringIndexOutOfBoundsException details) {
	    log.error("Passed request did not find a match", details);
	    response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
	finally {
	    sysMetaMgr.completeContext();
	}
	
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       Returns the basic properties of an object.
    **/
    private void describe(String reqPath, HttpServletResponse response, ObjectManager objManager) throws ServletException, IOException {
	log.info("describe()");

	String id = reqPath.substring("/object/".length());
	String[] parts = objManager.parseIDFormat(id);
	String name = parts[0];
	String format = parts[1];
	
	try {
	    long length = objManager.getObjectSize(name, format);
	    response.setContentLength((int) length);
	    
	    if (format.equals("xml") || format.equals("dap")) {
		response.setContentType(XML_CONTENT_TYPE);
	    }
	    else {
		ServletContext context = getServletContext();
		String mimeType = context.getMimeType("f." + format);
		
		if (mimeType == null || mimeType.equals("")) {
		    mimeType = "application/octet-stream";
		}
		
		log.debug("Checking mimeType of " + format);
		
		log.debug("Setting data file MIME type to: "
			  + mimeType + " (this is configurable)");
		
		response.setContentType(mimeType);
	    }
	}
	catch (SQLException details) {
	    log.error(details.getMessage(), details);
	    throw new ServletException(details);
	}
	catch (StringIndexOutOfBoundsException details) {
	    log.error("Passed request did not find a match", details);
	    response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
	catch (Exception details) {
	    log.error("UNEXPECTED EXCEPTION", details);
	    response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
	finally {
	    objManager.completeContext();
	}
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       Returns the checksum associated with an object.
    **/
    private void getChecksum(String reqPath, HttpServletResponse response, ObjectManager objManager) throws IOException {
	log.info("getChecksum()");

	String id = reqPath.substring("/checksum/".length());
	int lastSlashIndex = id.lastIndexOf("/");
	String format = id.substring(lastSlashIndex + 1);
	String name = id.substring(0, lastSlashIndex);
	
	response.setContentType(TEXT_XML_CONTENT_TYPE);
	
	try {
	    String[] checksum = objManager.getObjectChecksum(name, format);
	    PrintWriter writer = response.getWriter();
	    
	    writer.print("<checksum xmlns=\"" + MN_SERVICE_TYPES_NAMESPACE
			 + "\" algorithm=\"" + checksum[1] + "\">" + checksum[0]
			 + "</checksum>");
	    
	    writer.close();
	}
	catch (NotFoundException details) {
	    response.sendError(HttpServletResponse.SC_NOT_FOUND, name + "/"
			    + format + " not found");
	}
	catch (SQLException details) {
	    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
			       "unable to get checksum for " + reqPath + "; " + details.getMessage());
	}
	catch (IOException details) {
	    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
			    details.getMessage());
	}
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       List all objects available on this Member Node.
    **/
    private void listObjects(HttpServletRequest request, HttpServletResponse response, ObjectManager objManager) throws IOException {
	log.info("listObjects()");
	String format = request.getParameter("objectFormat");

	try {
	    Date from = parseDate(request, "startTime");
	    Date to = parseDate(request, "endTime");
	    
	    int start = parseInt(request, "start",
				 ObjectManager.DEFAULT_START);
	    int count = parseInt(request, "count",
				 ObjectManager.DEFAULT_COUNT);
	    
	    response.setContentType(XML_CONTENT_TYPE);
	    
	    if (count <= 0) {
		OutputStream out = response.getOutputStream();
		objManager.printList(from, to, format, out);
	    }
	    else {
		OutputStream out = response.getOutputStream();
		objManager.printList(start, count, from, to, format,
				     out);
	    }
	} catch (ParseException e) {
	    log.error("unable to parse request info", e);
	    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
			       e.getMessage());
	    
	}  catch (SQLException e) {
	    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
			       "unable to list objects; " + e.getMessage());
	}

    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       Record the fact that a synchronization has failed.
    **/
    private void synchronizationFailed(HttpServletRequest request, HttpServletResponse response) throws IOException {
	log.info("synchronizationFailed()");
	response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
       Retrieve a replica of an item.
    **/
    private void getReplica(HttpServletRequest request, HttpServletResponse response) throws IOException {
	log.info("getReplica()");
	response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }


}
