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
 *   Date     : $Date: 2007/09/21 15:18:54 $
 *   Revision : $Revision: 1.4 $
 *   Name     : $Name:  $
 */

import java.util.ArrayList;
import java.util.List;

import org.purl.sword.base.Namespaces;
import org.w3.atom.ContentType;
import org.w3.atom.Title;

import nu.xom.Attribute;
import nu.xom.Elements; 
import nu.xom.Element;

/**
 * A representation of a SWORD Collection.
 * 
 * http://www.ukoln.ac.uk/repositories/digirep/index/SWORD_APP_Profile_0.5
 * 
 * @author Stuart Lewis
 * @author Neil Taylor
 */
public class Collection extends XmlElement implements SwordElementInterface
{
   /**
    * The element name. 
    */
   public static final String ELEMENT_NAME = "collection";
   
   /** 
    * Collection location, expressed as a URL. 
    */
   private String location;
   
   /**
    * Holds the ATOM Title for the collection. 
    */
   private Title title;
   
   /** 
    * List of the APP:Accept elements. 
    */
   private List<String> accepts;
   
   /**
    * Holds the SWORD Collection policy. 
    */
   private String collectionPolicy; 
   
   /** 
    * The SWORD mediation value. Indicates if mediation is allowed. 
    */
   private boolean mediation;
   
   /**
    * Internal value to track if the mediation value has been 
    * set programmatically. 
    */
   private boolean mediationSet; 
   
   /**
    * The SWORD treatment value. 
    */
   private String treatment;
   
   /** 
    * The SWORD namespace. 
    */
   private String namespace;
   
   /**
    * The DC Terms Abstract details. 
    */
   private String dcAbstract; 
   
   /**
    * Create a new instance.
    */
   public Collection()
   {
      super(null);
	   accepts = new ArrayList<String>();
	   mediationSet = false; 
   }
   
   /**
    * Create a new instance and set the initial location for the collection. 
    * 
    * @param location The initial location, expressed as a URL. 
    */
   public Collection(String location) 
   {
      super(null);
      this.location = location;
   }
   
   /**
    * Retrieve an array that holds all of the Accept details. 
    * 
    * @return An array of strings. Each string represents an 
    *         individual accept element. The array will have a length
    *         of 0 if no accepts elements are stored in this collection. 
    */
   public String[] getAccepts() 
   {
      String[] values = new String[this.accepts.size()];
      return (String[])accepts.toArray(values);
   }
   
   /**
    * Retrieve an array that holds all of the Accept details. 
    * 
    * @return An array of strings. Each string represents an 
    *         individual accept element. The array will have a length
    *         of 0 if no accepts elements are stored in this collection. 
    */
   public List<String> getAcceptsList() 
   {
      return accepts;
   }   

   /**
    * Add an accepts entry. 
    * 
    * @param accepts The accepts value. 
    */
   public void addAccepts(String accepts) {
      this.accepts.add(accepts);
   }
   
   /**
    * Remove all of the accepts associated with this Collection. 
    */
   public void clearAccepts( )
   {
      this.accepts.clear();
   }

   /**
    * Get the collection policy. 
    * 
    * @return The SWORD collectionPolicy.
    */
   public String getCollectionPolicy() {
      return collectionPolicy;
   }

   /**
    * Set the collection policy. 
    * 
    * @param collectionPolicy The collection policy.
    */
   public void setCollectionPolicy(String collectionPolicy) {
      this.collectionPolicy = collectionPolicy;
   }

   /**
    * Get the location. 
    * 
    * @return TShe location
    */
   public String getLocation() {
      return location;
   }

   /**
    * Set the location. 
    * 
    * @param location The location.
    */
   public void setLocation(String location) {
      this.location = location;
   }

   /**
    * Get the mediation value. 
    * 
    * @return The mediation
    */
   public boolean getMediation() {
      return mediation;
   }

   /**
    * Set the mediation value. 
    * 
    * @param mediation The mediation value. 
    */
   public void setMediation(boolean mediation) {
      this.mediation = mediation;
      mediationSet = true;
   }

   /**
    * See getFormatNamespace. 
    * @return the namespace
    * @deprecated Use getFormatNamespace()
    */
   public String getNamespace() {
      return getFormatNamespace();
   }
   
   /**
    * Get the format namespace. 
    * 
    * @return The format namespace. 
    */
   public String getFormatNamespace()
   {
      return namespace;
   }

   /**
    * See setFormatNamespace. 
    * @param namespace the namespace to set
    * @deprecated Use setFormatNamespace
    */
   public void setNamespace(String namespace) {
      setFormatNamespace(namespace);
   }
   
   /**
    * Set the format namespace. 
    * 
    * @param namespace The namespace. 
    */
   public void setFormatNamespace(String namespace)
   {
      this.namespace = namespace; 
   }

   /**
    * Get the DC Term abstract.
    *  
    * @return The abstract. 
    */
   public String getAbstract()
   {
      return dcAbstract;   
   }
   
   /**
    * Set the abstract. 
    * 
    * @param abstractString The abstract. 
    */
   public void setAbstract(String abstractString)
   {
      this.dcAbstract = abstractString;
   }
   
