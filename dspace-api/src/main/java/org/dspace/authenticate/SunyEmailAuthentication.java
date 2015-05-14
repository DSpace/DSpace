/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/*
* SunyEmailAuthentication.java
*
*/
package org.dspace.authenticate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;


/**
* Identify members of "SUNY Community" and give them membership in
* a special group.
*
* <p>
* 1. When a user logs in, put them in the special SUNY group (so
* they get access to materials restricted to the SUNY community).
* The membership test is by email address domain.
* <p>
*
* Note that this method does <strong>not</strong> actually authenticate
* anyone, it just adds a special group. With stackable authentication it
* can do its work from within the stack and let other methods handle
* the authentication.
*
* @author Nathan Fixler
* @author Benjamin Follis
*
*/
public class SunyEmailAuthentication implements AuthenticationMethod {

        /** log4j category */
  private static Logger log = Logger.getLogger(SunyEmailAuthentication.class);

  /**
* Name of DSpace group to which SUNY-community clients are
* automatically added. The DSpace Admin must create this group.
*/
        public static final String SUNY_GROUPNAME = "SUNY Users";
        public static Hashtable sunyGroups = new Hashtable();

        public SunyEmailAuthentication()
        {
                sunyGroups = new Hashtable();
                Enumeration e = ConfigurationManager.propertyNames();

                while(e.hasMoreElements())
                {
                        String propName = (String) e.nextElement();
                        if (propName.startsWith("authentication.emaildomain."))
                        {
                                String[] nameParts = propName.split("\\.");
                                if (nameParts.length == 3)
                                {
                                        addMatchers(nameParts[2], ConfigurationManager.getProperty(propName));
                                } else {
                                        log.warn("Malformed configuration property name: " + propName);
                                }
                        }
                }
        }

        private void addMatchers(String groupName, String emailDomain)
        {
                String[] domainParts = emailDomain.split(",");
                for (int i = 0; i < domainParts.length; i++) {
                        sunyGroups.put(domainParts[i].trim(), groupName);
                }
  }

        /**
* We don't care about self registering here.
* Let a real auth method return true if it wants.
*/
        public boolean canSelfRegister(Context context,
                                                HttpServletRequest request,
                                  String username)
                throws SQLException
    {
        return false;
    }

  /**
* Initialize new EPerson.
*/
  public void initEPerson(Context context,
                          HttpServletRequest request,
                          EPerson eperson)
    throws SQLException
          {
          }

  /**
* Predicate, is user allowed to set EPerson password.
*/
  public boolean allowSetPassword(Context context,
                                  HttpServletRequest request,
                                  String username)
        throws SQLException
          {
        return true;
          }

        /*
* This is an implicit method, although it doesn't do authentication.
* The email-based checks should be run in the implicit stack.
*/
        public boolean isImplicit()
        {
                return true;
        }

  /**
* Add user to special SUNY group if they're a member of SUNY community.
*/
  public int[] getSpecialGroups(Context context, HttpServletRequest request)
  {
    String emailGroupName = null;
    EPerson user = context.getCurrentUser();
    if (user != null) {
                        emailGroupName = emailToGroup(user.getEmail());
    }

    try {
                        Group sunyGroup = Group.findByName(context, SUNY_GROUPNAME);

                        if (emailGroupName != null) {
                                Group emailGroup = Group.findByName(context, emailGroupName);
  
                          if (emailGroup == null) {
                                        log.warn(LogManager.getHeader(context,
                                                "No Group found!! Admin needs to create group named \""+
                                                 emailGroupName+"\"", ""));
                                        return new int[0];
                                } else {
                                  return new int[] {emailGroup.getID(), sunyGroup.getID()};
                                }
                        } else if (emailGroupName != null) {
                                Group emailGroup = Group.findByName(context, emailGroupName);
                                if (emailGroup == null) {
                                        log.warn(LogManager.getHeader(context,
                                          "No Group found!! Admin needs to create group named \""+
                                           emailGroupName+"\"", ""));
                                        return new int[0];
                                } else {
                                        return new int[] {emailGroup.getID(), sunyGroup.getID()};
                                }
      } else {
              log.debug(LogManager.getHeader(context, "getSpecialGroups",
                  "Not an SUNY user, no groups for you."));
      }
    }
                catch (java.sql.SQLException e)
                {
                }
                return new int[0];
        }

        /**
* This method is not used.
* This class is only for special groups and enforcement of cert policy.
* Use X509Authentication to authenticate.
*
* @return One of: SUCCESS, BAD_CREDENTIALS, NO_SUCH_USER, BAD_ARGS
*/
        public int authenticate(Context context,
                          String username,
                          String password,
                          String realm,
                          HttpServletRequest request)
          throws SQLException
        {
          return BAD_ARGS;
  }

  /*
* Returns URL to which to redirect to obtain credentials (either password
* prompt or e.g. HTTPS port for client cert.); null means no redirect.
*
* @param context
* DSpace context, will be modified (ePerson set) upon success.
*
* @param request
* The HTTP request that started this operation, or null if not applicable.
*
* @param response
* The HTTP response from the servlet method.
*
* @return fully-qualified URL
*/
        public String loginPageURL(Context context,
                             HttpServletRequest request,
                             HttpServletResponse response)
  {
          return null;
  }
  public String loginPageTitle(Context context)
  {
          return null;
  }

        /**
* Return the user's group associated with their email address' domain.
* Returns null if it cannot find the domain.
* @param request email address
*
* @return group name or null
*/
        private String emailToGroup(String email)
        {
                String retval = null;
                email = email.toLowerCase();
                if (email != null) {
                        //this is a bastard way to do it but is easy to think about
                        String[] subemail = email.split("@");
                        int subemailLength = subemail.length;
                        if (subemailLength == 2) {
                                String domain = subemail[1];
                                if (sunyGroups.containsKey(domain)) {
                                        retval = (String)sunyGroups.get(domain);
                                }
                        }
                }
                return retval;
        }
}
