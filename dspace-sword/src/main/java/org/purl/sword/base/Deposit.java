/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

/**
 * Represents a deposit. 
 * 
 * @author Stuart Lewis 
 */
public class Deposit 
{
    private static final Logger log = Logger.getLogger(Deposit.class);
   
   /** The File deposited */
   private File file;
   
   /** The content type */
   private String contentType;
   
   /** The content length */
   private int contentLength;
   
   /** The username */
   private String username;
   
   /** The password */
   private String password;
   
   /** The onBehalfOf value */
   private String onBehalfOf;
   
   /** The slug string */
   private String slug;
   
   /** MD5 hash */
   private String md5;
   
   /** True if verbose should be used */
   private boolean verbose;
   
   /** True if this is a no-operation command */
   private boolean noOp;
   
   /** The packaging format */
   private String packaging;
   
   /** Deposit ID */
   private String depositID;
   
   /** The IP address */
   private String IPAddress;
   
   /** The location */
   private String location;
     
   /** The content disposition */
   private String contentDisposition; 
   
   /**
    * Submission created
    */
   public static final int CREATED = HttpServletResponse.SC_CREATED;
   
   /**
    * Submission accepted.
    */
   public static final int ACCEPTED = HttpServletResponse.SC_ACCEPTED; 

   /**
    * @return the authenticatedUserName
    */
   public String getUsername() {
      return username;
   }

   /**
    * @param username the authenticated UserName to set
    */
   public void setUsername(String username) {
      this.username = username;
   }

   /**
    * @return the authenticatedUserPassword
    */
   public String getPassword() {
      return password;
   }

   /**
    * @param password the password to set
    */
   public void setPassword(String password) {
      this.password = password;
   }

   /**
    * @return the contentLength
    */
   public int getContentLength() {
      return contentLength;
   }

   /**
    * @param contentLength the contentLength to set
    */
   public void setContentLength(int contentLength) {
      this.contentLength = contentLength;
   }

   /**
    * @return the contentType
    */
   public String getContentType() {
      return contentType;
   }

   /**
    * @param contentType the contentType to set
    */
   public void setContentType(String contentType) {
      this.contentType = contentType;
   }

   /**
    * @return the depositID
    */
   public String getDepositID() {
      return depositID;
   }

   /**
    * @param depositID the depositID to set
    */
   public void setDepositID(String depositID) {
      this.depositID = depositID;
   }

   /**
    * @return the file
    */
   public File getFile() {
      return file;
   }

   /**
    * @param file the file to set
    */
   public void setFile(File file) {
      this.file = file;
   }

   /**
    * @return the packaging
    */
   public String getPackaging() {
      return packaging;
   }

   /**
    * @param packaging the packaging to set
    */
   public void setPackaging(String packaging) {
      this.packaging = packaging;
   }

   /**
    * @return the md5
    */
   public String getMd5() {
      return md5;
   }

   /**
    * @param md5 the md5 to set
    */
   public void setMd5(String md5) {
      this.md5 = md5;
   }

   /**
    * @return the noOp
    */
   public boolean isNoOp() {
      return noOp;
   }

   /**
    * @param noOp the noOp to set
    */
   public void setNoOp(boolean noOp) {
      this.noOp = noOp;
   }

   /**
    * @return the onBehalfOf
    */
   public String getOnBehalfOf() {
      return onBehalfOf;
   }

   /**
    * @param onBehalfOf the onBehalfOf to set
    */
   public void setOnBehalfOf(String onBehalfOf) {
      this.onBehalfOf = onBehalfOf;
   }

   /**
    * @return the slug
    */
   public String getSlug() {
      return slug;
   }

   /**
    * @param slug the slug to set
    */
   public void setSlug(String slug) {
      this.slug = slug;
   }

   /**
    * @return the verbose
    */
   public boolean isVerbose() {
      return verbose;
   }

   /**
    * @param verbose the verbose to set
    */
   public void setVerbose(boolean verbose) {
      this.verbose = verbose;
   }
   
   /**
    * Get the IP address of the user
    * 
    * @return the IP address
    */
   public String getIPAddress() {
	   return IPAddress;
   }
   
   /**
    * Set the IP address of the user
    *
    * @param IPAddress the IP address
    */
   public void setIPAddress(String IPAddress) {
	   this.IPAddress = IPAddress;
   }
   
   /**
    * Get the location of the deposit
    * 
    * @return the location of the deposit
    */
   public String getLocation() {
	   return location;
   }
   
   /**
    * Set the location of the deposit
    *
    * @param location the location
    */
   public void setLocation(String location) {
	   this.location = location;
   }

   /**
    * Retrieve the filename that is associated with this deposit. This
    * is extracted from the content disposition value.  
    * 
    * @return The filename. 
    */
   public String getFilename() 
   {
	   String filename = null; // default return value
	   if( contentDisposition != null ) 
	   {
		   try
		   {
			   String filePattern = ".*filename=(.*?)((; *.*)|( +)){0,1}";
			   Pattern p = Pattern.compile(filePattern);
			   Matcher m = p.matcher(contentDisposition);

			   if( m.matches() && m.groupCount() > 2 )
			   {
				   filename = m.group(1);
			   }
		   }
		   catch( Exception ex )
		   {
               log.error("Unable to extract filename", ex);
		   }
	   }
	   return filename; 
   }

   /**
    * Set the content disposition that is to be used for this deposit.  
    * This will include the filename, if specified.  
    *  
    * @param disposition The content disposition value. 
    */
   public void setContentDisposition(String disposition) 
   {
      this.contentDisposition = disposition;
   }
   
   /**
    * Return the content disposition value. 
    * 
    * @return The value. 
    */
   public String getContentDisposition()
   {
	   return this.contentDisposition;
   }
   
}
