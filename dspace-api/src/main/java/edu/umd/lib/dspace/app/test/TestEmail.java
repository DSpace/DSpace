package edu.umd.lib.dspace.app.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;

import java.util.ArrayList;

import javax.activation.DataSource;
import javax.activation.DataHandler;

import javax.mail.internet.MimeBodyPart;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;

import edu.umd.lib.activation.SimpleDataSource;

public class TestEmail {

  /***************************************************************** main */
  /**
   * Command line interface.
   */

  public static void main(String args[]) throws Exception {
	 ArrayList l = new ArrayList();
	 l.add("\"benjamin \"\"clay\"\" wallberg\",b,c\n");
	 l.add("x,y,z\n");
	 l.add("m,n,o");
	 SimpleDataSource ds = new SimpleDataSource(l, "text/csv", "etd2.csv");

	 Email bean = ConfigurationManager.getEmail("duplicate_title");

	 bean.addRecipient("wallberg@umd.edu");

	 bean.addArgument("title");
	 bean.addArgument("item-id");
	 bean.addArgument("handle");
	 bean.addArgument("collections");

	 MimeBodyPart part = new MimeBodyPart();
	 part.setDataHandler(new DataHandler(ds));

	 part.setFileName("etd2.csv");
	 
	 bean.addAttachment(part);

	 bean.send();

  }
}
