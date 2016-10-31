/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

/**
 * Utility class that holds Checksum related methods. 
 * 
 * @author Neil Taylor
 * @author Stuart Lewis 
 */
public class ChecksumUtils
{
    /** Logger */
    private static Logger log = Logger.getLogger(ChecksumUtils.class);
    
    /**
     * Generate an MD5 hash for the file that is specified in the 
     * filepath. The hash is returned as a String representation. 
     * 
     * @param filepath The path to the file to load. 
     * @return A string hash of the file. 
     * @throws NoSuchAlgorithmException If the MD5 algorithm is 
     * not supported by the installed virtual machine. 
     * 
     * @throws IOException If there is an error accessing the file. 
     */
    public static String generateMD5(String filepath)
    throws NoSuchAlgorithmException, IOException 
    {
       return generateMD5(new FileInputStream(filepath));
    }
    
    /**
     * Generate an MD5 hash for the file that is specified in the 
     * filepath. The hash is returned as a String representation. 
     * 
     * @param md5Stream The InputStream to checksum. 
     * @return A string hash of the file. 
     * @throws NoSuchAlgorithmException If the MD5 algorithm is 
     * not supported by the installed virtual machine. 
     * 
     * @throws IOException If there is an error accessing the file. 
     */
    public static String generateMD5(InputStream md5Stream)
    throws NoSuchAlgorithmException, IOException 
    {
       String md5 = null; 
 
       try
       {
          MessageDigest md = MessageDigest.getInstance("MD5");
          md.reset();
 
          byte[] bytes = new byte[1024];
          int count = 0; 
          while ( (count = md5Stream.read(bytes)) != -1 )
          {
             md.update(bytes, 0, count);
          }
 
          byte[] md5Digest = md.digest();
 
          StringBuffer buffer = new StringBuffer(); 
          for ( byte b : md5Digest )
          {
             // 0xFF is used to handle the issue of negative numbers in the bytes
             String hex = Integer.toHexString(b & 0xFF);
             if ( hex.length() == 1 )
             {
                buffer.append("0");
             }
             buffer.append(hex);
          }
 
          md5 = buffer.toString();
       }
       catch (NoSuchAlgorithmException ex )
       {
          log.error("MD5 Algorithm Not found");
          throw ex;  
       }
       finally
       {
          if ( md5Stream != null )
          {
             md5Stream.close();
          }
       }
 
       return md5; 
    }
    
    /**
     * Generate an MD5 hash for the file that is specified in the 
     * filepath. The hash is returned as a String representation. 
     * 
     * @param bytes The byte array to checksum. 
     * @return A string hash of the file. 
     * @throws NoSuchAlgorithmException If the MD5 algorithm is 
     * not supported by the installed virtual machine. 
     * 
     * @throws IOException If there is an error accessing the file. 
     */
    public static String generateMD5(byte[] bytes)
    throws NoSuchAlgorithmException, IOException 
    {
       String md5 = null; 
 
       try
       {
          MessageDigest md = MessageDigest.getInstance("MD5");
          md.reset();
 
          md.update(bytes);
 
 
          byte[] md5Digest = md.digest();
 
          StringBuffer buffer = new StringBuffer(); 
          for ( byte b : md5Digest )
          {
             // 0xFF is used to handle the issue of negative numbers in the bytes
             String hex = Integer.toHexString(b & 0xFF);
             if ( hex.length() == 1 )
             {
                buffer.append("0");
             }
             buffer.append(hex);
          }
 
          md5 = buffer.toString();
       }
       catch (NoSuchAlgorithmException ex)
       {
          log.error("MD5 Algorithm Not found");
          throw ex; // rethrow 
       }
 
       return md5; 
    }
       
    /**
     * Run a simple test to process the file. 
     * 
     * @param args the command line arguments given
     * @throws NoSuchAlgorithmException If there was an error generating the MD5. 
     * @throws IOException If there is an error accessing the file. 
     */
    public static void main(String[] args) 
    throws NoSuchAlgorithmException, IOException
    {
        System.out.println(ChecksumUtils.generateMD5(args[0]));
    }
}
