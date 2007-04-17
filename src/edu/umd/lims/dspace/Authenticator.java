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

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import org.dspace.app.webui.SimpleAuthenticator;

import edu.umd.lims.dspace.Ldap;


/*********************************************************************
 DRUM Authentication.

 @author  Ben Wallberg

 <pre>
 Revision History:

   2005/01/25: Ben
     - reread ldap.authorization.config if it has changed

   2005/01/19: Ben
     - use ldap.authorization.config file to map ldap ou to Group

   2004/12/22: Ben
     - get the entire ldap object
     - move authorizations into getAuthorization

   2004/12/14: Ben
     - initial version
 </pre>

*********************************************************************/


public class Authenticator extends SimpleAuthenticator
{
  /** log4j category */
  private static Logger log = Logger.getLogger(Authenticator.class);

  private static Map auth = null;
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
      // Check if the user is faculty
      if (ldap.isFaculty()) {
	loadAuthorizationTable(context);

	// Loop through each ou
	Iterator i = ldap.getUnits().iterator();
	while (i.hasNext()) {
	  String strUnit = (String)i.next();

	  // Loop through the auth table
	  Iterator j = auth.keySet().iterator();
	  while (j.hasNext()) {
	    Pattern p = (Pattern)j.next();
	    Matcher m = p.matcher(strUnit);

	    // Check for a match
	    if (m.matches()) {
	      sGroups.add(auth.get(p));
	      break;
	    }
	  }
	}
      }
    }
    catch (Exception e) {
      log.error(LogManager.getHeader(null,
				     "ldap_authorization",
				     "unable to get authorization information for ldap user"),
		e);
      
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
      if (auth == null || 
	  (dOldFileDate != null && dNewFileDate.after(dOldFileDate))) 
      {
	bSuccess = false;
	dOldFileDate = dNewFileDate;
	auth = new LinkedHashMap();

	BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fFile)));
	
	// Read through the file
	String strLine = null;
	while ((strLine = br.readLine()) != null) {
	  strLine = strLine.trim();
	  if (strLine.equals("") || strLine.startsWith("#")) {
	    continue;
	  }

	  try {
	    // Parse the line
	    StringTokenizer st = new StringTokenizer(strLine, ":");
	    if (st.countTokens() != 2) {
	      throw new Exception("Incorrect number of fields in line: " + strLine);
	    }

	    String strRegex = st.nextToken().trim();
	    String strGroup = st.nextToken().trim();
	    Group group = Group.findByName(context, strGroup);
	    if (group == null) {
	      throw new Exception("Unrecognized group: " + strGroup);
	    }

	    // Add the line
	    auth.put(Pattern.compile(strRegex), group);
	  }
	  catch (Exception e) {
	    log.error(LogManager.getHeader(null,
					   "ldap_authorization",
					   "Error reading line from ldap auth file: " + strLine + "\n" + e.getMessage()));
	  }
 
	}
	bSuccess = true;
      }
    }
    finally {
      if (! bSuccess) {
	auth = null;
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



