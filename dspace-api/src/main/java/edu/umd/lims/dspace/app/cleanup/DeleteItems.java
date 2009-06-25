/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lims.dspace.app.cleanup;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Set;
import java.util.StringTokenizer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.text.SimpleDateFormat;

// IO
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.StringWriter;

// Log4J
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// DSpace
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Email;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.FormatIdentifier;
import org.dspace.content.InstallItem;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;

import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;

import org.dspace.handle.HandleManager;


// Lims
import edu.umd.lims.util.ErrorHandling;


/*********************************************************************
 Delete items. Input is list of item handles on stdin.

 @author  Ben Wallberg

*********************************************************************/

public class DeleteItems {

  private static Logger log = Logger.getLogger(DeleteItems.class);

  static long lRead = 0;
  static long lDeleted = 0;


  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception 
  {

    Context context = new Context();
    context.setIgnoreAuthorization(true);

    try {

      // Properties
      Properties props     = System.getProperties();

      // dspace dir
      String strDspace     = ConfigurationManager.getProperty("dspace.dir");

      // logging (log4j.defaultInitOverride needs to be set or
      // config/log4j.properties will be read and used additionally)
      PropertyConfigurator.configure(strDspace + "/config/log4j-app.properties");
      // Open the reader
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

      // Loop through the lines
      String strLine = null;

      while ((strLine = br.readLine()) != null) {
        lRead++;
        String strHandle = strLine.trim();

        Item item = (Item)HandleManager.resolveToObject(context, strHandle);

        if (item == null) {
          log.info(strHandle + ": not found");
        } else {
          DCValue dcval[] = item.getDC("title", null, Item.ANY);
          String strTitle = dcval[0].value;

          // Remove from all collection; will be deleted when removed from
          // last collection
          Collection[] cs = item.getCollections();
          for (int i=0; i < cs.length; i++) {
            Collection c = cs[i];
            c.removeItem(item);
          }

          context.commit();

          log.info(strHandle + ": deleted: " + strTitle);
          lDeleted++;
        }
      }
    }

    catch (Exception e) {
      log.error("Uncaught exception: " + ErrorHandling.getStackTrace(e));
      System.exit(1);
    }

    finally {
      if (context != null) {
        try { context.complete(); } catch (Exception e) {}
      }

      log.info("=====================================\n" +
	       "Items read:    " + lRead + "\n" +
	       "Items deleted: " + lDeleted + "\n");

    }

    System.exit(0);
  }
}



