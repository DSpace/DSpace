/**
 * Copyright (c) 2008-2009, Aberystwyth University
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.Properties;
import nu.xom.Element;
import nu.xom.Elements;

import org.apache.log4j.Logger;
import org.purl.sword.atom.ContentType;
import org.purl.sword.atom.Title;

/**
 * Represents an Atom Publishing Protocol Workspace element. 
 * 
 * @author Neil Taylor
 */
public class Workspace extends XmlElement implements SwordElementInterface
{
   /**
    * The title for the workspace. 
    */
   private Title title; 

   /**
    * A list of collections associated with this workspace. 
    */
   private List<Collection> collections; 

   /**
    * The logger. 
    */
   private static Logger log = Logger.getLogger(Workspace.class);

   /**
    * Local name part of this element.
    */
   @Deprecated
   public static final String ELEMENT_NAME = "workspace";
   
   private static final XmlName XML_NAME = 
           new XmlName(Namespaces.PREFIX_APP, "workspace", Namespaces.NS_APP);
   
   /**
    * Create a new instance of the workspace, with no title. 
    */
   public Workspace( ) 
   {
      super(XML_NAME);
      initialise();
   }

   public static XmlName elementName()
    {
        return XML_NAME;
    }

   /**
    * Create a new instance of the workspace with the specified title. 
    * 
    * @param title The title. 
    */
   public Workspace( String title )
   {
      this();

      setTitle(title);
   }

   /**
    * Initialise the object, ready for use.
    */
   protected void initialise()
   {
       collections = new ArrayList<Collection>();
       title = null; 
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
      Element workspace = new Element(xmlName.getQualifiedName(), xmlName.getNamespace());

      if( title != null )
      {
         workspace.appendChild(title.marshall());
      }
      
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
      unmarshall(workspace, null);
   }

   /**
    *
    * @param workspace
    * @param validate
    * @return
    * @throws org.purl.sword.base.UnmarshallException
    */
   public SwordValidationInfo unmarshall( Element workspace, Properties validationProperties )
   throws UnmarshallException
   {
      if( ! isInstanceOf(workspace, xmlName))
      {
         return handleIncorrectElement(workspace, validationProperties);
      }

      ArrayList<SwordValidationInfo> validationItems = new ArrayList<SwordValidationInfo>();

      try
      {
         initialise();

         // FIXME - process the attributes 

         // retrieve all of the sub-elements
         Elements elements = workspace.getChildElements();
         Element element = null;
         int length = elements.size();

         for(int i = 0; i < length; i++ )
         {
            element = elements.get(i);
            if( isInstanceOf(element, Title.elementName() ) )
            {
               if( title == null )
               {
                  title = new Title();
                  validationItems.add(title.unmarshall(element, validationProperties));
               }
               else
               {
                  SwordValidationInfo info =
                          new SwordValidationInfo(Title.elementName(),
                             SwordValidationInfo.DUPLICATE_ELEMENT,
                             SwordValidationInfoType.WARNING);
                  info.setContentDescription(element.getValue());
                  validationItems.add(info);
               }
            }
            else if( isInstanceOf(element, Collection.elementName() ))
            {
               Collection collection = new Collection( );
               validationItems.add(collection.unmarshall(element, validationProperties));
               collections.add(collection); 
            }
            else if( validationProperties != null )
            {
                validationItems.add(new SwordValidationInfo(new XmlName(element),
                        SwordValidationInfo.UNKNOWN_ELEMENT,
                        SwordValidationInfoType.INFO));
            }
         }
      }
      catch( Exception ex )
      {
         log.error("Unable to parse an element in workspace: " + ex.getMessage());
         throw new UnmarshallException("Unable to parse element in workspace.", ex);
      }

      SwordValidationInfo result = null;
      if( validationProperties != null )
      {
          result = validate(validationItems, validationProperties);
      }
      return result; 
   }

   /**
    * 
    * @return A validation object that specifies the status of this object.
    */
   @Override
   public SwordValidationInfo validate(Properties validationContext)
   {
       return validate(null, validationContext);
   }

   /**
    * 
    * @param existing
    * @return
    */
   protected SwordValidationInfo validate(ArrayList<SwordValidationInfo> existing,
           Properties validationContext)
   {
      boolean validateAll = (existing == null );

      SwordValidationInfo result = new SwordValidationInfo(xmlName);

      if( collections == null || collections.size() == 0 )
      {
          result.addValidationInfo(new SwordValidationInfo(Collection.elementName(),
                    SwordValidationInfo.MISSING_ELEMENT_WARNING,
                    SwordValidationInfoType.WARNING ));
      }

      if( validateAll )
      {
          if( title != null )
          {
              result.addValidationInfo(title.validate(validationContext));
          }

          if( collections.size() > 0 )
          {
             Iterator<Collection> iterator = collections.iterator();
             while( iterator.hasNext() )
             {
                 result.addValidationInfo(iterator.next().validate(validationContext));
             }
          }
      }

      result.addUnmarshallValidationInfo(existing, null);
      return result; 
   }


}
