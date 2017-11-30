/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.atom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.Properties;
import nu.xom.Element;
import nu.xom.Elements;

import org.apache.log4j.Logger;
import org.purl.sword.base.HttpHeaders;
import org.purl.sword.base.Namespaces;
import org.purl.sword.base.SwordElementInterface;
import org.purl.sword.base.SwordValidationInfo;
import org.purl.sword.base.SwordValidationInfoType;
import org.purl.sword.base.UnmarshallException;
import org.purl.sword.base.XmlElement;
import org.purl.sword.base.XmlName;

/**
 * Represents an ATOM entry. 
 * 
 * @author Neil Taylor
 *
 */
public class Entry extends XmlElement implements SwordElementInterface
{
   /**
	* Local name for the element. 
	*/
   @Deprecated
   public static final String ELEMENT_NAME = "entry";
   
   /**
    * Local name for the atom id element. 
    */
   @Deprecated
   public static final String ELEMENT_ID = "id";
   
   /**
    * Local name for the atom published element. 
    */
   @Deprecated
   public static final String ELEMENT_PUBLISHED = "published";
   
   /**
    * Local name for the atom updated element. 
    */
   @Deprecated
   public static final String ELEMENT_UPDATED = "updated";

   /**
    * Local name for the atom category element. 
    */
   @Deprecated
   public static final String ELEMENT_CATEGORY = "category";

   /**
    * Local name for the atom generator element. 
    */
   @Deprecated
   public static final String ELEMENT_GENERATOR = "generator";
   
   /**
    * A list of authors associated with this entry. There can be 0 
    * or more of these elements.   
    */
   private List<Author> authors; 

   /**
    * The atom:category data. There can be 0 or more of these elements.
    */
   private List<Category> categories;

   /**
    * A single content element for the Entry. 
    */
   private Content content; 

   /**
    * A single content element for the Entry. 
    */
   private Generator generator; 

   /**
    * A list of contributors associated with this entry. There can be 0 or
    * more of these elements. 
    */
   private List<Contributor> contributors; 

   /**
    * This is a simplified version. The ID can also have atomCommonAttributes, 
    * but these have not been modeled in this version. The content of 
    * ID is an unconstrained string, which is intended to represent a URI. 
    */
   private Id id;

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
   private Published published;

   /**
    * A single, optional, content element for the Entry. 
    */
   private Rights rights; 

   /**
    * A single, optional, content element for the Entry. 
    */
   @Deprecated
   private Source source; 

   /**
    * A single, optional, summary element for the Entry. 
    */
   private Summary summary; 

   /**
    * A required title element for the entry. 
    */
   private Title title; 

   /**
    * The date on which the entry was last updated.  
    */
   private Updated updated;

   /**
    * The log. 
    */
   private static Logger log = Logger.getLogger(Entry.class);

   /**
    * The prefix, local name and namespace used for this element.
    */
   private static final XmlName XML_NAME =
           new XmlName(Namespaces.PREFIX_ATOM, "entry", Namespaces.NS_ATOM);

   /**
    * Create a new instance of the class and initialise it. 
    * Also, set the prefix to 'atom' and the local name to 'entry'. 
    */
   public Entry() 
   {
      this(XML_NAME.getPrefix(),
           XML_NAME.getLocalName(),
           XML_NAME.getNamespace());
   }
   
   /**
    * Create a new instance of the class an initalise it, setting the
    * element namespace and name.
    * 
    * @param prefix The namespace prefix of the element
    * @param element The element name
    */
   public Entry(String prefix, String element)
   {
	   this(prefix, element, XML_NAME.getNamespace());
   }

   /**
    * 
    * @param prefix
    * @param element
    * @param namespaceUri
    */
   public Entry(String prefix, String element, String namespaceUri)
   {
       super(prefix, element, namespaceUri);
       initialise();
   }

   public Entry(XmlName name)
   {
       this(name.getPrefix(), name.getLocalName(), name.getNamespace());
   }



   public static XmlName elementName()
   {
       return XML_NAME;
   }

