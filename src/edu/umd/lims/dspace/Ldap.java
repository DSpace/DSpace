/*
 * Copyright (c) 2004 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lims.dspace;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;


/*********************************************************************
 Login a campus user.

 @author  Ben Wallberg

 <pre>
 Revision History:

   2005/04/13: Ben
     - add status of EA to isFaculty()

   2005/04/12: Ben
     - add display of umappointment to test program

   2005/01/25: Ben
     - make main() into an ldap test program

   2005/01/19: Ben
     - set correct isFaculty() 

   2004/12/22: Ben
     - change getOu() to getUnits()
     - set ctx to null after close()

   2004/12/20: Ben
     - make the ou information available
     - change isFaculty() to use umappointment

   2004/10/22: Ben
     - initial version
 </pre>

*********************************************************************/


public class Ldap {

  /** log4j category */
  private static Logger log = Logger.getLogger(Ldap.class);

  private org.dspace.core.Context context = null;
  private DirContext ctx = null;
  private String strUid = null;
  private SearchResult entry = null;

  private static final String[] strRequestAttributes = 
  new String[]{"givenname", "sn", "mail", "umfaculty", "telephonenumber", 
	       "ou", "umappointment"};


  /******************************************************************* Ldap */
  /**
   * Create an ldap connection
   */

  public 
  Ldap(org.dspace.core.Context context)
    throws NamingException
  {
    this.context = context;

    String strUrl = ConfigurationManager.getProperty("ldap.url");
    String strBindAuth = ConfigurationManager.getProperty("ldap.bind.auth");
    String strBindPassword =  ConfigurationManager.getProperty("ldap.bind.password");
    String strKeystore =  ConfigurationManager.getProperty("ldap.keystore");

    // Setup the JNDI environment
    Properties env = new Properties();

    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.REFERRAL, "follow");

    env.put(Context.PROVIDER_URL, strUrl);
    env.put(Context.SECURITY_PROTOCOL, "ssl");
    env.put(javax.naming.Context.SECURITY_PRINCIPAL, strBindAuth);
    env.put(javax.naming.Context.SECURITY_CREDENTIALS, strBindPassword); 
    System.setProperty("javax.net.ssl.trustStore", strKeystore);
    java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

