/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * List of options that are parsed from the command line. 
 * @author Neil Taylor
 */
public class ClientOptions
{
   /**
    * Label for the service operation. 
    */
   public static final String TYPE_SERVICE = "service";

   /**
    * Label for the post operation. 
    */
   public static final String TYPE_POST = "post";

   /**
    * Label for the multipost operation. 
    */
   public static final String TYPE_MULTI_POST = "multipost";

   /**
    * The access type. 
    */
   private String accessType = null; 

   /** 
    * Proxy host name. 
    */
   private String proxyHost = null;

   /**
    * Proxy host port. 
    */
   private int proxyPort = 8080; 

   /** 
    * Username to access the service/post server. 
    */
   private String username = null;

   /**
    * Password to access the service/post server.
    */
   private String password = null; 

   /**
    * HREF of the server to access. 
    */
   private String href = null;

   /** 
    * Filename to post. 
    */
   private String filename = null; 

   /**
    * Filetype. 
    */
   private String filetype = null;

   /**
    * Specifies that the output streams are not to be captured by the GUI client. 
    */
   private boolean noCapture = false; 
   
   
   /**
    * SLUG Header field.
    */
   private String slug = null; 

   /**
    * NoOp, used to indicate an operation on the server that does not 
    * require the file to be stored. 
    */
   private boolean noOp = false; 

   /**
    * Request verbose output from the server. 
    */
   private boolean verbose = false; 

   /** 
    * OnBehalfOf user id. 
    */
   private String onBehalfOf = null; 

   /**
    * Format namespace to be used for the posted file. 
    */
   private String formatNamespace = null; 

   /**
    * Introduce a checksum error. This is used to simulate an error with the 
    * MD5 value. 
    */
   private boolean checksumError = false;

   /**
    * Logger. 
    */
   private static Logger log = Logger.getLogger(ClientOptions.class);

   /**
    * List of multiple destination items. Used if the mode is set to multipost. 
    */
   private List<PostDestination> multiPost = new ArrayList<PostDestination>();

   /**
    * Pattern string to extract the data from a destination parameter in multipost mode. 
    */
   private static final Pattern multiPattern = Pattern.compile("(.*?)(\\[(.*?)\\]){0,1}(:(.*)){0,1}@(http://.*)");

   /**
    * Flag that indicates if the GUI mode has been set. This is 
    * true by default. 
    */
   private boolean guiMode = true; 

   /**
    * Flat that indicates if the MD5 option has been selected. This
    * is true by default. 
    */
   private boolean md5 = false; 