   protected boolean isElementChecked(XmlName elementName)
   {
       if( elementName == null )
       {
           return false;
       }

       return elementName.equals(Author.elementName()) |
              elementName.equals(Category.elementName()) |
              elementName.equals(Content.elementName()) |
              elementName.equals(Generator.elementName()) |
              elementName.equals(Contributor.elementName()) |
              elementName.equals(Id.elementName()) |
              elementName.equals(Link.elementName()) |
              elementName.equals(Published.elementName()) |
              elementName.equals(Rights.elementName()) |
              elementName.equals(Source.elementName()) |
              elementName.equals(Summary.elementName()) |
              elementName.equals(Title.elementName()) |
              elementName.equals(Updated.elementName());
   }

   /**
    * 
    */
   protected void initialise()
   {
	   authors = new ArrayList<Author>();
	   categories = new ArrayList<Category>();
	   contributors = new ArrayList<Contributor>();
	   links = new ArrayList<Link>();
   }

   /**
    * Marshal the data stored in this object into Element objects. 
    * 
    * @return An element that holds the data associated with this object. 
    */
   public Element marshall()
   {
      Element entry = new Element(getQualifiedName(), Namespaces.NS_ATOM);
      entry.addNamespaceDeclaration(Namespaces.PREFIX_SWORD, Namespaces.NS_SWORD);
      entry.addNamespaceDeclaration(Namespaces.PREFIX_ATOM, Namespaces.NS_ATOM);
      this.marshallElements(entry);
      return entry;  
   }
   
   protected void marshallElements(Element entry)
   {
	      if (id != null)
	      {
             entry.appendChild(id.marshall());
	      }

	      for (Author author : authors)
	      {
	         entry.appendChild(author.marshall());
	      }

	      if (content != null)
	      {
	         entry.appendChild(content.marshall());
	      }

	      if (generator != null)
	      {
	         entry.appendChild(generator.marshall());
	      }

	      for (Author contributor : contributors)
	      {
	         entry.appendChild(contributor.marshall());
	      }

	      for (Link link : links)
	      {
	         entry.appendChild(link.marshall());
	      }

	      if (published != null)
	      {
             entry.appendChild(published.marshall());
	      }

	      if (rights != null)
	      {
	         entry.appendChild(rights.marshall());
	      }

	      if (summary != null)
	      {
	         entry.appendChild(summary.marshall());
	      }

	      if (title != null)
	      {
	         entry.appendChild(title.marshall());
	      }

	      if (source != null)
	      {
	         entry.appendChild(source.marshall());
	      }

	      if (updated != null)
	      {
             entry.appendChild(updated.marshall());
	      }

	      for (Category category : categories)
	      {
             entry.appendChild(category.marshall());
	      }
   }

