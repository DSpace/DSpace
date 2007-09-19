/*
 * Copyright (c) 2007 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lims.dspace.app;

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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.text.SimpleDateFormat;

// SQL
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;

import org.dspace.handle.HandleManager;

// Lims
import edu.umd.lims.util.ErrorHandling;


/*********************************************************************
 Update view statistics using the output from analog.

 @author  Ben Wallberg

*********************************************************************/

public class UpdateStats {

  private static Logger log = Logger.getLogger(UpdateStats.class);

  static long lItems = 0;
  static long lBitstreams = 0;

  private static Pattern pStat = Pattern.compile("^ *(\\d+): /dspace/(handle|bitstream)/(\\d+[^/]*/\\d+)(/(\\d+))?");


  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception {

    Context context = null;

    try {

      // Properties
      Properties props     = System.getProperties();

      // dspace dir
      String strDspace     = ConfigurationManager.getProperty("dspace.dir");

      // logging (log4j.defaultInitOverride needs to be set or
      // config/log4j.properties will be read and used additionally)
      PropertyConfigurator.configure(strDspace + "/config/log4j-app.properties");
      // open the stat input file
      String strFile = strDspace + "/stats/views.txt";
      FileInputStream fis = new FileInputStream(new File(strFile));
      BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
      log.info(strFile + " opened for reading");

      // Setup context
      context = new Context();
      context.setIgnoreAuthorization(true);

      // Read through each line in the file
      String strLine = null;
      while ((strLine = br.readLine()) != null) {

	Matcher mStat = pStat.matcher(strLine);

	if (mStat.find()) {
	  String strCount  = mStat.group(1);
	  String strType   = mStat.group(2);
	  String strHandle = mStat.group(3);

	  int nCount = Integer.parseInt(strCount);

	  DSpaceObject dso = HandleManager.resolveToObject(context, strHandle);

	  if (dso == null) {
	    // handle not found
	    log.warn("Unrecognized handle: " + strHandle);

	  } else if (dso.getType() == Constants.COLLECTION ||
		     dso.getType() == Constants.COMMUNITY) {
	    // ignore collections and communities

	  } else {
	    Item item = (Item)dso;

	    if (strType.equals("handle")) {
	      log.debug("Item: "
			+ "count=" + strCount
			+ ", handle=" + strHandle
			);

	      int nItemCount = item.getIntMetadata("views");
	      nItemCount += nCount;
	      item.setMetadata("views", nItemCount);
	      item.update();

	      lItems++;

	    } else {
	      // bitstream
	      String strSeq = mStat.group(5);
	      
	      if (strSeq != null) {
		int nSeq = Integer.parseInt(strSeq);

		log.debug("Bitstream: "
			  + "count=" + strCount
			  + ", handle=" + strHandle
			  + ", sequence=" + strSeq
			  );

		boolean found = false;

		// Find the bitstream
		Bitstream bs = null;
		Bundle[] bundles = item.getBundles();
		
		for (int i = 0; (i < bundles.length) && !found; i++) {
		  Bitstream[] bitstreams = bundles[i].getBitstreams();
		  for (int k = 0; (k < bitstreams.length) && !found; k++) {
		    if (nSeq == bitstreams[k].getSequenceID()) {
		      bs = bitstreams[k];
		      found = true;
		    }
		  }
		}

		if (bs == null) {
		  log.warn("Unrecognized bitstream sequence: "+strHandle+"/"+strSeq);
		} else {
		  int nItemCount = bs.getIntMetadata("views");
		  nItemCount += nCount;
		  bs.setMetadata("views", nItemCount);
		  bs.update();
		
		  lBitstreams++;
		}
	      }
	    }

	    
	  }
	}

      }

      // commit
      log.info("Committing changes");
      context.commit();

      // Close the input file
      br.close();

    }

    catch (Exception e) {
      if (context != null) {
	context.abort();
      }

      log.error(ErrorHandling.getStackTrace(e));

      System.exit(1);
    }

    finally {
      if (context != null) {
	try { context.complete(); } catch (Exception e) {}
      }
      
      log.info("=====================================\n" +
	       "Items updated:      " + lItems + "\n" +
	       "Bitstreams updated: " + lBitstreams + "\n"
	       );
    }

    System.exit(0);
  }
}



