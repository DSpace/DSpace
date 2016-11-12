/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import java.util.Properties;
import nu.xom.Element;
import nu.xom.Elements;

import org.purl.sword.atom.Entry;


/**
 * Extension of the ATOM Entry class. This adds support for the additional 
 * SWORD elements. These elements reside inside the ATOM Entry object, 
 * created in org.w3.atom.Entry class. 
 * 
 * @author Neil Taylor
 */
public class SWORDEntry extends Entry 
{

    /**
     * Specifies whether the document was run in noOp mode, i.e.
     * if the document records that no operation was taken for
     * the deposit other than to generate a response.
     */
    protected SwordNoOp swordNoOp;
 
    /**
     * Used to supply a verbose description. 
     */
    protected SwordVerboseDescription swordVerboseDescription;
    
    /**
     * Used for a human readable statement about what treatment
     * the deposited resource has received. Include either a
     * text description or a URI.
     */
    protected SwordTreatment swordTreatment;
 
    /**
     * The user agent
     */
    protected SwordUserAgent swordUserAgent;
    
    /** 
     * The packaging information
     */
    private SwordPackaging swordPackaging;
    
    /**
     * Create a new SWORDEntry with the given namespace and element. This method is
     * not normally used, instead the default constructor should be used as this will
     * set the namespace and element correctly.
     * 
     * @param namespace The namespace of the element
     * @param element The element name
     * @param namespaceUri The namespace URI.
     */
    public SWORDEntry(String namespace, String element, String namespaceUri)
    {
        super(namespace, element, namespaceUri);
    }
    
    /**
     * A default constructor.
     */
    public SWORDEntry()
    {
        super();
    }
 
    public SWORDEntry(XmlName name)
    {
        super(name); 
    }
 
    protected void initialise()
    {
        super.initialise();
        swordNoOp = null;
        swordPackaging = null;
        swordVerboseDescription = null;
        swordTreatment = null;
        swordUserAgent = null;
    }
    
    /**
     * Get the current value of NoOp.  
     * 
     * @return True if the value is set, false otherwise. 
     */
    public boolean isNoOp()
    {
        if ( swordNoOp == null )
        {
            return false;
        }
        return swordNoOp.getContent();
    }
 
    /**
     * Call this method to set noOp. It should be called even by internal 
     * methods so that the object can determine if the value has been set 
     * or whether it just holds the default value.  
     * 
     * @param noOp  True if the NoOp header should be used.
     */
    public void setNoOp(boolean noOp)
    {
        swordNoOp = new SwordNoOp(noOp); 
    }
    
    /**
     * Determine if the noOp value has been set. This should be called 
     * if you want to know whether false for noOp means that it is the 
     * default value (i.e. no code has set it) or it is a value that
     * has been actively set. 
     * 
     * @return True if the value has been set. Otherwise, false. 
     */
    public boolean isNoOpSet()
    {
        if ( swordNoOp == null )
        {
             return false;
        }
        return swordNoOp.isSet();
    }
 
    /**
     * Get the Verbose Description for this entry. 
     * 
     * @return The description. 
     */
    public String getVerboseDescription()
    {
        if ( swordVerboseDescription == null )
        {
            return null;
        }
        return swordVerboseDescription.getContent();
    }
 