   /**
    * Unmarshal the contents of the Entry element into the internal data objects
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
       unmarshall(entry, null);
   }

   public SwordValidationInfo unmarshallWithoutValidate(Element entry, Properties validationProperties)
   throws UnmarshallException
   {
	  if (! isInstanceOf(entry, xmlName) )
      {
         return handleIncorrectElement(entry, validationProperties);
      }

      // used to hold the element and attribute unmarshal info results
      SwordValidationInfo result = new SwordValidationInfo(xmlName);

      try
      {
         initialise();

         // FIXME - attributes? 

         // retrieve all of the sub-elements
         Elements elements = entry.getChildElements();
         Element element = null; 
         int length = elements.size();

         for (int i = 0; i < length; i++)
         {
            element = elements.get(i);

            if (isInstanceOf(element, Author.elementName()))
            {
               Author author = new Author(); 
               result.addUnmarshallElementInfo(author.unmarshall(element, validationProperties));
               authors.add(author);
            }
            else if (isInstanceOf(element, Category.elementName()))
            {
               Category category = new Category();
               result.addUnmarshallElementInfo(category.unmarshall(element, validationProperties));
               categories.add(category); 
            }
            else if (isInstanceOf(element, Content.elementName()))
            {
               if( content == null )
               {
                  content = new Content();
                  result.addUnmarshallElementInfo(content.unmarshall(element, validationProperties));
               }
               else if( validationProperties != null )
               {
                   SwordValidationInfo info = new SwordValidationInfo(Content.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                  info.setContentDescription(element.getValue());
                  result.addUnmarshallElementInfo(info);
               }

            }
            else if (isInstanceOf(element, Generator.elementName()))
            {
               if( generator == null )
               {
                  generator = new Generator();
                  result.addUnmarshallElementInfo(generator.unmarshall(element, validationProperties));
               }
               else if( validationProperties != null )
               {
                  SwordValidationInfo info = new SwordValidationInfo(Generator.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                  info.setContentDescription(element.getValue());
                  result.addUnmarshallElementInfo(info);
               }
            }
            else if (isInstanceOf(element, Contributor.elementName()))
            {
               Contributor contributor = new Contributor(); 
               result.addUnmarshallElementInfo(contributor.unmarshall(element, validationProperties));
               contributors.add(contributor);
            }
            else if (isInstanceOf(element, Id.elementName()))
            {
               if( id == null )
               {
                 id = new Id();
                 result.addUnmarshallElementInfo(id.unmarshall(element, validationProperties));
               }
               else if( validationProperties != null )
               {
                  SwordValidationInfo info = new SwordValidationInfo(Id.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                  info.setContentDescription(element.getValue());
                  result.addUnmarshallElementInfo(info);
               }

            }
            else if (isInstanceOf(element, Link.elementName()))
            {
               Link link = new Link(); 
               result.addUnmarshallElementInfo(link.unmarshall(element, validationProperties));
               links.add(link);
            }
            else if (isInstanceOf(element, Published.elementName()))
            {
               if( published == null )
               {
                  published = new Published();
                  result.addUnmarshallElementInfo(published.unmarshall(element, validationProperties));
               }
               else if( validationProperties != null )
               {
                  SwordValidationInfo info = new SwordValidationInfo(Published.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                  info.setContentDescription(element.getValue());
                  result.addUnmarshallElementInfo(info);
               }
            }
            else if (isInstanceOf(element, Rights.elementName()))
            {
               if( rights == null )
               {
                  rights = new Rights();
                  result.addUnmarshallElementInfo(rights.unmarshall(element, validationProperties));
               }
               else if( validationProperties != null )
               {
                  SwordValidationInfo info = new SwordValidationInfo(Rights.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                  info.setContentDescription(element.getValue());
                  result.addUnmarshallElementInfo(info);
               }
            }
            else if (isInstanceOf(element, Summary.elementName()))
            {
               if( summary == null )
               {
                  summary = new Summary();
                  result.addUnmarshallElementInfo(summary.unmarshall(element, validationProperties));
               }
               else if( validationProperties != null )
               {
                  SwordValidationInfo info = new SwordValidationInfo(Summary.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                  info.setContentDescription(element.getValue());
                  result.addUnmarshallElementInfo(info);
               }
            }
            else if (isInstanceOf(element, Title.elementName()))
            {
               if( title == null )
               {
                  title = new Title();
                  result.addUnmarshallElementInfo(title.unmarshall(element, validationProperties));
               }
               else if( validationProperties != null )
               {
                  SwordValidationInfo info = new SwordValidationInfo(Title.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                  info.setContentDescription(element.getValue());
                  result.addUnmarshallElementInfo(info);
               }
            }
            else if (isInstanceOf(element, Updated.elementName()))
            {
               if( updated == null )
               {
                  updated = new Updated();
                  result.addUnmarshallElementInfo(updated.unmarshall(element, validationProperties));
               }
               else if( validationProperties != null )
               {
                  SwordValidationInfo info = new SwordValidationInfo(Updated.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                  info.setContentDescription(element.getValue());
                  result.addUnmarshallElementInfo(info);
               }
            }
            else if (isInstanceOf(element, Source.elementName()))
            {
               if( source == null )
               {
                  source = new Source();
                  result.addUnmarshallElementInfo(source.unmarshall(element, validationProperties));
               }
               else if( validationProperties != null )
               {
                  SwordValidationInfo info = new SwordValidationInfo(Source.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                  info.setContentDescription(element.getValue());
                  result.addUnmarshallElementInfo(info);
               }
            }
            else if( validationProperties != null )
            {
                XmlName name = new XmlName(element);
                if( ! isElementChecked(name) )
                {
                   SwordValidationInfo info = new SwordValidationInfo(name,
                          SwordValidationInfo.UNKNOWN_ELEMENT,
                          SwordValidationInfoType.INFO);
                   info.setContentDescription(element.getValue());
                   result.addUnmarshallElementInfo(info);
                }
            }

         } // for 
      }
      catch (Exception ex)
      {
         log.error("Unable to parse an element in Entry: " + ex.getMessage());
         ex.printStackTrace();
         throw new UnmarshallException("Unable to parse an element in " + getQualifiedName(), ex);
      }

      return result; 
   }

   public SwordValidationInfo unmarshall(Element entry, Properties validationProperties)
   throws UnmarshallException
   {

      SwordValidationInfo result = unmarshallWithoutValidate(entry, validationProperties);
      if( validationProperties != null )
      {
          result = validate(result, validationProperties);
      }
      return result; 
   }

   /**
    *
    */
   public SwordValidationInfo validate(Properties validationContext)
   {
       return validate(null, validationContext);
   }