   /**
    * Parse the list of options contained in the specified array. 
    * 
    * @param args The array of options. 
    * 
    * @return True if the options were parsed successfully. 
    */
   public boolean parseOptions( String[] args )
   {
      try
      {
         // iterate over the args
         for( int i = 0; i < args.length; i++ )
         {

            if( "-md5".equals(args[i]))
            {
               md5 = true;
            }

            if( "-noOp".equals(args[i]))
            {
               noOp = true;
            }

            if( "-verbose".equals(args[i]))
            {
               verbose = true; 
            }

            if( "-cmd".equals(args[i]) )
            {
               guiMode = false;  
            }

            if( "-gui".equals(args[i]) )
            {
               guiMode = true;  
            }

            if( "-host".equals(args[i]) )
            {
               i++;
               proxyHost = args[i];
            }

            if( "-port".equals(args[i]) )
            {
               i++;
               proxyPort = Integer.parseInt(args[i]);
            }

            if( "-u".equals(args[i]) )
            {
               i++;
               username = args[i];
            }

            if( "-p".equals(args[i]))
            {
               i++;
               password = args[i];
            }

            if( "-href".equals(args[i]))
            {
               i++;
               href = args[i];
            }

            if( "-help".equals(args[i]) )
            {
               // force the calling code to display the usage information
               return false;     
            }

            if( "-t".equals(args[i]))
            {
               i++;
               accessType = args[i];
            }

            if( "-file".equals(args[i]))
            {
               i++; 
               filename = args[i];
            }

            if( "-filetype".equals(args[i]))
            {
               i++;
               filetype = args[i];
            }

            if( "-slug".equals(args[i]))
            {
               i++;
               slug = args[i]; 
            }

            if( "-onBehalfOf".equals(args[i]))
            {
               i++;
               onBehalfOf = args[i];
            }

            if( "-formatNamespace".equals(args[i]))
            {
               i++;
               formatNamespace = args[i];
            }

            if( "-checksumError".equals(args[i]))
            {
               i++;
               checksumError = true;
            }

            if( "-dest".equals(args[i]))
            {
               i++;
               Matcher m = multiPattern.matcher(args[i]);
               if( ! m.matches() ) 
               {
                  log.debug("Error with dest parameter. Ignoring value: " + args[i]);
               }
               else
               {
                  int numGroups = m.groupCount();
                  for( int g = 0; g <= numGroups; g++ )
                  {
                     log.debug("Group (" + g + ") is: " + m.group(g));
                  }

                  String username = m.group(1);
                  String onBehalfOf = m.group(3);
                  String password = m.group(5);
                  String url = m.group(6);
                  PostDestination destination = new PostDestination(url, username, password, onBehalfOf);

                  multiPost.add(destination);
               }
            }

            if( "-nocapture".equals(args[i]) )
            {
               i++;
               noCapture = true;
            }

         }

         // apply any settings 
         if( href == null && "service".equals(accessType) )
         {
            log.error( "No href specified.");
            return false; 
         }

         if( multiPost.size() == 0 && "multipost".equals(accessType))
         {
            log.error("No destinations specified");
            return false;
         }

         if( accessType == null && ! guiMode ) 
         {
            log.error("No access type specified");
            return false;
         }

         if( ( username == null && password != null ) || (username != null && password == null))
         {
            log.error("The username and/or password are not specified. If one is specified, the other must also be specified.");
            return false; 
         }
      }
      catch( ArrayIndexOutOfBoundsException ex )
      {
         log.error("Error with parameters.");
         return false;
      }

      return true;
   }

   /** 
    * Get the access type. 
    * @return The value, or <code>null</code> if the value is not set. 
    */	
   public String getAccessType()
   {
      return accessType;
   }

   /** 
    * Set the access type. 
    * @param accessType The value, or <code>null</code> to clear the value. 
    */
   public void setAccessType(String accessType)
   {
      this.accessType = accessType;
   }

   /** 
    * Get the proxy host. 
    * @return The value, or <code>null</code> if the value is not set.  
    */
   public String getProxyHost()
   {
      return proxyHost;
   }

   /**
    * Set the proxy host.
    * @param proxyHost The value, or <code>null</code> to clear the value.  
    */
   public void setProxyHost(String proxyHost)
   {
      this.proxyHost = proxyHost;
   }

   /**
    * Get the proxy port. 
    * @return The proxy port. Default value is 80.
    */
   public int getProxyPort()
   {
      return proxyPort;
   }

   /**
    * Set the proxy port. 
    * @param proxyPort The proxy port. 
    */
   public void setProxyPort(int proxyPort)
   {
      this.proxyPort = proxyPort;
   }

   /** 
    * Get the username. 
    * @return The value, or <code>null</code> if the value is not set. 
    */
   public String getUsername()
   {
      return username;
   }

   /**
    * Set the username. 
    * @param username The value, or <code>null</code> to clear the value. 
    */
   public void setUsername(String username)
   {
      this.username = username;
   }

   /**
    * Get the password. 
    * @return The value, or <code>null</code> if the value is not set. 
    */
   public String getPassword()
   {
      return password;
   }

   /**
    * Set the password. 
    * @param password The value, or <code>null</code> to clear the value. 
    */
   public void setPassword(String password)
   {
      this.password = password;
   }

   /**
    * Get the HREF of the service to access. 
    * @return The value, or <code>null</code> if the value is not set. 
    */
   public String getHref()
   {
      return href;
   }

   /**
    * Set the HREF of the service to access. 
    * @param href The value, or <code>null</code> to clear the value. 
    */
   public void setHref(String href)
   {
      this.href = href;
   }

   /**
    * Get the name of the file to post. 
    * @return The value, or <code>null</code> if the value is not set. 
    */
   public String getFilename()
   {
      return filename;
   }

