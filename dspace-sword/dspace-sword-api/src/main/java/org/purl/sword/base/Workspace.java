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
 *   Date     : $Date: 2007/09/21 15:18:53 $
 *   Revision : $Revision: 1.4 $
 *   Name     : $Name:  $
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.purl.sword.base.Namespaces;
import org.w3.atom.ContentType;
import org.w3.atom.Title;

import nu.xom.Element;
import nu.xom.Elements; 

/**
 * Represents an Atom Publishing Protocol Workspace element. 
 * 
 * @author Neil Taylor
 */
public class Workspace extends XmlElement implements SwordElementInterface
{
	/** 
	 * The element name that is used in the textual representatin of the XML data. 
	 */
	public static final String ELEMENT_NAME = "workspace";

	/**
	 * The title for the workspace. 
	 */
   private Title title; 
   
   /**
    * A list of collections associated with this workspace. 
    */
   private List<Collection> collections; 
   
   /**
    * Create a new instance of the workspace, with no title. 
    */
   public Workspace( ) 
   {
      this(null); 
   }
   
   /**
    * Create a new instance of the workspace with the specified title. 
    * 
    * @param title The title. 
    */
   public Workspace( String title )
   {
      super("app", ELEMENT_NAME);
      setTitle(title);
      collections = new ArrayList<Collection>();
   }
   
   /**
    * Set the title. The type for the title will be set to 
    * <code>ContentType.TEXT</code>
    * 
    * @param title The title. 
    */
   public void setTitle( String title )
   {
      if( this.title == null)
      {
         this.title = new Title();
      }
      this.title.setContent(title);
      this.title.setType(ContentType.TEXT);
      
   }
   
   /**
    * Get the content of the Title element. 
    * 
    * @return The title. 
    */
   public String getTitle( )
   {
      if( title == null ) 
      {
         return null;
      }
      
      return title.getContent(); 
   }
   
   /**
    * Add a collection to the Workspace. 
    * 
    * @param collection The collection. 
    */
   public void addCollection( Collection collection )
   {
      collections.add(collection);
   }
   
   /**
    * Get an Iterator over the collections. 
    * 
    * @return An iterator. 
    */
   public Iterator<Collection> collectionIterator( )
   {
      return collections.iterator();
   }
   
   /**
    * Get a list of the collections
    * 
    * @ return A list.
    */
   public List<Collection> getCollections( )
   {
	   return collections;
   }
   
   /**
    * Marshall the data in this element to an Element. 
    * 
    * @return An element that contains the data in this object. 
    */
   public Element marshall( ) 
   {
   // convert data into XOM elements and return the 'root', i.e. the one 
      // that represents the collection. 
      Element workspace = new Element(ELEMENT_NAME, Namespaces.NS_APP);
      
      workspace.appendChild(title.marshall());
      
      for( Collection item : collections )
      {
         workspace.appendChild(item.marshall());
      }
      
      return workspace;   
   }

   /**
    * Unmarshall the workspace element into the data in this object. 
    * 
    * @throws UnmarshallException If the element does not contain a
    *                             workspace element or if there are problems
    *                             accessing the data. 
    */
   public void unmarshall( Element workspace )
   throws UnmarshallException 
   {
      if( ! isInstanceOf(workspace, ELEMENT_NAME, Namespaces.NS_APP))
      {
         throw new UnmarshallException( "Not an app:workspace element" );
      }
      
      try
      {
         collections.clear(); 
         
         // retrieve all of the sub-elements
         Elements elements = workspace.getChildElements();
         Element element = null; 
         int length = elements.size();
         
         for(int i = 0; i < length; i++ )
         {
            element = elements.get(i);
            // FIXME - atom assumes that it has been defined. WHAT DID I MEAN???
            if( isInstanceOf(element, "title", Namespaces.NS_ATOM ) )
            {
               title = new Title();
               title.unmarshall(element);   
            }
            else if( isInstanceOf(element, "collection", Namespaces.NS_APP ))
            {
               Collection collection = new Collection( );
               collection.unmarshall(element);
               collections.add(collection);
            }
         }
      }
      catch( Exception ex )
      {
         InfoLogger.getLogger().writeError("Unable to parse an element in workspace: " + ex.getMessage());
         throw new UnmarshallException("Unable to parse element in workspace.", ex);
      }
   }
   
}
