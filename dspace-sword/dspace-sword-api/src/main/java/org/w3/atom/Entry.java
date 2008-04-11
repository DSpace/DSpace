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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import nu.xom.Element;
import nu.xom.Elements;

import org.purl.sword.base.InfoLogger;
import org.purl.sword.base.Namespaces;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;

/**
 * Represents an ATOM entry. 
 * 
 * @author Neil Taylor
 *
 */
public class Entry extends XmlElement implements SwordElementInterface
{
   /**
    * A list of authors associated with this entry. There can be 0 
    * or more of these elements.   
    */
   private List<Author> authors; 
   
   /**
    * The atom:category data. There can be 0 or more of these elements.
    * FIXME - this does not accommodate the idea of 'anyForeignElement'
    */
   private List<String> categories;  // FIXME - needed? 
   
   /**
    * A single content element for the Entry. 
    */
   private Content content; 
   
   /**
    * A list of contributors associated with this entry. There can be 0 or
    * more of these elements. 
    */
   private List<Contributor> contributors; 
   
   /**
    * This is a simplified version. The ID can also have atomCommonAttributes, 
    * but these have not been modelled in this version. The content of 
    * ID is an unconstrained string, which is intended to represent a URI. 
    */
   private String id; 
   
   /**
    * A list of link elements. This can contain 0 or more entries. 
    */
   private List<Link> links; 
   
   /**
    * Simplified version of the atom:published element. This implementation 
    * does not record the general atomCommonAttributes. The date is 
    * taken from an xsd:dateTime value. 
    * 
    * This item is optional. 
    */
   private Date published; 
   
   /**
    * A single, optional, content element for the Entry. 
    * FIXME - this does not cater for the different content types.
    */
   private Rights rights; 
   
   /**
    * A single, optional, content element for the Entry. 
    */
   private Source source; 
   
   /**
    * A single, optional, summary element for the Entry. 
    * FIXME - this does not cater for the different content types.
    */
   private Summary summary; 
   
   /**
    * A required title element for the entry. 
    * FIXME - this does not cater for the different content types.  
    */
   private Title title; 

   /**
    * The date on which the entry was last updated.  
    */
   private Date updated; 
   
   /**
    * Create a new instance of the class and initialise it. 
    * Also, set the prefix to 'atom' and the local name to 'entry'. 
    */
   public Entry() 
   {
      super("atom", "entry");
      
      authors = new ArrayList<Author>();
      categories = new ArrayList<String>();
      contributors = new ArrayList<Contributor>();
      links = new ArrayList<Link>();
   }

   /**
    * Mashall the data stored in this object into Element objects. 
    * 
    * @return An element that holds the data associated with this object. 
    */
   public Element marshall()
   {
      Element entry = new Element(getQualifiedName(), Namespaces.NS_ATOM);
      entry.addNamespaceDeclaration("sword", Namespaces.NS_SWORD);
      entry.addNamespaceDeclaration("atom", Namespaces.NS_ATOM);
	      
	   if( id != null )
      {
         Element idElement = new Element(getQualifiedName("id"), Namespaces.NS_ATOM);
         idElement.appendChild(id);
		   entry.appendChild(idElement);
      }
	      
	   for( Author author : authors )
	   {
         entry.appendChild(author.marshall());
	   }
	
	  if( content != null )
	  {
		  entry.appendChild(content.marshall());
	  }
	  
	  for( Author contributor : contributors )
	  {
         entry.appendChild(contributor.marshall());
	  }
	
	  for( Link link : links )
	  {
         entry.appendChild(link.marshall());
	  }
	
	  if( published != null )
	  {
		  Element publishedElement = new Element(getQualifiedName("published"), Namespaces.NS_ATOM);
	      publishedElement.appendChild(dateToString(published));
	      entry.appendChild(publishedElement);
	  }
	
	  if( rights != null )
	  {
		  entry.appendChild(rights.marshall());
	  }
	  
	  if( summary != null )
	  {
		  entry.appendChild(summary.marshall());
	  }
	  
	  if( title != null )
	  {
		  entry.appendChild(title.marshall());
	  }
	  
	  if( source != null )
	  {
		  entry.appendChild(source.marshall());
	  }
	  
	  if( updated != null )
	  {
		  Element updatedElement = new Element(getQualifiedName("updated"), Namespaces.NS_ATOM);
	     updatedElement.appendChild(dateToString(updated));
	     entry.appendChild(updatedElement);
	  }
	  
	  Element categoryElement = null; 
	  for( String category : categories )
	  {
		 categoryElement = new Element(getQualifiedName("category"), Namespaces.NS_ATOM );
		 categoryElement.appendChild(category);
         entry.appendChild(categoryElement);
	  }
	 
      return entry;  
   }