   /**
    *
    * @param info
    * @param validationContext
    */
   protected SwordValidationInfo validate(SwordValidationInfo info,
            Properties validationContext)
   {
       // determine if a full validation is required 
       boolean validateAll = (info == null);

       SwordValidationInfo result = info;
       if( result == null )
       {
          result = new SwordValidationInfo(xmlName);
       }
       
       // id, title an updated are required
       if( id == null )
       {
          result.addValidationInfo(new SwordValidationInfo(Id.elementName(),
                  SwordValidationInfo.MISSING_ELEMENT_ERROR,
                  SwordValidationInfoType.ERROR));
       }
       else if( id != null && validateAll )
       {
           result.addValidationInfo(id.validate(validationContext));
       }

       if( title == null )
       {
          result.addValidationInfo(new SwordValidationInfo(Title.elementName(),
                  SwordValidationInfo.MISSING_ELEMENT_ERROR,
                  SwordValidationInfoType.ERROR));
       }
       else if( title != null && validateAll )
       {
           result.addValidationInfo(title.validate(validationContext));
       }

       if( updated == null )
       {
          result.addValidationInfo(new SwordValidationInfo(Updated.elementName(),
                  SwordValidationInfo.MISSING_ELEMENT_ERROR,
                  SwordValidationInfoType.ERROR));
       }
       else if( updated != null && validateAll )
       {
           result.addValidationInfo(updated.validate(validationContext));
       }

       // additional sword requirements on the element
       if( contributors.isEmpty() )
       {
          String contributor = validationContext.getProperty(HttpHeaders.X_ON_BEHALF_OF);
          if( contributor != null )
          {
             result.addValidationInfo(new SwordValidationInfo(Contributor.elementName(),
                  SwordValidationInfo.MISSING_ELEMENT_ERROR +
                    " This item SHOULD contain the value of the X-On-Behalf-Of header, if one was present in the POST request.",
                  SwordValidationInfoType.ERROR));
          }
       }
       else if( (! contributors.isEmpty()) && validateAll )
       {
           Iterator<Contributor> iterator = contributors.iterator();
           while( iterator.hasNext() )
           {
              Contributor contributor = iterator.next();
              result.addValidationInfo(contributor.validate(validationContext));
           }
       }

       if( generator == null )
       {
           result.addValidationInfo(new SwordValidationInfo(Generator.elementName(),
                   SwordValidationInfo.MISSING_ELEMENT_ERROR +
                    " SHOULD contain the URI and version of the server software.",
                   SwordValidationInfoType.ERROR));
       }
       else if( generator != null && validateAll )
       {
           result.addValidationInfo(generator.validate(validationContext));
       }

       if( validateAll )
       {
           // process the remaining items
           Iterator<Link> linksIterator = links.iterator();
           while( linksIterator.hasNext() )
           {
              Link link = linksIterator.next();
              result.addValidationInfo(link.validate(validationContext));
           }

           Iterator<Author> authorIterator = authors.iterator();
           while( authorIterator.hasNext() )
           {
              Author author = authorIterator.next();
              result.addValidationInfo(author.validate(validationContext));
           }

           if( content != null )
           {
               result.addValidationInfo(content.validate(validationContext));
           }

           if( published != null )
           {
               result.addValidationInfo(published.validate(validationContext));
           }

           if( rights != null )
           {
               result.addValidationInfo(rights.validate(validationContext));
           }

           if( summary != null )
           {
               result.addValidationInfo(summary.validate(validationContext));
           }

           Iterator<Category> categoryIterator = categories.iterator();
           while( categoryIterator.hasNext() )
           {
              Category category = categoryIterator.next();
              result.addValidationInfo(category.validate(validationContext));
           }
           
       }

       return result;
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
      ArrayList<String> items = new ArrayList<String>();
      for( int i = 0; i < categories.size(); i++ )
      {
         items.add(categories.get(i).getContent());
      }

      return items.iterator();
   }

