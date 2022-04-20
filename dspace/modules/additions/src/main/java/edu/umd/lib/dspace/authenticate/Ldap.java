/*
 * Copyright (c) 2004 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lib.dspace.authenticate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
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
import org.dspace.content.MetadataSchema;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.Unit;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.eperson.service.UnitService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;


/*********************************************************************
 Use Ldap to provide authorizations for CAS authentication.

 @author  Ben Wallberg

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
    
  private final static ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

  private final static EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();

  private final static GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();   
  
  // Begin UMD Customization
  private final static UnitService unitService = EPersonServiceFactory.getInstance().getUnitService();   
  // End UMD Customization
  
  /**
  * Wild card for Dublin Core metadata qualifiers/languages
  */
 public static final String ANY = "*";


  /******************************************************************* Ldap */
  /**
   * Create an ldap connection
   */

  public 
  Ldap(org.dspace.core.Context context)
    throws NamingException
  {
    this.context = context;

    String strUrl = configurationService.getProperty("drum.ldap.url");
    String strBindAuth = configurationService.getProperty("drum.ldap.bind.auth");
    String strBindPassword =  configurationService.getProperty("drum.ldap.bind.password");
    String strConnectTimeout =  configurationService.getProperty("drum.ldap.connect.timeout");
    String strReadTimeout =  configurationService.getProperty("drum.ldap.read.timeout");

    // Setup the JNDI environment
    Properties env = new Properties();

    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.REFERRAL, "follow");

    env.put(Context.PROVIDER_URL, strUrl);
    env.put(Context.SECURITY_PROTOCOL, "ssl");
    env.put(javax.naming.Context.SECURITY_PRINCIPAL, strBindAuth);
    env.put(javax.naming.Context.SECURITY_CREDENTIALS, strBindPassword); 
    env.put("com.sun.jndi.ldap.connect.timeout", strConnectTimeout);
    env.put("com.sun.jndi.ldap.read.timeout", strReadTimeout);

    // Create the directory context
    log.debug("Initailizing new LDAP context");
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
      log.warn(LogHelper.getHeader(context,
                                    "null returned on ctx.search for " + strFilter,
                                    ""));
      return false;
    }

    // Check for a match
    if (!entries.hasMore()) {
      log.debug(LogHelper.getHeader(context,
                                     "no matching entries for " + strFilter,
                                     ""));
      return false;
    }

    // Get entry
    entry = (SearchResult)entries.next();
    log.debug(LogHelper.getHeader(context,
                                   "matching entry for " + strUid + ": " + entry.getName(),
                                   ""));

    // Check for another match
    if (entries.hasMore()) {
      entry = null;
      log.warn(LogHelper.getHeader(context,
                                    "multiple matching entries for " + strFilter,
                                    ""));
      return false;
    }

    log.debug(LogHelper.getHeader(context,
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
    if (checkAdmin(strPassword)) {
      log.info(LogHelper.getHeader(context,
                                    "admin password override for uid=" + strUid,
                                    ""));
      return true;
    }

    if (ctx == null || entry == null)
      return false;

    String strCompare = "(userpassword=" + strPassword.trim() + ")";

    // Set up search controls
    SearchControls sc = new SearchControls();
    sc.setReturningAttributes(new String[0]);       // return no attrs
    sc.setSearchScope(SearchControls.OBJECT_SCOPE); // search object only

    // Perform the compare
    NamingEnumeration compare = ctx.search(entry.getName(), strCompare, sc);

    // Make sure we got something
    if (compare == null) {
      log.warn(LogHelper.getHeader(context,
                                    "compare on userpassword failed for " + strUid,
                                    ""));
      return false;
    }

    boolean ret = compare.hasMore();

    log.debug(LogHelper.getHeader(context,
                                   "password compared '" + ret + "' for uid=" + strUid,
                                   ""));
    return ret;
  }


  /*********************************************************** checkAdmin */
  /**
   * Check for an admin user override.
   */

  public boolean
  checkAdmin(String strLdapPassword)
  {
    try {
      int i;
      if ((i = strLdapPassword.indexOf(':')) > -1) {
        // Extract email, password
        String strEmail = strLdapPassword.substring(0,i);
        String strPassword = strLdapPassword.substring(i+1);

        // Find the eperson
        EPerson eperson = epersonService.findByEmail(context, strEmail.toLowerCase());
        if (eperson != null && epersonService.checkPassword(context, eperson, strPassword)) {
          // Is the eperson an admin?
          if (groupService.isMember(context, eperson, Group.ADMIN)) {
            return true;
          }
        }
      }
    }
    catch (Exception e) {
      log.error(LogHelper.getHeader(context,
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

  public List<String> getAttributeAll(String strName)
  throws NamingException
  {
    List<String> attributes = new ArrayList<>();

    if (entry != null) {
      Attributes as = entry.getAttributes();
      Attribute a = as.get(strName);

      if (a != null) {
        NamingEnumeration e = a.getAll();

        while (e.hasMore()) {
            attributes.add((String)e.next());
        }
      }
    }

    return attributes;
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

  public List<String> getUnits()
  throws NamingException
  {
    return getAttributeAll("ou");
  }


  /************************************************************* getGroups */
  /**
   * Groups mapped by the Units for faculty.
   */

  public List<Group> getGroups() throws NamingException, java.sql.SQLException 
  {
    HashSet<Group> ret = new HashSet();

    for (Iterator i = getUnits().iterator(); i.hasNext(); ) {
      String strUnit = (String) i.next();

      Unit unit = unitService.findByName(context, strUnit);

      if (unit != null && (!unit.getFacultyOnly() || isFaculty())) {
        ret.addAll(unit.getGroups());
      }
    }

    return new ArrayList<Group>(ret);
  }


  /************************************************************ isFaculty */
  /**
   * is the user CP faculty with an acceptable status?
   */

  public boolean isFaculty()
  throws NamingException
  {
    if (strUid.equals("tstusr2")) {
      return true;
    }

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


  /****************************************************** registerEPerson */
  /**
   * Register this ldap user as an EPerson
   */

  public EPerson registerEPerson(String uid) throws Exception {
    // Save the current dspace user
    EPerson user = context.getCurrentUser();

    try {
      // Use the admin account to create the eperson
      EPerson admin = epersonService.findByEmail(context, "ldap_um@drum.umd.edu");
      context.setCurrentUser(admin);

      // Create a new eperson
      EPerson eperson = epersonService.create(context);
                        
      String strFirstName = getFirstName();
      if (strFirstName == null)
        strFirstName = "??";

      String strLastName = getLastName();
      if (strLastName == null)
        strLastName = "??";

      String strPhone = getPhone();
      if (strPhone == null)
        strPhone = "??";

      eperson.setNetid(uid);
      eperson.setEmail(uid + "@umd.edu");
      eperson.setFirstName(context, strFirstName);
      eperson.setLastName(context, strLastName);
      epersonService.setMetadata(context, eperson, "phone", strPhone);
      eperson.setCanLogIn(true);
      eperson.setRequireCertificate(false);

      epersonService.update(context, eperson);
      context.commit();
                        
      log.info(LogHelper.getHeader(context,
                                    "create_um_eperson",
                                    "eperson_id="+eperson.getID() +
                                    ", uid=" + strUid));

      return eperson;
    }

    finally {
      context.setCurrentUser(user);
    }                        
  }


  /*********************************************************** setContext */
  /**
   * Reset the context.  We lost it after every request.
   */

  public void setContext(org.dspace.core.Context context) {
    this.context = context;
  }

  
  public String toString() {
	  if (entry == null) return "null";
	  
	  return strUid + " (" + entry.getName() + ")";
  }

}