   /**
    * Unmarshall the contents of the Entry element into the internal data objects
    * in this object. 
    * 
    * @param entry The Entry element to process. 
    *
    * @throws UnmarshallException If the element does not contain an ATOM entry
    *         element, or if there is a problem processing the element or any 
    *         subelements. 
    */
   public void unmarshall(Element entry)
   throws UnmarshallException
   {
      if( ! isInstanceOf(entry, localName, Namespaces.NS_ATOM))
      {
         throw new UnmarshallException( "Not a " + getQualifiedName() + " element" );
      }
      
	   try
	   {
	      authors.clear();
		   categories.clear();
		   contributors.clear();
		   links.clear(); 
		 
		   // retrieve all of the sub-elements
		   Elements elements = entry.getChildElements();
		   Element element = null; 
		   int length = elements.size();

		   for(int i = 0; i < length; i++ )
         {
            element = elements.get(i);

            if( isInstanceOf(element, "author", Namespaces.NS_ATOM ) )
			   {
    		      Author author = new Author(); 
				   author.unmarshall(element);
				   authors.add(author);
			   }
			   else if( isInstanceOf(element, "category", Namespaces.NS_ATOM ))
			   {
			      categories.add(unmarshallString(element));
			   }
			   else if( isInstanceOf(element, "content", Namespaces.NS_ATOM))
			   {
			      content = new Content();
			      content.unmarshall(element); 
			   }
			   else if( isInstanceOf(element, "contributor", Namespaces.NS_ATOM))
			   {
				   Contributor contributor = new Contributor(); 
				   contributor.unmarshall(element);
				   contributors.add(contributor);
			   }
		      else if( isInstanceOf(element, "id", Namespaces.NS_ATOM ))
			   {
		         id = unmarshallString(element);
		      }
		      else if( isInstanceOf(element, "link", Namespaces.NS_ATOM))
		      {
		    	   Link link = new Link(); 
			      link.unmarshall(element);
				   links.add(link);
		      }
		      else if( isInstanceOf(element, "published", Namespaces.NS_ATOM) )
		      {
		         published = unmarshallDate(element);   	
		      }
		      else if( isInstanceOf(element, "rights", Namespaces.NS_ATOM))
		      {
		    	   rights = new Rights(); 
			      rights.unmarshall(element);
			   }
		      else if( isInstanceOf(element, "summary", Namespaces.NS_ATOM))
		      {
		    	   summary = new Summary(); 
			      summary.unmarshall(element);
			   }
		      else if( isInstanceOf(element, "title", Namespaces.NS_ATOM))
		      {
		         title = new Title(); 
			      title.unmarshall(element);
			   }
		      else if( isInstanceOf(element, "updated", Namespaces.NS_ATOM) )
		      {
		         updated = unmarshallDate(element);   	
		      }
		      else if( isInstanceOf(element, "source", Namespaces.NS_ATOM))
		      {
		         source = new Source(); 
			      source.unmarshall(element);
			   }
		      else
			   {
			      // unknown element type
			      //counter.other++; 
			   }
         } // for 
	   }
	   catch( Exception ex )
	   {
         InfoLogger.getLogger().writeError("Unable to parse an element in Entry: " + ex.getMessage());
         ex.printStackTrace();
         throw new UnmarshallException("Unable to parse an element in " + getQualifiedName(), ex);
	   }
   }

   /**
    * Get an iterator for the authors in the Entry. 
    * 
    * @return An iterator. 
    */
   public Iterator<Author> getAuthors()
   {
      return authors.iterator();
   }

   /**
    * Add an author to the Entry. 
    * 
    * @param author The author to add. 
    */
   public void addAuthors(Author author)
   {
      this.authors.add(author);
   }

   /**
    * Clear the list of authors. 
    */
   public void clearAuthors()
   {
      this.authors.clear();
   }
   