   /** 
    * Set the name of the file to post. 
    * @param filename The value, or <code>null</code> to clear the value. 
    */
   public void setFilename(String filename)
   {
      this.filename = filename;
   }

   /** 
    * Get the type of the file to post. 
    * @return The filetype, or <code>null</code> if the value is not set. 
    */
   public String getFiletype()
   {
      return filetype;
   }

   /**
    * Set the type of the file to post. 
    * @param filetype The value, or <code>null</code> to clear the value. 
    */
   public void setFiletype(String filetype)
   {
      this.filetype = filetype;
   }

   /**
    * Determine if the tool is to be run in GUI mode. 
    * @return True if the tool is set for GUI mode. 
    */
   public boolean isGuiMode()
   {
      return guiMode;
   }

   /**
    * Set the tool to run in GUI mode. 
    * @param guiMode True if the tool is to run in gui mode. 
    */
   public void setGuiMode(boolean guiMode)
   {
      this.guiMode = guiMode; 
   }

   /**
    * Get the MD5 setting. True if the tool is to use MD5 for post operations. 
    * @return The MD5 setting. 
    */
   public boolean isMd5() {
      return md5;
   }

   /**
    * Set the MD5 setting. 
    * @param md5 True if the tool should use MD5 for post operations. 
    */
   public void setMd5(boolean md5) {
      this.md5 = md5;
   }

   /**
    * Determine if the NoOp header should be sent. 
    * @return True if the header should be sent. 
    */
   public boolean isNoOp() {
      return noOp;
   }

   /**
    * Set the NoOp setting. 
    * @param noOp True if the NoOp header should be used. 
    */
   public void setNoOp(boolean noOp) {
      this.noOp = noOp;
   }

   /**
    * Determine if the verbose option is set. 
    * @return True if verbose option is set. 
    */
   public boolean isVerbose() {
      return verbose;
   }

   /**
    * Set the verbose option. 
    * @param verbose True if verbose should be set. 
    */
   public void setVerbose(boolean verbose) {
      this.verbose = verbose;
   }

   /**
    * Get the onBehalfOf value. 
    * @return The value, or <code>null</code> to clear the value.  
    */
   public String getOnBehalfOf() {
      return onBehalfOf;
   }

   /**
    * Set the onBehalf of Value. 
    * @param onBehalfOf The value, or <code>null</code> to clear the value. 
    */
   public void setOnBehalfOf(String onBehalfOf) {
      this.onBehalfOf = onBehalfOf;
   }

   /**
    * Get the format namespace value. 
    * @return The value, or <code>null</code> if the value is not set. 
    */
   public String getFormatNamespace() {
      return formatNamespace;
   }

   /**
    * Set the format namespace value. 
    * @param formatNamespace The value, or <code>null</code> to clear the value. 
    */
   public void setFormatNamespace(String formatNamespace) {
      this.formatNamespace = formatNamespace;
   }

   /**
    * Get the checksum error value. 
    * @return True if an error should be introduced into the checksum. 
    */
   public boolean getChecksumError() {
      return checksumError;
   }

   /**
    * Set the checksum error value. 
    * @param checksumError True if the error should be introduced. 
    */
   public void setChecksumError(boolean checksumError) {
      this.checksumError = checksumError;
   }

   /**
    * Get the current slug header.
    * @return The slug value, or <code>null</code> if the value is not set.  
    */
   public String getSlug( ) 
   {
      return this.slug;
   }

   /**
    * Set the text that is to be used for the slug header.
    * @param slug The value, or <code>null</code> to clear the value.  
    */
   public void setSlug(String slug)
   {
      this.slug = slug;
   }

   /**
    * Get the list of post destinations. 
    * @return An iterator over the list of PostDestination objects. 
    */
   public Iterator<PostDestination> getMultiPost() 
   {
      return multiPost.iterator();
   }


   /**
    * Determine if the noCapture option is set. This indicates that the code 
    * should not attempt to redirect stdout and stderr to a different output 
    * destination. Intended for use in a GUI client. 
    * 
    * @return The noCapture setting. True if set. 
    */
   public boolean isNoCapture()
   {
      return noCapture;
   }
}