   /**
    * Add a category. 
    * 
    * @param category the category to add. 
    */
   public void addCategory(String category) {
      this.categories.add(new Category(category));
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
   public Content getContent() 
   {
      return content;
   }

   /**
    * Set the content element for this Entry. 
    * @param content
    */
   public void setContent(Content content) 
   {
      this.content = content;
   } 

   /**
    * Get the generator for this Entry. 
    * 
    * @return The generator element. 
    */
   public Generator getGenerator() 
   {
      return generator;
   }

   /**
    * Set the generator for this Entry. 
    * @param generator
    */
   public void setGenerator(Generator generator) 
   {
      this.generator = generator;
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
   public void addContributor(Contributor contributor) 
   {
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
   public String getId() 
   {
      if( id == null )
      {
          return null;
      }
      return id.getContent();
   }

   /**
    * Set the ID for this Entry. 
    * 
    * @param id The ID. 
    */
   public void setId(String id) 
   {
      this.id = new Id(id);
   }

   /**
    * Get the list of links for this Entry. 
    * 
    * @return An iterator. 
    */
   public Iterator<Link> getLinks() 
   {
      return links.iterator();
   }

   /**
    * Get the link for this Entry. 
    * 
    * @param link The link. 
    */
   public void addLink(Link link) 
   {
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
   public String getPublished() 
   {
      if( published == null )
      {
          return null;
      }
      return published.getContent();
   }
   
   /**
    * Set the published date. The date should be in one of the 
    * supported formats. This method will not check that the string 
    * is in the correct format. 
    * 
    * @param published The string. 
    */
   public void setPublished(String published) 
   {
      this.published = new Published(published);
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
    * @deprecated
    */
   @Deprecated
   public Source getSource() {
      return source;
   }

   /**
    * Set the source for this entry. 
    * 
    * @param source The source.
    * @deprecated
    */
   @Deprecated
   public void setSource(Source source) 
   {
      this.source = source;
   }

   /** 
    * Get the summary. 
    * 
    * @return The summary. 
    */
   public Summary getSummary() 
   {
      return summary;
   }

   /** 
    * Set the summary. 
    * 
    * @param summary The summary. 
    */
   public void setSummary(Summary summary) 
   {
      this.summary = summary;
   }

   /**
    * Get the title. 
    * 
    * @return The title. 
    */
   public Title getTitle() 
   {
      return title;
   }

   /**
    * Set the title. 
    * 
    * @param title The title. 
    */
   public void setTitle(Title title) 
   {
      this.title = title;
   }

   /**
    * Get the updated date, expressed as a String. See
    * org.purl.sword.XmlElement.stringToDate for the 
    * list of supported formats. This particular method 
    * will not check if the date is formatted correctly. 
    * 
    * @return The date. 
    */
   public String getUpdated() 
   {
      if( updated == null )
      {
          return null;
      }
      return updated.getContent();
   }

   /**
    * Set the updated date. The date should match one of the 
    * supported formats. This method will not check the format of the 
    * string. 
    * 
    * @param updated The string. 
    * @see Entry#setPublished(String) setPublished
    */ 
   public void setUpdated(String updated) 
   {
      this.updated = new Updated(updated);
   }     
}
