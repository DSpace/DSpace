package ua.edu.sumdu.essuir.servlet;


import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authenticate.AuthenticationMethod;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;


public class UploadServlet extends org.dspace.app.webui.servlet.DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(UploadServlet.class);

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
	    java.io.PrintWriter out = response.getWriter();
	
	    out.println("Upload result:");
	
	    String email = request.getParameter("email");
	    String password = request.getParameter("password");
	    int colID = -1;
	
	    String titleUA = request.getParameter("title_ua");
	    String titleRU = request.getParameter("title_ru");
	    String titleEN = request.getParameter("title_en");
	    String lastname = request.getParameter("lastname");
	    String firstname = request.getParameter("firstname");
	    String year = request.getParameter("year");
	    String month = request.getParameter("month");
	    String day = request.getParameter("day");
	    String citation = request.getParameter("citation");
	    String type = request.getParameter("type");
	    String filename = request.getParameter("filename");
	    String filecontent = request.getParameter("filecontent");
	    String keywords = request.getParameter("keywords");
	    String annotationUA = request.getParameter("abstract_ua");
	    String annotationRU = request.getParameter("abstract_ru");
	    String annotationEN = request.getParameter("abstract_en");
	    String publisher = request.getParameter("publisher");
	    String autoSubmit = request.getParameter("autosubmit");
	
	    String md5 = request.getParameter("md5");
		
	    StringBuilder totalData = new StringBuilder();
	    totalData.append(titleUA);
	    totalData.append(titleRU);
	    totalData.append(titleEN);
	    totalData.append(lastname);
	    totalData.append(firstname);
	    totalData.append(year);
	    totalData.append(month);
	    totalData.append(day);
	    totalData.append(citation);
	    totalData.append(type);
	    totalData.append(filename);
	    totalData.append(filecontent);
	    totalData.append(keywords);
    	totalData.append(annotationUA);
    	totalData.append(annotationRU);
    	totalData.append(annotationEN);
	    totalData.append(publisher);
    
	    
	    String checkedMD5 = Utils.getMD5(totalData.toString());
	

	    int status = AuthenticationManager.authenticate(context, email, password, null, request);
	
	    if (status == AuthenticationMethod.SUCCESS && checkedMD5.equals(md5)) {
	        if (titleUA != null) titleUA = new String(Base64.decodeBase64(titleUA.getBytes()), "utf-8");
	        if (titleRU != null) titleRU = new String(Base64.decodeBase64(titleRU.getBytes()), "utf-8");
	        if (titleEN != null) titleEN = new String(Base64.decodeBase64(titleEN.getBytes()), "utf-8");
	        if (lastname != null) lastname = new String(Base64.decodeBase64(lastname.getBytes()), "utf-8");
	        if (firstname != null) firstname = new String(Base64.decodeBase64(firstname.getBytes()), "utf-8");
	        if (year != null) year = new String(Base64.decodeBase64(year.getBytes()), "utf-8");
	        if (month != null) month = new String(Base64.decodeBase64(month.getBytes()), "utf-8");
	        if (day != null) day = new String(Base64.decodeBase64(day.getBytes()), "utf-8");
	        if (citation != null) citation = new String(Base64.decodeBase64(citation.getBytes()), "utf-8");
	        if (type != null) type = new String(Base64.decodeBase64(type.getBytes()), "utf-8");
	        if (filename != null) filename = new String(Base64.decodeBase64(filename.getBytes()), "utf-8");
	        if (keywords != null) keywords = new String(Base64.decodeBase64(keywords.getBytes()), "utf-8");
	        if (annotationUA != null) annotationUA = new String(Base64.decodeBase64(annotationUA.getBytes()), "utf-8");
	        if (annotationRU != null) annotationRU = new String(Base64.decodeBase64(annotationRU.getBytes()), "utf-8");
	        if (annotationEN != null) annotationEN = new String(Base64.decodeBase64(annotationEN.getBytes()), "utf-8");
		if (publisher != null) publisher = new String(Base64.decodeBase64(publisher.getBytes()), "utf-8");
	
		EPerson submitter = context.getCurrentUser();
	        
	        try {
	            Connection c = null;
	            try {
	                Class.forName(ConfigurationManager.getProperty("db.driver"));
	        
	                c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
	                                                ConfigurationManager.getProperty("db.username"),
	                                                ConfigurationManager.getProperty("db.password"));
	
	                Statement s = c.createStatement();
	
	                ResultSet res = s.executeQuery("SELECT * " +
	                                               "    FROM eperson_service " +
	                                               "    WHERE eperson_id = " + submitter.getID() + "; ");
	
	                if (res.next()) {
	                    colID = res.getInt("collection_id");
	                }
	
	                s.close();
	            } finally {
	                if (c != null) 
	                    c.close();
	            }
	        } catch (Exception e) {
	        }
	

	        Collection col = Collection.find(context, colID);
	
	        // create new submission info
		SubmissionInfo subInfo = SubmissionInfo.load(request, null);
	
		WorkspaceItem wi = WorkspaceItem.create(context, col, true);
		subInfo.setSubmissionItem(wi);
	
	        context.commit();
	
	        subInfo.reloadSubmissionConfig(request);
	
		// questions step
	        subInfo.getSubmissionItem().setMultipleTitles(true);
	        subInfo.getSubmissionItem().setPublishedBefore(true);
	        subInfo.getSubmissionItem().setMultipleFiles(true);
	        subInfo.getSubmissionItem().update();
	
	        context.commit();
	
		// desciption step 1 - title, authors, date, citation, type
		Item item = subInfo.getSubmissionItem().getItem();
	
		java.util.StringTokenizer tokensL = new java.util.StringTokenizer(lastname, ",");
		java.util.StringTokenizer tokensF = new java.util.StringTokenizer(firstname, ",");
	
		while (tokensL.hasMoreTokens() && tokensF.hasMoreTokens()) {
		    item.addMetadata("dc", "contributor", "author", null, new DCPersonName(tokensL.nextToken(), tokensF.nextToken()).toString());
		}
	
		
		if (titleUA != null && !titleUA.equals(""))
		    item.addMetadata("dc", "title", null, null, titleUA);
		else
		    out.println("Warning! Incorrect title!");

		if (titleRU != null && !titleRU.equals(""))
		    item.addMetadata("dc", "title", "alternative", null, titleRU);
		
		if (titleEN != null && !titleEN.equals(""))
		    item.addMetadata("dc", "title", "alternative", null, titleEN);
		
	    int yearI = Integer.parseInt(year != null && !year.equals("") ? year : "-1");
		int monthI = Integer.parseInt(month != null && !month.equals("") ? month : "-1");
		int dayI = Integer.parseInt(day != null && !day.equals("") ? day : "-1");
		if (yearI > 0) {
		    DCDate d = new DCDate(yearI, monthI, dayI, -1, -1, -1);
	
		    item.addMetadata("dc", "date", "issued", null, d.toString());
		} else {
	            out.println("Warning! Incorrect date!");
	        }
	
		if (citation != null && !citation.equals("")) 
	            item.addMetadata("dc", "identifier", "citation", null, citation);
	        else
	            out.println("Warning! Incorrect citation!");
	
	        if (type != null && !type.equals(""))
		    item.addMetadata("dc", "type", null, null, type);
		else
		    out.println("Warning! Incorrect type!");
	
		// keywords
		java.util.StringTokenizer tokensK = new java.util.StringTokenizer(keywords, ",");
	
		while (tokensK.hasMoreTokens()) {
		    item.addMetadata("dc", "subject", null, null, tokensK.nextToken());
		}
		
		// abstract
		if (annotationUA != null && !annotationUA.equals("")) { 
       		item.addMetadata("dc", "description", "abstract", null, annotationUA);
		} else
	        out.println("Warning! Incorrect abstract!");

		if (annotationRU != null && !annotationRU.equals("")) { 
       		item.addMetadata("dc", "description", "abstract", null, annotationRU);
		}
		
		if (annotationEN != null && !annotationEN.equals("")) { 
       		item.addMetadata("dc", "description", "abstract", null, annotationEN);
		}
		
		// publisher
		if (publisher != null && !publisher.equals("")) 
	            item.addMetadata("dc", "publisher", null, null, publisher);
	        else
	            out.println("Warning! Incorrect publisher!");
	
		wi.setStageReached(2);
	        subInfo.getSubmissionItem().update();
	        context.commit();
	
	
	
		// upload step
		if (filename != null && !filename.equals("") && filecontent != null && !filecontent.equals("")) {
		    String filePath = filename;
		
	            // do we already have a bundle?
	            Bundle[] bundles = item.getBundles("ORIGINAL");
	            BitstreamFormat bf = null;
	            Bitstream b = null;
        
		    java.io.InputStream fileInputStream = 
				new java.io.ByteArrayInputStream(Base64.decodeBase64(filecontent.getBytes()));

	            b = item.createSingleBitstream(fileInputStream, "ORIGINAL");
       
	            // Strip all but the last filename. It would be nice
	            // to know which OS the file came from.
	            String noPath = filePath;
        
	            while (noPath.indexOf('/') > -1) {
	                noPath = noPath.substring(noPath.indexOf('/') + 1);
	            }
	        
	            while (noPath.indexOf('\\') > -1) {
	                noPath = noPath.substring(noPath.indexOf('\\') + 1);
	            }
	        
	            b.setName(noPath);
	            b.setSource(filePath);
	            b.setDescription("");
	        
	            // Identify the format
	            bf = FormatIdentifier.guessFormat(context, b);
	            b.setFormat(bf);
	        
	            // Update to DB
	            b.update();
	            item.update();
	         
	            if (bf == null || !bf.isInternal()) {
	                context.commit();
	                subInfo.setBitstream(b);
	            } else {
	                // remove bitstream from bundle..
	                // delete bundle if it's now empty
	                Bundle[] bnd = b.getBundles();
	
	                bnd[0].removeBitstream(b);
	
	                Bitstream[] bitstreams = bnd[0].getBitstreams();
	
	                // remove bundle if it's now empty
	                if (bitstreams.length < 1) {
	                    item.removeBundle(bnd[0]);
	                    item.update();
	                }
	
	                subInfo.setBitstream(null);
	            }
	        } else {
	            out.println("Warning! Incorrect file content!");
		}
	
		wi.setStageReached(4);
	        subInfo.getSubmissionItem().update();
		context.commit();
	
	
		// license step
		item.removeDSpaceLicense();
	
		String license = LicenseUtils.getLicenseText(context.getCurrentLocale(), subInfo.getSubmissionItem().getCollection(), item, submitter);
		LicenseUtils.grantLicense(context, item, license);
	
		wi.setStageReached(7);
	        subInfo.getSubmissionItem().update();
		context.commit();
	
	
		// complete
		try {
		    if (autoSubmit != null && !autoSubmit.equals(""))
			    WorkflowManager.start(context, (WorkspaceItem) subInfo.getSubmissionItem());
	
		    context.commit();
	
		    out.println("Success");
		} catch (Exception e) {
	  	    out.println("Error - Can't submit item, try manual");
	
		    context.getDBConnection().rollback();
		}
	
	        context.setCurrentUser(null);
	    } else {
	        try {
			Thread.sleep(2000);
		} catch (Exception e) {}

	        if (checkedMD5.equals(md5))
		    out.println("Error - Authentication fail");
		else
		    out.println("Error - MD5 Hashcode incorrect!");
	    }
    }

}
