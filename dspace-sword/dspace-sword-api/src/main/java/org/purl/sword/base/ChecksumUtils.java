/**
 * Copyright (c) 2007, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */
package org.purl.sword.base;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class that holds Checksum related methods. 
 * 
 * @author Neil Taylor
 */
public class ChecksumUtils
{
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
	         while( (count = md5Stream.read(bytes)) != -1 )
	         {
	            md.update(bytes, 0, count);
	         }
	
	         byte[] md5Digest = md.digest();
	
	         StringBuffer buffer = new StringBuffer(); 
	         for( byte b : md5Digest )
	         {
	            // 0xFF is used to handle the issue of negative numbers in the bytes
	            String hex = Integer.toHexString(b & 0xFF);
	            if( hex.length() == 1 )
	            {
	               buffer.append("0");
	            }
	            buffer.append(hex);
	         }
	
	         md5 = buffer.toString();
	      }
	      catch(NoSuchAlgorithmException ex )
	      {
	          InfoLogger.getLogger().writeInfo("Error accessing string");
	          throw ex; // rethrow 
	      }
	      finally
	      {
	         if( md5Stream != null )
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
	         for( byte b : md5Digest )
	         {
	            // 0xFF is used to handle the issue of negative numbers in the bytes
	            String hex = Integer.toHexString(b & 0xFF);
	            if( hex.length() == 1 )
	            {
	               buffer.append("0");
	            }
	            buffer.append(hex);
	         }
	
	         md5 = buffer.toString();
	      }
	      catch(NoSuchAlgorithmException ex )
	      {
	          InfoLogger.getLogger().writeInfo("Error accessing string");
	          throw ex; // rethrow 
	      }
	      
	      return md5; 
	   }
      
   public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
	   System.out.println(ChecksumUtils.generateMD5(args[0]));
   }
}
