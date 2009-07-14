/*
 * Copyright (c) 2007 The University of Maryland. All Rights Reserved.
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
 
 Single map of an ldap ou to a dspace group.

 @author  Ben Wallberg

*********************************************************************/


public class LdapMap {

  /**
   * ldap ou attribute
   */
  public String strOU = null;

  /**
   * dspace Group
   */
  public Group group = null;

  /**
   * is this mapping restricted to faculty?
   */
  public boolean bFaculty = true;


  /**************************************************************** LdapMap */
  /**
   * Contstructor.
   */

  public LdapMap(String strOU, Group group, boolean bFaculty) {
    this.strOU = strOU;
    this.group = group;
    this.bFaculty = bFaculty;
  }


  /************************************************************** toString */
  /**
   * String representation.
   */

  public String toString() {
    return 
      strOU 
      + ", " + group.getName() 
      + ", " + bFaculty
      ;
  }
}


