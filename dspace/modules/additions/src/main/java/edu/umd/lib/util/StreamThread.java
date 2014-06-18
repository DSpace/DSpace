/*
 * Copyright (c) 2006 The University of Maryland. All Rights Reserved.
 * 
 */

package edu.umd.lib.util;

import java.io.InputStream;
import java.io.OutputStream;

/**********************************************************************
 Provide a Thread which reads from an InputStream and writes to an
 OutputStream.  Errors are written to stderr.
**********************************************************************/

public class StreamThread extends Thread {

  InputStream in = null;
  OutputStream out = null;


  /********************************************************* StreamThread */
  /**
   * Constructor.
   */
  
  public StreamThread(InputStream in, OutputStream out) {
    this.in = in;
    this.out = out;
  }


  /****************************************************************** run */
  /**
   * Run the logic of the thread.
   */
  
  public void run() {
    try {
      byte b[] = new byte[2048];
      int n;
      while ((n = in.read(b)) != -1) {
	out.write(b, 0, n);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
	if (out != null) {
	  out.close();
	}
      }
      catch (Exception e) {
	e.printStackTrace();
      }
    }
  }
}