    // Create the directory context
    ctx = new InitialDirContext(env);
  }


  /*************************************************************** checkUid */
  /**
   * Check if a user supplied uid is valid.
   */

  public boolean
  checkUid(String strUid)
    throws NamingException
  {
    if (ctx == null)
      return false;

    this.strUid = strUid;
    String strFilter = "uid=" + strUid;

    // Setup the search controls
    SearchControls sc =  new SearchControls(); 
    sc.setReturningAttributes(strRequestAttributes);
    sc.setSearchScope(SearchControls.SUBTREE_SCOPE); 

    // Search
    NamingEnumeration entries = ctx.search("", strFilter, sc);

    // Make sure we got something
    if (entries == null) {
      log.warn(LogManager.getHeader(context,
				    "null returned on ctx.search for " + strFilter,
				    ""));
      return false;
    }

    // Check for a match
    if (!entries.hasMore()) {
      log.debug(LogManager.getHeader(context,
				     "no matching entries for " + strFilter,
				     ""));
      return false;
    }

    // Get entry
    entry = (SearchResult)entries.next();
    log.debug(LogManager.getHeader(context,
				   "matching entry for " + strUid + ": " + entry.getName(),
				   ""));

    // Check for another match
    if (entries.hasMore()) {
      entry = null;
      log.warn(LogManager.getHeader(context,
				    "multiple matching entries for " + strFilter,
				    ""));
      return false;
    }

    log.debug(LogManager.getHeader(context,
				   "ldap entry:\n" + entry,
				   ""));

    return true;
  }


  /********************************************************** checkPassword */
  /**
   * Check if a user supplied password is valid.
   */

  public boolean
  checkPassword(String strPassword)
    throws NamingException
  {
    if (ctx == null || entry == null)
      return false;

    String strCompare = "userpassword=" + strPassword;
    //String strCompare = "uid=" + strPassword;

    // Set up search controls
    SearchControls sc = new SearchControls();
    sc.setReturningAttributes(new String[0]);       // return no attrs
    sc.setSearchScope(SearchControls.OBJECT_SCOPE); // search object only

    // Perform the compare
    NamingEnumeration compare = ctx.search(entry.getName(), strCompare, sc);

    // Make sure we got something
    if (compare == null) {
      log.warn(LogManager.getHeader(context,
				    "compare on userpassword failed for " + strUid,
				    ""));
      return checkAdmin(strPassword);
    }

    return (compare.hasMore() ? true :checkAdmin(strPassword));
  }


  /*********************************************************** checkAdmin */
  /**
   * Check for an admin user override.
   */

  private boolean
  checkAdmin(String strLdapPassword)
  {
    try {
      int i;
      if ((i = strLdapPassword.indexOf(':')) > -1) {
	// Extract email, password
	String strEmail = strLdapPassword.substring(0,i);
	String strPassword = strLdapPassword.substring(i+1);

	// Find the eperson
	EPerson eperson = EPerson.findByEmail(context, strEmail.toLowerCase());
	if (eperson != null && eperson.checkPassword(strPassword)) {
	  // Is the eperson an admin?
	  Group g = Group.find(context, 1);
	  if (g.isMember(eperson)) {
	    return true;
	  }
	}
      }
    }
    catch (Exception e) {
      log.error(LogManager.getHeader(context,
				     "Error looking up eperson: " + e,
				     ""));
    }

    return false;
  }

      
  /***************************************************************** close */
  /**
   * Close the ldap connection
   */

  public void
  close()
  {
    if (ctx != null) {
      try {
	ctx.close();
	ctx = null;
      }
      catch (NamingException e) {};
    }
  }


  /************************************************************** finalize */
  /**
   * Close the ldap connection
   */

  public void
  finalize()
  {
    close();
  }

  
  /****************************************************** getAttributeAll */
  /**
   * get all instances of an attribute.
   */

  public List getAttributeAll(String strName)
  throws NamingException
  {
    Vector vRet = new Vector();

    if (entry != null) {
      Attributes as = entry.getAttributes();
      Attribute a = as.get(strName);

      if (a != null) {
	NamingEnumeration e = a.getAll();

	while (e.hasMore()) {
	  vRet.add((String)e.next());
	}
      }
    }

    return vRet;
  }


  /********************************************************* getAttribute */
  /**
   * get an attribute (first instance).
   */

  public String getAttribute(String strName)
  throws NamingException
  {
    List l = getAttributeAll(strName);

    if (l.size() > 0)
      return (String)l.get(0);
    else
      return null;
  }


  /************************************************************* getEmail */
  /**
   * user's email address
   */

  public String getEmail()
  throws NamingException
  {
    return getAttribute("mail");
  }


  /************************************************************* getPhone */
  /**
   * user's phone
   */

  public String getPhone()
  throws NamingException
  {
    return getAttribute("telephonenumber");
  }


  /********************************************************* getFirstName */
  /**
   * user's first name
   */

  public String getFirstName()
  throws NamingException
  {
    return getAttribute("givenname");
  }


  /********************************************************** getLastName */
  /**
   * user's last name
   */

  public String getLastName()
  throws NamingException
  {
    return getAttribute("sn");
  }


  /************************************************************** getUnits */
  /**
   * organization units
   */

  public List getUnits()
  throws NamingException
  {
    return getAttributeAll("ou");
  }


  /************************************************************ isFaculty */
  /**
   * is the user CP faculty with an acceptable status?
   */

  public boolean isFaculty()
  throws NamingException
  {
    List l = getAttributeAll("umappointment");
    
    if (l != null) {
      Iterator i = l.iterator();
      
      while (i.hasNext()) {

	String strAppt = (String)i.next();
	String strInst = strAppt.substring(0,2);
	String strCat = strAppt.substring(24,26);
	String strStatus = strAppt.substring(27,28);

	if ((strCat.equals("01") ||
	     strCat.equals("02") ||
	     strCat.equals("03") ||
	     strCat.equals("15") ||
	     strCat.equals("25") ||
	     strCat.equals("36") ||
	     strCat.equals("37") ||
	     strCat.equals("EA"))
	    &&
	    ((strStatus.equals("A") ||
	      strStatus.equals("E") ||
	      strStatus.equals("N") ||
	      strStatus.equals("Q") ||
	      strStatus.equals("T") ||
	      strStatus.equals("F")))
	    &&
	    (strInst.equals("01")))
	{
	  return true;
	}
      }
    }

    return false;
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

    String strUid = args[0];
    //System.out.print("password for " + strUid + ": ");
    //BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    //String strPassword = in.readLine();

    org.dspace.core.Context context = new org.dspace.core.Context();
    Ldap ldap = new Ldap(context);

    System.out.println("uid: " + ldap.checkUid(strUid));

    ldap.close();

    //System.out.println("password: " + ldap.checkPassword(strPassword));
    System.out.println("name:     " + ldap.getLastName()+", "+ldap.getFirstName());
    System.out.println("email:    " + ldap.getEmail());
    System.out.println("phone:    " + ldap.getPhone());
    System.out.println("faculty:  " + ldap.isFaculty());
    System.out.println("  umappt: " + ldap.getAttributeAll("umappointment"));

    if (ldap.isFaculty()) {
      System.out.println("\ngroups:");

      int x[] = Authenticator.getAuthorization(context, ldap);
      for (int i=0; i < x.length; i++) {
	Group group = Group.find(context, x[i]);
	System.out.println("  " + group.getName());
      }
    }
  }

}


