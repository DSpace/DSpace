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

/**
 *   Author   : $Author: nst $
 *   Date     : $Date: 2007/09/21 15:18:55 $
 *   Revision : $Revision: 1.3 $
 *   Name     : $Name:  $
 */

/**
 * 
 */
public class InfoLogger
{
	/** 
	 * Single instance of the InfoLogger that can be used by all 
	 * calling classes.
	 */
   private static InfoLogger logger = null; 
   
   /**
    * Set the default level to ERROR messages only. 
    */
   private InfoLoggerLevel level = InfoLoggerLevel.ERROR; 
   
   /**
    * 
    */
   public void setLevel( InfoLoggerLevel level ) 
   {
      this.level = level;      
   }
   
   /**
    * Returns the single instance of this class. If this is the first call, 
    * the logger is created on this call. 
    * 
    * @return The InfoLogger. 
    */
   public static InfoLogger getLogger()
   {
      if( logger == null )
      {
         logger = new InfoLogger();
      }
      return logger;
   }
   
   public void writeError(String message)
   {
      // always log errors
      System.err.println("Error: " + message );
   }
   
   public void writeInfo(String message)
   {
      if( level == InfoLoggerLevel.ERROR_WARNING_INFO )
      {
         System.err.println("Info: " + message );
      }
   }
   
   public void writeWarning(String message)
   {
      if( level != InfoLoggerLevel.ERROR )
      {   
         System.err.println("Warning: " + message );
      }
   }
}
