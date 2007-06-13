/*
 * Copyright (c) 2006 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lims.dspace.app.mediafilter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.dspace.app.mediafilter.MediaFilter;

import edu.umd.lims.util.StreamThread;

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

  public InputStream getDestinationStream(InputStream source) throws Exception {
    PipedInputStream pin = new PipedInputStream();
    PipedOutputStream pout = new PipedOutputStream(pin);

    Convert t = new Convert(source, pout);
    t.start();

    return pin;
  }


  /**********************************************************************
   **********************************************************************/

  static class Convert extends Thread {

    InputStream in = null;
    OutputStream out = null;


    /************************************************************ Convert */
    /**
     * Constructor.
     */

    public Convert(InputStream in, OutputStream out) {
      this.in = in;
      this.out = out;
    }


   /**************************************************************** run */
   /**
    * Run the logic of the thread.
    */

   public void run() {
     // Not closing out (a PipedOutputStream) will cause an IOException
     // which will cause MediaFilter to abort the new bitstream creation
     boolean bCloseOut = true;

     try {
       // Start the process
       Process p = Runtime.getRuntime().exec(new String[]{"/usr/local/bin/ps2pdf","-","-"});
       
       // Create a thread to send the input to the process
       StreamThread t = new StreamThread(in, p.getOutputStream());
       t.start();

       // Read the results and send to the output
       InputStream pin = p.getInputStream();
       byte b[] = new byte[2048];
       int n;
       while ((n = pin.read(b)) != -1) {
	 out.write(b, 0, n);
       }

       // Wait for completion
       p.waitFor();

       // Get the return value
       if (p.exitValue() != 0) {
	 bCloseOut = false;
       }
     }       
     catch (Exception e) {
       e.printStackTrace();
       bCloseOut = false;
     }
     finally {
       try {
	 if (out != null && bCloseOut) {
	   out.close();
	 }
       }
       catch (Exception e) {
	 e.printStackTrace();
       }
     }
   }
  }
}

