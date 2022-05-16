/*
 * Copyright (c) 2006 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lib.dspace.app.mediafilter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.dspace.app.mediafilter.MediaFilter;
import org.dspace.content.Item;

import edu.umd.lib.util.StreamThread;

/*
 * 
 */

public class PostscriptFilter extends MediaFilter {

  String strOldFileName = "";


  /******************************************************* getFilteredName */
  /**
   * @param filename
   *            string filename
   * 
   * @return string filtered filename
   */

  public String getFilteredName(String strOldFileName) {
    this.strOldFileName = strOldFileName;

    if (strOldFileName.toLowerCase().endsWith(".ps")) {
      strOldFileName = strOldFileName.substring(0, strOldFileName.length()-3);
    }

    return strOldFileName + ".pdf";
  }


  /******************************************************** getBundleName */
  /**
   * @return String bundle name
   *  
   */

  public String getBundleName() {
    return "ORIGINAL";
  }


  /****************************************************** getFormatString */
  /**
   * @return String bitstreamformat
   */

  public String getFormatString() {
    return "Adobe PDF";
  }


  /******************************************************* getDescription */
  /**
   * @return String description
   */

    public String getDescription() {
      return "Auto-generated copy of " + strOldFileName;
    }


  /************************************************* getDestinationStream */
  /**
   * @param source
   *            source input stream
   * 
   * @return InputStream the resulting input stream
   */

  @Override
  public InputStream getDestinationStream(Item item, InputStream source, boolean verbose) throws Exception {
    // Start the process
    Process p = Runtime.getRuntime().exec(new String[]{"/usr/local/bin/ps2pdf","-","-"});
       
    // Create a thread to send the input to the process
    StreamThread tin = new StreamThread(source, p.getOutputStream());
    tin.start();

    // Create a thread to read the output from the process
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamThread tout = new StreamThread(p.getInputStream(), baos);
    tout.start();

    // Wait for completion
    p.waitFor();
    tin.join();
    tout.join();

    // Get the return value
    if (p.exitValue() != 0) {
      throw new Exception("ps2pdf returned an error: " + p.exitValue());
    }

    // Return the pdf
    return new ByteArrayInputStream(baos.toByteArray());
  }
}

