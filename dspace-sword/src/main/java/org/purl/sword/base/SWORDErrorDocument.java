/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;

import java.util.Properties;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * Extension of the SWORD Entry class, specialized for Error Documents. 
 * 
 * @author Stuart Lewis (sdl@aber.ac.uk)
 * @author Neil Taylor (nst@aber.ac.uk)
 */ 
public class SWORDErrorDocument extends SWORDEntry
{
    /**
    * Local name for the element. 
    */
    @Deprecated
    public static final String ELEMENT_NAME = "error";
    
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_SWORD, "error", Namespaces.NS_SWORD);
 
    private static final XmlName ATTRIBUTE_HREF_NAME =
            new XmlName(Namespaces.PREFIX_SWORD, "href", Namespaces.NS_SWORD);
 
    /**
     * The Error URI
     */
    private String errorURI;
 
    /**
     * Create the error document (intended to be used when unmarshalling an error document
     * as this will set the errorURI)
     */
    public SWORDErrorDocument() {
        super(XML_NAME.getPrefix(),
              XML_NAME.getLocalName(),
              XML_NAME.getNamespace());
    }
 
    /**
     * Create the error document
     * 
     * @param errorURI The URI of the error
     */
    public SWORDErrorDocument(String errorURI) {
        this();
        this.errorURI = errorURI;
    }
 
    /**
     * Get the element name.
     *
     * @return the element name.
     */
    public static XmlName elementName()
    {
       return XML_NAME; 
    }
 
    /**
     * Overrides the marshal method in the parent SWORDEntry. This will 
     * call the parent marshal method and then add the additional 
     * elements that have been added in this subclass.  
     */
    public Element marshall()
    {
        Element entry = new Element(getQualifiedName(), Namespaces.NS_SWORD);
        entry.addNamespaceDeclaration(Namespaces.PREFIX_SWORD, Namespaces.NS_SWORD);
        entry.addNamespaceDeclaration(Namespaces.PREFIX_ATOM, Namespaces.NS_ATOM);
        Attribute error = new Attribute("href", errorURI);
        entry.addAttribute(error);
        super.marshallElements(entry);
        return entry;
    }
 
    /**
     * Overrides the unmarshal method in the parent SWORDEntry. This will 
     * call the parent method to parse the general Atom elements and
     * attributes. This method will then parse the remaining sword
     * extensions that exist in the element. 
     * 
     * @param entry The entry to parse. 
     * 
     * @throws UnmarshallException If the entry is not an atom:entry 
     *              or if there is an exception extracting the data. 
     */
    public void unmarshall(Element entry) throws UnmarshallException
    {
        unmarshall(entry, null);
    }
 
    /**
     * 
     * @param entry an element to unmarshall.
     * @param validationProperties FIXME: PLEASE DOCUMENT.
     * @throws org.purl.sword.base.UnmarshallException passed through.
     */
    public SwordValidationInfo unmarshall(Element entry, Properties validationProperties)
    throws UnmarshallException
    {
       SwordValidationInfo result = super.unmarshall(entry, validationProperties);
       result.clearValidationItems();
 
       errorURI = entry.getAttributeValue(ATTRIBUTE_HREF_NAME.getLocalName());
       
       if ( validationProperties != null )
       {
          result = validate(result, validationProperties);
       }
       
       return result;
    }
 
    /**
     * This method overrides the XmlElement definition so that it can allow
     * the definition of the href attribute. All other attributes are
     * shown as 'Unknown Attribute' info elements.
     *
     * @param element The element that contains the attributes
     * @param info    The info object that will hold the validation info. 
     */
    @Override
    protected void processUnexpectedAttributes(Element element, SwordValidationInfo info)
    {
        int attributeCount = element.getAttributeCount();
        Attribute attribute = null;
 
        for ( int i = 0; i < attributeCount; i++ )
        {
             attribute = element.getAttribute(i);
             if ( ! ATTRIBUTE_HREF_NAME.getLocalName().equals(attribute.getQualifiedName()) )
             {
 
                XmlName attributeName = new XmlName(attribute.getNamespacePrefix(),
                        attribute.getLocalName(),
                        attribute.getNamespaceURI());
 
                SwordValidationInfo item = new SwordValidationInfo(xmlName, attributeName,
                        SwordValidationInfo.UNKNOWN_ATTRIBUTE,
                        SwordValidationInfoType.INFO);
                item.setContentDescription(attribute.getValue());
                info.addUnmarshallAttributeInfo(item);
             }
        }
    }
 
    /**
     *
     * @param validationContext FIXME: PLEASE DOCUMENT.
     * @return SWORD validation info
     */
    public SwordValidationInfo validate(Properties validationContext)
    {
        return validate(null, validationContext);
    }
 
    /**
     * 
     * @param info add results to this.
     * @param validationContext unused.
     * @return SWORD validation info
     */
    protected SwordValidationInfo validate(SwordValidationInfo info,
             Properties validationContext)
    {
       
       if ( errorURI == null )
       {
          info.addValidationInfo(new SwordValidationInfo(xmlName, ATTRIBUTE_HREF_NAME,
                  SwordValidationInfo.MISSING_ATTRIBUTE_WARNING,
                  SwordValidationInfoType.WARNING));
       }
       else
       {
          boolean validUri = true;
          if (errorURI.startsWith("http://purl.org/net/sword/error/"))
          {
              // check that the list of codes
              if ( ! (errorURI.equals(ErrorCodes.ERROR_CONTENT) ||
                     errorURI.equals(ErrorCodes.ERROR_CHECKSUM_MISMATCH) ||
                     errorURI.equals(ErrorCodes.ERROR_BAD_REQUEST) ||
                     errorURI.equals(ErrorCodes.TARGET_OWNER_UKNOWN) ||
                     errorURI.equals(ErrorCodes.MEDIATION_NOT_ALLOWED)) )
              {
                  info.addValidationInfo(new SwordValidationInfo(xmlName,
                          ATTRIBUTE_HREF_NAME,
                          "Errors in the SWORD namespace are reserved and legal values are enumerated in the SWORD 1.3 specification. Implementations MAY define their own errors, but MUST use a different namespace to do so.",
                          SwordValidationInfoType.ERROR));
                  validUri = false; 
              }
          }
 
          if ( validUri )
          {
              SwordValidationInfo item = new SwordValidationInfo(xmlName, ATTRIBUTE_HREF_NAME);
              item.setContentDescription(errorURI);
              info.addAttributeValidationInfo(item);
          }
       }
       return info; 
    }
 
    
    /**
     * Get the error URI
     * 
     * @return the error URI
     */
    public String getErrorURI()
    {
        return errorURI;
    }
 
    /**
     * set the error URI
     * 
     * @param error the error URI
     */
    public void setErrorURI(String error)
    {
        errorURI = error;
    }
 
    /**
     * Main method to perform a brief test of the class
     * 
     * @param args the command line arguments given
     */
/*public static void main(String[] args)
    {
        SWORDErrorDocumentTest sed = new SWORDErrorDocumentTest(ErrorCodes.MEDIATION_NOT_ALLOWED);
        sed.setNoOp(true);
        sed.setTreatment("Short back and shine");
        sed.setId("123456789");
        Title t = new Title();
        t.setContent("My first book");
        sed.setTitle(t);
        Author a = new Author();
        a.setName("Lewis, Stuart");
        a.setEmail("stuart@example.com");
        sed.addAuthors(a);
        
        System.out.println(sed.marshall().toXML());
    }
*/
}
