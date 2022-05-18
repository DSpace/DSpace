package edu.umd.lib.activation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;

import java.util.Iterator;
import java.util.List;

import javax.activation.DataSource;


public class SimpleDataSource implements DataSource {

  private String strSource = null;
  private String strContentType = null;
  private String strName = null;

  public SimpleDataSource(String strSource, String strContentType, String strName) {
	 this.strSource = strSource;
	 this.strContentType = strContentType;
	 this.strName = strName;
  }

  public SimpleDataSource(List lSource, String strContentType, String strName) {
	 this.strContentType = strContentType;
	 this.strName = strName;

	 StringBuffer sb = new StringBuffer();
	 for (Iterator i = lSource.iterator(); i.hasNext(); ) {
		sb.append(i.next());
	 }

	 strSource = sb.toString();
  }

  public InputStream getInputStream() throws IOException {
	 return new ByteArrayInputStream(strSource.getBytes("UTF-8"));
  }

  public OutputStream getOutputStream() throws IOException {
	 throw new IOException("getOutputStream() is not supported");
  }
		  
  public String getContentType() {
	 return strContentType;
  }

  public String getName() {
	 return strName;
  }
}