    /**
     * Set the verbose description. 
     * 
     * @param verboseDescription The description. 
     */
    public void setVerboseDescription(String verboseDescription)
    {
        swordVerboseDescription = new SwordVerboseDescription(verboseDescription);
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
     * Set the treatment value. 
     *  
     * @param treatment The treatment. 
     */
    public void setTreatment(String treatment)
    {
        swordTreatment = new SwordTreatment(treatment);
    }
    
    /**
     * Get the user agent
     * 
     * @return the user agent
     */
    public String getUserAgent() 
    {
        if ( swordUserAgent == null )
        {
            return null;
        }
        return swordUserAgent.getContent();
    }
    
    /**
     * Set the user agent
     * 
     * @param userAgent the user agent
     */
    public void setUserAgent(String userAgent)
    {
        swordUserAgent = new SwordUserAgent(userAgent);
    }
    
    /**
     * Get the packaging format
     * 
     * @return the packaging format
     */
    public String getPackaging()
    {
        if ( swordPackaging == null )
        {
            return null;
        }
        return swordPackaging.getContent();
    }
    
    /**
     * Set the packaging format
     * 
     * @param packaging the packaging format
     */
    public void setPackaging(String packaging)
    {
        this.swordPackaging = new SwordPackaging(packaging);
    }
    
    protected void marshallElements(Element entry)
    {
        super.marshallElements(entry);
 
        if ( swordTreatment != null )
        {
            entry.appendChild(swordTreatment.marshall());
        }
 
        if ( swordVerboseDescription != null )
        {
            entry.appendChild(swordVerboseDescription.marshall());
        }
 
        if (swordNoOp != null)
        {
            entry.appendChild(swordNoOp.marshall());
        }
 
        if ( swordUserAgent != null )
        {
            entry.appendChild(swordUserAgent.marshall());
        }
 
        if ( swordPackaging != null )
        {
            entry.appendChild(swordPackaging.marshall()); 
        }
        
    }
 
    /**
     * Overrides the unmarshall method in the parent Entry. This will 
     * call the parent method to parse the general Atom elements and
     * attributes. This method will then parse the remaining sword
     * extensions that exist in the element. 
     * 
     * @param entry The entry to parse. 
     * @param validationProperties FIXME: PLEASE DOCUMENT.
     * @return SWORD validation info
     * @throws UnmarshallException If the entry is not an atom:entry 
     *              or if there is an exception extracting the data. 
     */
    public SwordValidationInfo unmarshallWithValidation(Element entry, Properties validationProperties)
    throws UnmarshallException
    {
        SwordValidationInfo result = super.unmarshallWithoutValidate(entry, validationProperties);
 
        processUnexpectedAttributes(entry, result);
 
        // retrieve all of the sub-elements
        Elements elements = entry.getChildElements();
        Element element = null; 
        int length = elements.size();
 
        for (int i = 0; i < length; i++ )
        {
            element = elements.get(i);
 
            if (isInstanceOf(element, SwordTreatment.elementName()))
            {
                if ( swordTreatment == null )
                {
                    swordTreatment = new SwordTreatment();
                    result.addUnmarshallElementInfo(
                        swordTreatment.unmarshall(element, validationProperties));
                }
                else if ( validationProperties != null )
                {
                    SwordValidationInfo info = new SwordValidationInfo(SwordTreatment.elementName(),
                              SwordValidationInfo.DUPLICATE_ELEMENT,
                              SwordValidationInfoType.WARNING);
                    info.setContentDescription(element.getValue());
                    result.addUnmarshallElementInfo(info);
                }
            }
            else if (isInstanceOf(element, SwordNoOp.elementName()))
            {
               if ( swordNoOp == null )
               {
                   swordNoOp = new SwordNoOp();
                   result.addUnmarshallElementInfo(swordNoOp.unmarshall(element, validationProperties));
               }
               else if ( validationProperties != null )
               {
                   SwordValidationInfo info = new SwordValidationInfo(SwordNoOp.elementName(),
                             SwordValidationInfo.DUPLICATE_ELEMENT,
                             SwordValidationInfoType.WARNING);
                   info.setContentDescription(element.getValue());
                   result.addUnmarshallElementInfo(info);
               }
            }
            else if (isInstanceOf(element, SwordVerboseDescription.elementName()))
            {
               if ( swordVerboseDescription == null )
               {
                   swordVerboseDescription = new SwordVerboseDescription();
                   result.addUnmarshallElementInfo(swordVerboseDescription.unmarshall(element, validationProperties));
               }
               else if ( validationProperties != null )
               {
                   SwordValidationInfo info = new SwordValidationInfo(SwordVerboseDescription.elementName(),
                             SwordValidationInfo.DUPLICATE_ELEMENT,
                             SwordValidationInfoType.WARNING);
                   info.setContentDescription(element.getValue());
                   result.addUnmarshallElementInfo(info);
               }
            }
            else if (isInstanceOf(element, SwordUserAgent.elementName()))
            {
               if ( swordUserAgent == null )
               {
                   swordUserAgent = new SwordUserAgent();
                   result.addUnmarshallElementInfo(swordUserAgent.unmarshall(element, validationProperties));
               }
               else if ( validationProperties != null )
               {
                   SwordValidationInfo info = new SwordValidationInfo(SwordUserAgent.elementName(),
                             SwordValidationInfo.DUPLICATE_ELEMENT,
                             SwordValidationInfoType.WARNING);
                   info.setContentDescription(element.getValue());
                   result.addUnmarshallElementInfo(info);
               }
            }
            else if (isInstanceOf(element, SwordPackaging.elementName()))
            {
                if ( swordPackaging == null )
                {
                    swordPackaging = new SwordPackaging();
                    result.addUnmarshallElementInfo(swordPackaging.unmarshall(element, validationProperties));
                }
                else if ( validationProperties != null )
                {
                    SwordValidationInfo info = new SwordValidationInfo(SwordPackaging.elementName(),
                              SwordValidationInfo.DUPLICATE_ELEMENT,
                              SwordValidationInfoType.WARNING);
                    info.setContentDescription(element.getValue());
                    result.addUnmarshallElementInfo(info);
                }
            }
            else if (validationProperties != null )
            {
                XmlName name = new XmlName(element);
                if ( ! isElementChecked(name) )
                {
                    SwordValidationInfo info = new SwordValidationInfo(name,
                              SwordValidationInfo.UNKNOWN_ELEMENT,
                              SwordValidationInfoType.INFO);
                    info.setContentDescription(element.getValue());
                    result.addUnmarshallElementInfo(info);
                }
            }
 
        } // for
        return result;
    }
    
    public SwordValidationInfo unmarshall(Element entry, Properties validationProperties)
    throws UnmarshallException
    {
 
        SwordValidationInfo result = unmarshallWithValidation(entry, validationProperties);
        if ( validationProperties != null )
        {
            result = validate(result, validationProperties);
        }
        return result;
    }
 
    /**
     * 
     * @param elementName
     *     name of element to check
     * @return true if element is checked
     */
    protected boolean isElementChecked(XmlName elementName)
    {
        if ( elementName == null )
        {
            return false;
        }
 
        return elementName.equals(SwordNoOp.elementName()) |
               elementName.equals(SwordUserAgent.elementName()) | 
               elementName.equals(SwordTreatment.elementName()) |
               elementName.equals(SwordVerboseDescription.elementName()) |
               elementName.equals(SwordPackaging.elementName()) |
               super.isElementChecked(elementName);
    }
 
    public SwordValidationInfo validate(Properties validationContext)
    {
        return validate(null, validationContext);
    }
 
    protected SwordValidationInfo validate(SwordValidationInfo info, 
            Properties validationContext)
    {
        boolean validateAll = (info == null);
 
        SwordValidationInfo swordEntry = super.validate(info, validationContext);
        
        if ( swordUserAgent == null )
        {
             String agent = validationContext.getProperty(HttpHeaders.USER_AGENT);
 
             if ( agent != null )
             {
                 swordEntry.addValidationInfo(new SwordValidationInfo(SwordUserAgent.elementName(),
                     SwordValidationInfo.MISSING_ELEMENT_WARNING +
                         " Clients SHOULD provide a User-Agent request-header (as described in [HTTP1.1] section 14.43). If provided, servers SHOULD store the value in the sword:userAgent element.",
                     SwordValidationInfoType.WARNING));
             }
        }
        else if ( swordUserAgent != null && validateAll )
        {
            info.addValidationInfo(swordUserAgent.validate(validationContext));
        }
 
        // additional rules for sword elements
        if ( swordTreatment == null )
        {
            swordEntry.addValidationInfo(new SwordValidationInfo(SwordTreatment.elementName(),
                SwordValidationInfo.MISSING_ELEMENT_ERROR + " MUST be present and contain either a human-readable statement describing treatment the deposited resource has received or a URI that dereferences to such a description.",
                SwordValidationInfoType.ERROR));
        }
        else if ( swordTreatment != null && validateAll )
        {
            info.addValidationInfo(swordTreatment.validate(validationContext));
        }
 
        // additional rules for sword elements
        if ( swordVerboseDescription == null )
        {
            String verbose = validationContext.getProperty(HttpHeaders.X_VERBOSE);
            if ( verbose != null )
            {
                swordEntry.addValidationInfo(new SwordValidationInfo(SwordVerboseDescription.elementName(),
                    SwordValidationInfo.MISSING_ELEMENT_WARNING + " If the client made the POST request with an X-Verbose:true header, the server SHOULD supply a verbose description of the deposit process.",
                    SwordValidationInfoType.WARNING));
            }
        }
        else if ( swordVerboseDescription != null && validateAll )
        {
            info.addValidationInfo(swordVerboseDescription.validate(validationContext));
        }
 
        if ( swordNoOp == null )
        {
            String noOp = validationContext.getProperty(HttpHeaders.X_NO_OP);
            if ( noOp != null )
            {
                swordEntry.addValidationInfo(new SwordValidationInfo(SwordNoOp.elementName(),
                    SwordValidationInfo.MISSING_ELEMENT_WARNING + " If the client made the POST request with an X-No-Op:true header, the server SHOULD reflect this by including a sword:noOp element with a value of 'true' in the response. See Part A Section 3.1. Servers MAY use a value of 'false' to indicate that the deposit proceeded but MUST NOT use this element to signify an error.",
                    SwordValidationInfoType.WARNING));
            }
        }
        else if ( swordNoOp != null && validateAll )
        {
            info.addValidationInfo(swordNoOp.validate(validationContext));
        }
 
        if ( swordPackaging == null )
        {
             swordEntry.addValidationInfo(new SwordValidationInfo(SwordPackaging.elementName(),
                 SwordValidationInfo.MISSING_ELEMENT_WARNING + " If the POST request results in the creation of packaged resource, the server MAY use this element to declare the packaging type. If used it SHOULD take a value from [SWORD-TYPES].",
                 SwordValidationInfoType.INFO));
        }
        else if ( swordPackaging != null && validateAll )
        {
            info.addValidationInfo(swordPackaging.validate(validationContext));
        }
 
        return swordEntry;
    }
 
    /**
     * Overrides the unmarshall method in the parent Entry. This will 
     * call the parent method to parse the general Atom elements and
     * attributes. This method will then parse the remaining sword
     * extensions that exist in the element. 
     * 
     * @param entry The entry to parse. 
     * 
     * @throws UnmarshallException If the entry is not an atom:entry 
     *              or if there is an exception extracting the data. 
     */
    @Override
    public void unmarshall(Element entry)
    throws UnmarshallException
    {
        unmarshall(entry, null);
    }   
}