   /**
    * Set the title. This will set the title type to ContentType.TEXT. 
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
    * Get the title. 
    * 
    * @return The title, or <code>null</code> if no title has been set. 
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
    * Get the treatment value. 
    * 
    * @return The treatment.
    */
   public String getTreatment() 
   {
      return treatment;
   }

   /**
    * Set the treatment. 
    * 
    * @param treatment The treatment.
    */
   public void setTreatment(String treatment) 
   {
      this.treatment = treatment;
   }  
   
   /**
    * Get a string representation of this object. This is 
    * equivalent to calling marshall().toString().
    */
   public String toString()
   {
      Element element = marshall(); 
      return element.toString(); 
   }
   
   /**
    * Marshall the data in this object to an Element object. 
    * 
    * @return A XOM Element that holds the data for this Content element. 
    */
   public Element marshall( )
   {
      // convert data into XOM elements and return the 'root', i.e. the one 
      // that represents the collection. 
	  Element collection = new Element(ELEMENT_NAME, Namespaces.NS_APP);
	  Attribute href = new Attribute("href", location);
      collection.addAttribute(href);

      //title = new Title();
      collection.appendChild(title.marshall());
      
      Element acceptsElement = null; 
      for( String item : accepts )
      {
         acceptsElement = new Element("accepts", Namespaces.NS_APP);
         acceptsElement.appendChild(item);
         collection.appendChild(acceptsElement);
      }
	   
      if( collectionPolicy != null )
      {
         Element colPolicyElement = new Element("sword:collectionPolicy", Namespaces.NS_SWORD);
         colPolicyElement.appendChild(collectionPolicy);
         collection.appendChild(colPolicyElement);
      }
      
      if( dcAbstract != null )
      {
         Element dcAbstractElement = new Element("dcterms:abstract", Namespaces.NS_DC_TERMS);
         dcAbstractElement.appendChild(dcAbstract);
         collection.appendChild(dcAbstractElement);
      }
      
      if( mediationSet )
      {
         Element mediationElement = new Element("sword:mediation", Namespaces.NS_SWORD);
         mediationElement.appendChild(Boolean.toString(mediation));
         collection.appendChild(mediationElement);
      }
      
      // treatment
      if( treatment != null )
      {
         Element treatmentElement = new Element("sword:treatment", Namespaces.NS_SWORD);
         treatmentElement.appendChild(treatment);
         collection.appendChild(treatmentElement);
      }
      
      // namespace 
      if( namespace != null )
      {
         Element namespaceElement = new Element("sword:namespace", Namespaces.NS_SWORD);
         namespaceElement.appendChild(namespace);
         collection.appendChild(namespaceElement);
      }
      
      return collection; 
   }
   
   /**
    * Unmarshall the content element into the data in this object. 
    * 
    * @throws UnmarshallException If the element does not contain a
    *                             content element or if there are problems
    *                             accessing the data. 
    */
   public void unmarshall( Element collection )
   throws UnmarshallException 
   {
      if( ! isInstanceOf(collection, "collection", Namespaces.NS_APP))
      {
         throw new UnmarshallException( "Not an app:collection element" );
      }
      
      try
      {
         // retrieve the attributes
         int count = collection.getAttributeCount(); 
         Attribute a = null;
         for( int i = 0; i < count; i++ ) 
         {
            a = collection.getAttribute(i);
            if( "href".equals(a.getQualifiedName()))
            {
               location = a.getValue();
            }
         }
         
         accepts.clear(); 
         
         // retrieve all of the sub-elements
         Elements elements = collection.getChildElements();
         Element element = null; 
         int length = elements.size();
         
         for(int i = 0; i < length; i++ )
         {
            element = elements.get(i);
            // FIXME - atom assumes that it has been defined. not correct.
            if( isInstanceOf(element, "title", Namespaces.NS_ATOM ) )
            {
               title = new Title();
               title.unmarshall(element);   
            }
            else if( isInstanceOf(element, "accepts", Namespaces.NS_APP ))
            {
               accepts.add(unmarshallString(element));
            }
            else if( isInstanceOf(element, "collectionPolicy", Namespaces.NS_SWORD ))
            {
               collectionPolicy = unmarshallString(element);
            }
            else if( isInstanceOf(element, "abstract", Namespaces.NS_DC_TERMS ))
            {
               dcAbstract = unmarshallString(element);
            }
            else if( isInstanceOf(element, "mediation", Namespaces.NS_SWORD ))
            {
               setMediation(unmarshallBoolean(element));
            }
            else if( isInstanceOf(element, "treatment", Namespaces.NS_SWORD ))
            {
               treatment = unmarshallString(element);
            }
            else if( isInstanceOf(element, "namespace", Namespaces.NS_SWORD ))
            {
               namespace = unmarshallString(element);
            }
         }
      }
      catch( Exception ex )
      {
         InfoLogger.getLogger().writeError("Unable to parse an element in Collection: " + ex.getMessage());
         throw new UnmarshallException("Unable to parse an element in Collection", ex);
      }
      
   }
}