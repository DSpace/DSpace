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
package org.w3.atom;

import nu.xom.Attribute;
import nu.xom.Element;

import org.purl.sword.base.InfoLogger;
import org.purl.sword.base.Namespaces;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;

/**
 * Represents an ATOM Content element. 
 * 
 * @author Neil Taylor
 *
 */
public class Content extends XmlElement implements SwordElementInterface
{
   /**
    * The identifier for the src attribute. 
    */
   public static final String ATTR_SRC = "src";
   
   /**
    * The identifier for the type attribute. 
    */
   public static final String ATTR_TYPE = "type";
   
   /**
    * The data for the type attribute. 
    */
   private String type; 
   
   /**
    * The data for the source attribute. 
    */
   private String source; 

   /**
    * Create a new instance and set the prefix to 
    * 'atom' and the local name to 'content'.  
    */
   public Content()
   {
      super("atom", "content");
   }
   
   /**
    * Get the Source. 
    * 
    * @return The Source. 
    */
   public String getSource()
   {
      return source;
   }

   /**
    * Set the Source. 
    * 
    * @param source The source. 
    */
   public void setSource(String source)
   {
      this.source = source;
   }

   /**
    * Get the type. 
    * 
    * @return The type. 
    */
   public String getType()
   {
      return type;
   }

   /**
    * Set the type for the content. This should match the pattern 
    * ".* /.*" [Note, there is no space before the /, this has been added
    * to allow this text to be written in a Java comment.]. 
    * 
    * An example of the type is <code>application/zip</code>. 
    * 
    * @param type The specified type. 
    * @throws InvalidMediaTypeException If the specified type is null or
    * it does not match the specified pattern. 
    */
   public void setType(String type)
   throws InvalidMediaTypeException
   {
      if( type == null || ! type.matches(".*/.*") )
      {
         throw new InvalidMediaTypeException("Type: '" + type + "' does not match .*/.*");
      }
      
      this.type = type;
   }

   /**
    * Marshall the data in this object to an Element object.
    * 
    * @return A XOM Element that holds the data for this Content element. 
    */
   public Element marshall()
   {
      Element content = new Element(getQualifiedName(), Namespaces.NS_ATOM);
         
      if( type != null )
      {
         Attribute typeAttribute = new Attribute(ATTR_TYPE, type);
         content.addAttribute(typeAttribute);
      }
       
      if( source != null )
      {
         Attribute typeAttribute = new Attribute(ATTR_SRC, source);
         content.addAttribute(typeAttribute);
      }
      
      return content;
   }

   /**
    * Unmarshall the content element into the data in this object. 
    * 
    * @throws UnmarshallException If the element does not contain a
    *                             content element or if there are problems
    *                             accessing the data. 
    */
   public void unmarshall(Element content)
   throws UnmarshallException 
   {
      if( ! isInstanceOf( content, localName, Namespaces.NS_ATOM))
      {
         throw new UnmarshallException("Element is not of the correct type");
      }
      
      try
      {
         // get the attributes
         int attributeCount = content.getAttributeCount();
         Attribute attribute = null; 
         for( int i = 0; i < attributeCount; i++ )
         {
            attribute = content.getAttribute(i);
            String name = attribute.getQualifiedName();
            if( ATTR_TYPE.equals(name))
            {
                type = attribute.getValue();
            }
            
            if( ATTR_SRC.equals(name) )
            {
               source = attribute.getValue();
            }
         }
      }
      catch( Exception ex )
      {
         InfoLogger.getLogger().writeError("Unable to parse an element in Content: " + ex.getMessage());
         throw new UnmarshallException("Error parsing Content", ex);
      }
   }

}
