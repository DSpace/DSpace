/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.Properties;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

import org.apache.log4j.Logger;
import org.purl.sword.atom.ContentType;
import org.purl.sword.atom.Title;
import org.purl.sword.atom.Accept;

/**
 * A representation of a SWORD Collection.
 * 
 * @author Stuart Lewis
 * @author Neil Taylor
 */
public class Collection extends XmlElement implements SwordElementInterface
{
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
    private List<Accept> accepts;
 
    /**
     * Holds the SWORD Collection policy. 
     */
    //private String collectionPolicy;
 
    /** 
     * The SWORD mediation value. Indicates if mediation is allowed. 
     */
    //private boolean mediation;
    private SwordMediation swordMediation;
 
    private SwordService swordService;
 
    private DcAbstract dcTermsAbstract;
 
    private SwordTreatment swordTreatment;
 
    private SwordCollectionPolicy swordCollectionPolicy; 
    
    /**
     * The SWORD acceptsPackaging details.
     */
    private List<SwordAcceptPackaging> acceptPackaging;
 
    /**
     * The logger. 
     */
    private static Logger log = Logger.getLogger(Collection.class);
 
    /**
     * Label for the Href attribute.  
     */
    public static final String ATTRIBUTE_HREF = "href";
    
    /**
     * Label for the local part of this element. 
     */
    @Deprecated
    public static final String ELEMENT_NAME = "collection";
 
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_APP, "collection", Namespaces.NS_APP);
 
    /**
     * Create a new instance.
     */
    public Collection()
    {
        super(XML_NAME);
        initialise(); 
    }
 
    public static XmlName elementName()
    {
        return XML_NAME;
    }
 
    protected final void initialise()
    {
        location = null;
        title = null;
        accepts = new ArrayList<Accept>();
        acceptPackaging = new ArrayList<SwordAcceptPackaging>();
        swordCollectionPolicy = null;
        swordMediation = null;
        swordService = null;
        dcTermsAbstract = null;
        swordTreatment = null;
    }
 
    /**
     * Create a new instance and set the initial location for the collection. 
     * 
     * @param location The initial location, expressed as a URL. 
     */
    public Collection(String location) 
    {
        this();
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
        Iterator<Accept> iterator = accepts.iterator();
        for (int i = 0; iterator.hasNext(); i++ )
        {
            Accept accept = iterator.next();
            values[i] = accept.getContent();
        }
        return values; 
    }
 
    /**
     * Retrieve an array that holds all of the Accept details. 
     * 
     * @return An array of strings. Each string represents an 
     *         individual accept element. The array will have a length
     *         of 0 if no accepts elements are stored in this collection. 
     */
    @Deprecated
    public List<String> getAcceptsList() 
    {
        ArrayList<String> items = new ArrayList<String>();
        for (Accept item : accepts )
        {
            items.add(item.getContent());
        }
        return items;
    }   
 
    /**
     * Return the list of accepts entries.
     *
     * @return the list of accepts entries
     */
    public List<Accept> getAcceptList()
    {
        return accepts;
    }
 
    /**
     * Add an accepts entry. 
     * 
     * @param accepts The accepts value. 
     */
    public void addAccepts(String accepts) {
        this.accepts.add(new Accept(accepts));
    }
 
    /**
     * Remove all of the accepts associated with this Collection. 
     */
    public void clearAccepts( )
    {
        this.accepts.clear();
    }
 
    /**
     * Retrieve a hashtable that holds all the acceptsPackaging details. 
     * 
     * @return A hashtable. The keys are accepted packaging formats,
     *           and the values the quality values (stored as QualityValue objects)
     */
    public List<SwordAcceptPackaging> getAcceptPackaging()
    {
        return acceptPackaging;
    }
 
    /**
     * Add an acceptPackaging format. 
     * 
     * @param acceptPackaging the packaging format.
     * @param qualityValue the quality value of accepted packaging format.
     */
    public void addAcceptPackaging(String acceptPackaging, float qualityValue) {
 
        this.acceptPackaging.add(new SwordAcceptPackaging(acceptPackaging, qualityValue)); 
    }
 
    /**
     * Add an acceptPackaging format. A default quality vale is given.
     * 
     * @param acceptPackaging the packaging format.
     */
    public void addAcceptPackaging(String acceptPackaging) {
        this.acceptPackaging.add(new SwordAcceptPackaging(acceptPackaging, new QualityValue()));
    }
 
    /**
     * Remove all of the accepted packaging formats associated with this Collection. 
     */
    public void clearAcceptPackaging( )
    {
        this.acceptPackaging.clear();
    }
 
    /**
     * Get the collection policy. 
     * 
     * @return The SWORD collectionPolicy.
     */
    public String getCollectionPolicy() 
    {
        if ( swordCollectionPolicy == null )
        {
             return null;
        }
        return swordCollectionPolicy.getContent();
    }
 
    /**
     * Set the collection policy. 
     * 
     * @param collectionPolicy The collection policy.
     */
    public void setCollectionPolicy(String collectionPolicy) 
    {
        swordCollectionPolicy = new SwordCollectionPolicy(collectionPolicy);
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
        if ( swordMediation == null )
        {
            return false; 
        }
        return swordMediation.getContent();
    }
 
    public boolean isMediationSet()
    {
        if ( swordMediation == null )
        {
            return false;
        }
        return swordMediation.isSet();
    }
 
    /**
     * Set the mediation value. 
     * 
     * @param mediation The mediation value. 
     */
    public void setMediation(boolean mediation)
    {
        swordMediation = new SwordMediation(mediation); 
    }
 
    /**
     * Get the DC Term abstract.
     *  
     * @return The abstract. 
     */
    public String getAbstract()
    {
        if ( dcTermsAbstract == null )
        {
            return null;
        }
        return dcTermsAbstract.getContent();
    }
 
    /**
     * Set the abstract. 
     * 
     * @param abstractString The abstract. 
     */
    public void setAbstract(String abstractString)
    {
        dcTermsAbstract = new DcAbstract(abstractString);
    }
 
    /**
     * Get the sword service.
     *  
     * @return The service. 
     */
    public String getService()
    {
        if ( swordService == null )
        {
            return null;
        }
        return swordService.getContent();
    }
 
    /**
     * Set the sword service. 
     * 
     * @param serviceString The service. 
     */
    public void setService(String serviceString)
    {
        swordService = new SwordService(serviceString);
    }
 
    /**
     * Set the title. This will set the title type to ContentType.TEXT. 
     * 
     * @param title The title. 
     */
    public void setTitle( String title )
    {
        if ( this.title == null)
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
        if ( title == null ) 
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
        if ( swordTreatment == null )
        {
            return null;
        }
        return swordTreatment.getContent();
    }
 
    /**
     * Set the treatment. 
     * 
     * @param treatment The treatment.
     */
    public void setTreatment(String treatment) 
    {
        swordTreatment = new SwordTreatment(treatment);
    }  
 
    /**
     * Get a string representation of this object. This is 
     * equivalent to calling marshall().toString().
     */
    @Override
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
        Element collection = new Element(getQualifiedName(), Namespaces.NS_APP);
        Attribute href = new Attribute(ATTRIBUTE_HREF, location);
        collection.addAttribute(href);
  
        if (title == null)
        {
            title = new Title();
            title.setContent("Untitled");
        }
        collection.appendChild(title.marshall());
  
        for (Accept item:accepts)
        {
            collection.appendChild(item.marshall());
        }
        
        Iterator<SwordAcceptPackaging> apIterator = acceptPackaging.iterator();
        while ( apIterator.hasNext() )
        {
            collection.appendChild(apIterator.next().marshall());
        }
  
        if (swordCollectionPolicy != null)
        {
            collection.appendChild(swordCollectionPolicy.marshall());
        }
  
        if (dcTermsAbstract != null)
        {
            collection.appendChild(dcTermsAbstract.marshall());
        }
  
        if (swordService != null)
        {
            collection.appendChild(swordService.marshall()); 
        }
  
        if (swordMediation != null )
        {
            collection.appendChild(swordMediation.marshall());
        }
  
        if (swordTreatment != null)
        {
            collection.appendChild(swordTreatment.marshall()); 
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
    public void unmarshall(Element collection)
    throws UnmarshallException 
    {
        unmarshall(collection, null);
    }
 
    
    public SwordValidationInfo unmarshall(Element collection, Properties validationProperties)
    throws UnmarshallException
    {
        if (!isInstanceOf(collection, xmlName))
        {
            return handleIncorrectElement(collection, validationProperties);
        }
  
        ArrayList<SwordValidationInfo> validationItems = new ArrayList<SwordValidationInfo>();
        ArrayList<SwordValidationInfo> attributeValidationItems = new ArrayList<SwordValidationInfo>();
  
        try
        {
            initialise();
  
            // retrieve the attributes
            int count = collection.getAttributeCount();
            Attribute a = null;
            for ( int i = 0; i < count; i++ )
            {
                a = collection.getAttribute(i);
                if (ATTRIBUTE_HREF.equals(a.getQualifiedName()))
                {
                    location = a.getValue();
                    SwordValidationInfo info = new SwordValidationInfo(xmlName, new XmlName(a));
                    info.setContentDescription(location);
                    attributeValidationItems.add(info);
                }
                else
                {
                    SwordValidationInfo info = new SwordValidationInfo(xmlName, new XmlName(a),
                        SwordValidationInfo.UNKNOWN_ATTRIBUTE,
                        SwordValidationInfoType.INFO );
                    info.setContentDescription(a.getValue());
                    attributeValidationItems.add(info);
                }
            }
  
            // retrieve all of the sub-elements
            Elements elements = collection.getChildElements();
            Element element = null;
            int length = elements.size();
  
            for (int i = 0; i < length; i++)
            {
               element = elements.get(i);
               if (isInstanceOf(element, Title.elementName()))
               {
                  if ( title == null )
                  {
                     title = new Title();
                     validationItems.add(title.unmarshall(element, validationProperties));
                  }
                  else if ( validationProperties != null )
                  {
                      SwordValidationInfo info = new SwordValidationInfo(
                          Title.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.INFO);
                      info.setContentDescription(element.getValue());
                      validationItems.add(info);
                  }
               }
               else if (isInstanceOf(element, Accept.elementName()) )
               {
                  Accept accept = new Accept();
                  SwordValidationInfo info = accept.unmarshall(element, validationProperties);
                  accepts.add(accept);
                  validationItems.add(info);
               }
               else if (isInstanceOf(element, SwordAcceptPackaging.elementName()))
               {
                   SwordAcceptPackaging packaging = new SwordAcceptPackaging();
                   validationItems.add(packaging.unmarshall(element, validationProperties));
                   acceptPackaging.add(packaging);
               }
               else if (isInstanceOf(element, SwordCollectionPolicy.elementName()))
               {
                   if (swordCollectionPolicy == null) {
                       swordCollectionPolicy = new SwordCollectionPolicy();
                       validationItems.add(swordCollectionPolicy.unmarshall(element, validationProperties));
                   } 
                   else if ( validationProperties != null )
                   {
                       SwordValidationInfo info = new SwordValidationInfo(
                          SwordCollectionPolicy.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.INFO);
                       info.setContentDescription(element.getValue());
                       validationItems.add(info);
                   }
                  
               }
               else if (isInstanceOf(element, DcAbstract.elementName()))
               {
                  if ( dcTermsAbstract == null )
                  {
                     dcTermsAbstract = new DcAbstract();
                     validationItems.add(dcTermsAbstract.unmarshall(element, validationProperties));
                  }
                  else if ( validationProperties != null )
                  {
                      SwordValidationInfo info = new SwordValidationInfo(DcAbstract.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT, SwordValidationInfoType.INFO);
                      info.setContentDescription(element.getValue());
                      validationItems.add(info);
                  }
               }
               else if (isInstanceOf(element, SwordService.elementName()))
               {
                  if ( swordService == null )
                  {
                     swordService = new SwordService();
                     validationItems.add(swordService.unmarshall(element, validationProperties));
                  }
                  else if ( validationProperties != null )
                  {
                      SwordValidationInfo info = new SwordValidationInfo(SwordService.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.INFO);
                      info.setContentDescription(element.getValue());
                      validationItems.add(info);
                  }
               }
               else if (isInstanceOf(element, SwordMediation.elementName()))
               {
                  if ( swordMediation == null )
                  {
                      swordMediation = new SwordMediation();
                      validationItems.add(swordMediation.unmarshall(element, validationProperties));
                  }
                  else if ( validationProperties != null )
                  {
                      SwordValidationInfo info = new SwordValidationInfo(SwordMediation.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                      info.setContentDescription(element.getValue());
                      validationItems.add(info);
                  }
               }
               else if (isInstanceOf(element, SwordTreatment.elementName()))
               {
                  if ( swordTreatment == null )
                  {
                      swordTreatment = new SwordTreatment();
                      validationItems.add(swordTreatment.unmarshall(element, validationProperties));
                  }
                  else if ( validationProperties != null )
                  {
                      SwordValidationInfo info = new SwordValidationInfo(SwordTreatment.elementName(),
                          SwordValidationInfo.DUPLICATE_ELEMENT,
                          SwordValidationInfoType.WARNING);
                      info.setContentDescription(element.getValue());
                      validationItems.add(info);
                  }
               }
               else if ( validationProperties != null )
               {
                   SwordValidationInfo info = new SwordValidationInfo(new XmlName(element),
                       SwordValidationInfo.UNKNOWN_ELEMENT,
                       SwordValidationInfoType.INFO);
                   info.setContentDescription(element.getValue());
                   validationItems.add(info);
               }
            }
        }
        catch (Exception ex)
        {
            log.error("Unable to parse an element in collection: " + ex.getMessage());
            throw new UnmarshallException("Unable to parse an element in Collection", ex);
        }
  
        SwordValidationInfo result = null;
        if ( validationProperties != null )
        {
            result = validate(validationItems, attributeValidationItems, validationProperties);
        }
        return result; 
  
    }
 
    /**
     *
     */
    @Override
    public SwordValidationInfo validate(Properties validationContext)
    {
        return validate(null, null, validationContext);
    }
 
    /**
     *
     * @param existing add results to this.
     * @param attributes FIXME: PLEASE DOCUMENT.
     * @param validationContext FIXME: PLEASE DOCUMENT.
     * @return SWORD validation info
     */
    protected SwordValidationInfo validate(List<SwordValidationInfo> existing,
            List<SwordValidationInfo> attributes,
            Properties validationContext)
    {
        boolean validateAll = (existing == null);
  
        SwordValidationInfo result = new SwordValidationInfo(xmlName);
  
        if ( accepts == null || accepts.size() == 0 )
        {
            result.addValidationInfo(new SwordValidationInfo(Accept.elementName(),
                SwordValidationInfo.MISSING_ELEMENT_ERROR,
                SwordValidationInfoType.WARNING ));
        }
  
        if ( location == null )
        {
            XmlName attribute = new XmlName(Namespaces.PREFIX_ATOM, 
                                            ATTRIBUTE_HREF, 
                                            Namespaces.NS_ATOM);
  
            result.addAttributeValidationInfo(new SwordValidationInfo(xmlName,
                attribute, SwordValidationInfo.MISSING_ATTRIBUTE_WARNING,
                SwordValidationInfoType.WARNING ));
        }
  
        if ( swordMediation == null )
        {
            result.addValidationInfo(new SwordValidationInfo(SwordMediation.elementName(),
                SwordValidationInfo.MISSING_ELEMENT_WARNING,
                SwordValidationInfoType.WARNING));
        }
  
        if ( validateAll )
        {
            if ( accepts.size() > 0 )
            {
               Iterator<Accept> acceptIterator = accepts.iterator();
               while ( acceptIterator.hasNext() )
               {
                   result.addValidationInfo(acceptIterator.next().validate(validationContext));
               }
            }
  
            if ( acceptPackaging.size() > 0 )
            {
               Iterator<SwordAcceptPackaging> apIterator = acceptPackaging.iterator();
               while ( apIterator.hasNext() )
               {
                   result.addValidationInfo(apIterator.next().validate(validationContext));
               }
            }
  
            if ( location != null )
            {
                result.addAttributeValidationInfo(createValidAttributeInfo(ATTRIBUTE_HREF, location));
            }
  
            if ( title != null )
            {
                result.addValidationInfo(title.validate(validationContext));
            }
  
            if ( swordMediation != null )
            {
                result.addValidationInfo(swordMediation.validate(validationContext));
            }
  
            if ( swordService != null )
            {
                result.addValidationInfo(swordService.validate(validationContext));
            }
  
            if ( swordTreatment != null )
            {
                result.addValidationInfo(swordTreatment.validate(validationContext));
            }
  
            if ( swordCollectionPolicy != null ) 
            {
                result.addValidationInfo(swordCollectionPolicy.validate(validationContext));
            }
            
            if ( dcTermsAbstract != null )
            {
                result.addValidationInfo(dcTermsAbstract.validate(validationContext));
            }
        }
  
        result.addUnmarshallValidationInfo(existing, attributes);
        return result; 
    }
}