   /**
    * Get an iterator for the categories in this Entry. 
    * 
    * @return An iterator. 
    */
   public Iterator<String> getCategories() {
	  return categories.iterator();
   }
   
   /**
    * Add a category. 
    * 
    * @param category the category to add. 
    */
   public void addCategory(String category) {
	  this.categories.add(category);
   }
   
   /**
    * Clear the list of categories. 
    */
   public void clearCategories()
   {
	   this.categories.clear();
   }

   /**
    * Get the content element for this Entry. 
    * 
    * @return The content element. 
    */
   public Content getContent() {
	   return content;
   }

   /**
    * Set the content element for this Entry. 
    * @param content
    */
   public void setContent(Content content) {
	  this.content = content;
   } 

   /**
    * Get a list of contributors. 
    * 
    * @return An iterator. 
    */
   public Iterator<Contributor> getContributors() {
	  return contributors.iterator();
   }

   /**
    * Add a contributor. 
    * 
    * @param contributor The contributor. 
    */
   public void addContributor(Contributor contributor) {
	  this.contributors.add(contributor);
   }
   
   /**
    * Clear the list of contributors. 
    */
   public void clearContributors()
   {
	   this.contributors.clear();
   }

   /**
    * Get the ID for this Entry. 
    * 
    * @return The ID. 
    */
   public String getId() {
	  return id;
   }

   /**
    * Set the ID for this Entry. 
    * 
    * @param id The ID. 
    */
   public void setId(String id) {
	  this.id = id;
   }

   /**
    * Get the list of links for this Entry. 
    * 
    * @return An iterator. 
    */
   public Iterator<Link> getLinks() {
	  return links.iterator();
   }

   /**
    * Get the link for this Entry. 
    * 
    * @param link The link. 
    */
   public void addLink(Link link) {
	  this.links.add(link);
   }
   
   /**
    * Clear the list of links. 
    */
   public void clearLinks()
   {
	   this.links.clear();
   }

   /**
    * Get the published date, expressed as a String. 
    * 
    * @return The date. 
    */
   public String getPublished() {
	  return dateToString(published);
   }

   /**
    * Set the published date. Converts the date string into a date. 
    * The date should be expressed as a string with the format: 
    * 'yyyy-mm-ddThh:mm:ssZ'. 
    * 
    * @param published The string. 
    * 
    * @throws ParseException, if the date does not match format. 
    */
   public void setPublished(String published) 
   throws ParseException 
   {
      this.published = stringToDate(published);
   }

   /**
    * Get the rights for this Entry. 
    * @return The rights. 
    */
   public Rights getRights() {
	  return rights;
   }

   /**
    * Set the rights for this Entry. 
    * 
    * @param rights The rights. 
    */
   public void setRights(Rights rights) {
	  this.rights = rights;
   }

   /**
    * Get the source for this Entry. 
    * @return The source. 
    */
   public Source getSource() {
	  return source;
   }

   /**
    * Set the source for this entry. 
    * 
    * @param source The source. 
    */
   public void setSource(Source source) {
	  this.source = source;
   }

   /** 
    * Get the summary. 
    * 
    * @return The summary. 
    */
   public Summary getSummary() {
	  return summary;
   }

   /** 
    * Set the summary. 
    * 
    * @param summary The summary. 
    */
   public void setSummary(Summary summary) {
	  this.summary = summary;
   }

   /**
    * Get the title. 
    * 
    * @return The title. 
    */
   public Title getTitle() {
	  return title;
   }

   /**
    * Set the title. 
    * 
    * @param title The title. 
    */
   public void setTitle(Title title) {
	  this.title = title;
   }

   /**
    * Get the updated date, expressed as a String. 
    * 
    * @return The date. 
    */
   public String getUpdated() {
	  return dateToString(updated);
   }

   /**
    * Set the updated date. Converts the date string into a date. 
    * The date should be expressed as a string with the format: 
    * 'yyyy-mm-ddThh:mm:ssZ'. 
    * 
    * @param updated The string. 
    * 
    * @throws ParseException, if the date does not match format. 
    */ 
   public void setUpdated(String updated) 
   throws ParseException 
   {
      this.updated = stringToDate(updated);
   }
      
}
