/*
 * Copyright (c) 2004 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lims.dspace;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// XML
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.InvalidXPathException;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.Text;
import org.dom4j.XPath;

import org.dom4j.io.SAXReader;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentInputSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import org.xml.sax.InputSource;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import org.dspace.app.webui.SimpleAuthenticator;

import edu.umd.lims.util.ErrorHandling;


/*********************************************************************
 DRUM Authentication.

 @author  Ben Wallberg

*********************************************************************/


public class Authenticator extends SimpleAuthenticator
{
  /** log4j category */
  private static Logger log = Logger.getLogger(Authenticator.class);

  private static List lAuth = null;
  private static Date dOldFileDate = null;


  /***************************************************** getSpecialGroups */
  /**
   */

  public int[] 
  getSpecialGroups(Context context, HttpServletRequest request)
    throws SQLException
  {        
    Ldap ldap = (Ldap)
      request.getSession().getAttribute("dspace.current.user.ldap");

    if (ldap != null) {
        
      // Return a list of special group IDs.
      return getAuthorization(context, ldap);
    }
    
    return new int[0];
  }


  /***************************************************** getAuthorization */
  /**
   */

  public static int[] 
  getAuthorization(Context context, Ldap ldap)
  {
    Set sGroups = new HashSet();

    try {
      loadAuthorizationTable(context);

      // Loop through each ou
      Iterator iUnits = ldap.getUnits().iterator();
      while (iUnits.hasNext()) {
        String strUnit = (String)iUnits.next();

        // Loop through the lAuth table
        Iterator iMaps = lAuth.iterator();
        while (iMaps.hasNext()) {
          LdapMap map = (LdapMap)iMaps.next();

          // Check for a match
          if (map.strOU.equals(strUnit) &&
              (!map.bFaculty || ldap.isFaculty()))
          {
            sGroups.add(map.group);
          }
        }
      }
    }
    catch (Exception e) {
      log.error(LogManager.getHeader(null,
                                     "ldap_authorization",
                                     "unable to get authorization information for ldap user"),
                e);
      log.error(ErrorHandling.getStackTrace(e));

      return new int[]{};
    }
      
    // Create the return arry
    int ret[] = new int[sGroups.size()];
    Iterator i = sGroups.iterator();
    int j = 0;
    while (i.hasNext()) {
      ret[j++] = ((Group)i.next()).getID();
    }

    return ret;
  }


  /*********************************************** loadAuthorizationTable */
  /**
   * Load the authorization table.
   */

  private synchronized static void
  loadAuthorizationTable(Context context)
    throws Exception
  {
    boolean bSuccess = true;

    // Open the file
    String strFile = ConfigurationManager.getProperty("ldap.authorization.config");
    File fFile = new File(strFile);
    Date dNewFileDate = new Date(fFile.lastModified());

    try {
      if (lAuth == null || 
          (dOldFileDate != null && dNewFileDate.after(dOldFileDate))) 
      {
	log.info("Loading ldap.authorization.conf map");

        bSuccess = false;
        dOldFileDate = dNewFileDate;
        lAuth = new ArrayList();

        // open the file
        FileInputStream is = new FileInputStream(fFile);

        // read the xml document
        SAXReader reader = new SAXReader();
        Document doc = reader.read(is);

        List lNodes = doc.selectNodes("/maps/map");

        // loop through the nodes
        for (Iterator iNodes = lNodes.iterator(); iNodes.hasNext(); ) {
          Element map = (Element)iNodes.next();

	  try {
	    // Get values
	    String strOU = map.attributeValue("ou");
	    String strGroup = map.attributeValue("group");
	    String strFaculty = map.attributeValue("faculty");

	    boolean bFaculty = (strFaculty != null && 
				strFaculty.equals("false")
				? false
				: true);

	    Group group = Group.findByName(context, strGroup);
	    if (group == null) {
	      throw new Exception("Unrecognized group: " + strGroup);
	    }
	
	    // Add the map
	    lAuth.add(new LdapMap(strOU, group, bFaculty));
	  }

	  catch (Exception e) {
	    log.error(LogManager.getHeader(null,
					   "ldap_authorization",
					   "Error reading line from ldap auth file: " + e.getMessage()));
	  }
	}

	log.info("map size=" + lAuth.size());
      }

      bSuccess = true;
    }

    finally {
        if (! bSuccess) {
          lAuth = null;
	}
    }


  }


  /***************************************************************** main */
  /**
   * Command line interface.
   */
  
  public static void
    main(String[] args)
    throws Exception
  {
    PropertyConfigurator.configure("log4j.properties");

    Context context = new org.dspace.core.Context();
    Ldap ldap = new Ldap(context);
    ldap.checkUid(args[0]);
    ldap.close();

    int x[] = Authenticator.getAuthorization(context, ldap);
    for (int i=0; i < x.length; i++) {
      Group group = Group.find(context, x[i]);
      System.out.println(group.getName());
    }
  }


}



