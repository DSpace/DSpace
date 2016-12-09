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
import nu.xom.Element;
import nu.xom.Elements;

import org.apache.log4j.Logger;
import org.purl.sword.atom.Generator;


/**
 * Represents an Atom Publishing Protocol Service element, with 
 * SWORD extensions. 
 * 
 * @author Neil Taylor
 */
public class Service extends XmlElement implements SwordElementInterface
{

    private SwordVersion swordVersion;
 
    private SwordNoOp swordNoOp;
 
    private SwordVerbose swordVerbose;
 
    private SwordMaxUploadSize swordMaxUploadSize;
 
    /**
     * The details of the server software that generated the service document.
     */
    private Generator generator;
 
    /**
     * List of Workspaces. 
     */
    private List<Workspace> workspaces; 
    
    /** Logger */
    private static Logger log = Logger.getLogger(Service.class);
    
    /**
     * MaxUploadSize
     */
    @Deprecated
    public static final String ELEMENT_GENERATOR = "generator";
 
    /**
     * Name for this element. 
     */
    @Deprecated
    public static final String ELEMENT_NAME = "service";
 
    /**
     * The XML NAME (prefix, local name and namespace) for this element.
     */
    private static final XmlName XML_NAME =
            new XmlName(Namespaces.PREFIX_APP, "service", Namespaces.NS_APP);
 
 
    /**
     * Create a new instance. 
     */
    public Service()
    {
        super(XML_NAME);
        initialise();
    }
    
    /**
     * Create a new instance. 
     * 
     * @param version The service compliance level. 
     */
    public Service(String version)
    {
        this();
        setVersion(version);
    }
    
    /**
     * Create a new instance with the specified compliance level, noOp and 
     * verbose values. 
     * 
     * @param version  The service compliance level. 
     * @param noOp     The noOp.
     * @param verbose  The verbose element. 
     */
    public Service(String version, boolean noOp, boolean verbose) 
    {
        this();
        setVersion(version);
        setNoOp(noOp);
        setVerbose(verbose);
    }
 
    public static XmlName elementName()
    {
        return XML_NAME; 
    }
    /**
     * Initialise the data structures in this tool.
     */
    private void initialise()
    {
        workspaces = new ArrayList<Workspace>();
        swordVersion = null;
        swordNoOp = null; 
        swordVerbose = null; 
        swordMaxUploadSize = null;
        generator = null;
    }
 
 
    /**
     * Get the SWORD version. 
     * 
     * @return The version. 
     */
    public final String getVersion()
    {
        if ( swordVersion == null )
        {
            return null;
        }
        return swordVersion.getContent();
    }
 
    /**
     * Set the SWORD version. 
     * 
     * @param version The version. 
     */
    public final void setVersion(String version)
    {
        if ( version == null )
        {
            // clear the value 
            swordVersion = null;
            return; 
        }
  
        swordVersion = new SwordVersion(version);
    }
 
    /**
     * Get the NoOp value. 
     * 
     * @return The value. 
     */
    public final boolean isNoOp()
    {
        if ( swordNoOp == null  )
        {
            return false;
        }
        
        return swordNoOp.getContent();
    }
 
    /**
     * Set the NoOp value. 
     * 
     * @param noOp The value. 
     */
    public final void setNoOp(boolean noOp)
    {
        swordNoOp = new SwordNoOp(noOp);
    }
    
    /**
     * Determine if the NoOp value has been set. This should be called to 
     * check if an item has been programmatically set and does not have a
     * default value. 
     * 
     * @return True if it has been set programmatically. Otherwise, false. 
     */
    public final boolean isNoOpSet()
    {
        if ( swordNoOp == null )
        {
            return false;
        }
  
        return swordNoOp.isSet();
    }
 
    /**
     * Get the Verbose setting. 
     * 
     * @return The value. 
     */
    public final boolean isVerbose()
    {
        if ( swordVerbose == null  )
        {
            return false;
        }
 
        return swordVerbose.getContent();
    }
 
    /**
     * Set the Verbose value. 
     * 
     * @param verbose The value. 
     */
    public final void setVerbose(boolean verbose)
    {
        swordVerbose = new SwordVerbose(verbose);
    }
 
    /**
     * Determine if the Verbose value has been set. This should be called to 
     * check if an item has been programmatically set and does not have a
     * default value. 
     * 
     * @return True if it has been set programmatically. Otherwise, false. 
     */
    public final boolean isVerboseSet()
    {
        if ( swordVerbose == null )
        {
            return false;
        }
  
        return swordVerbose.isSet();
    }
    
    /**
     * Set the maximum file upload size in kB
     * 
     * @param maxUploadSize Max upload file size in kB
     */
    public final void setMaxUploadSize(int maxUploadSize)
    {
        swordMaxUploadSize = new SwordMaxUploadSize(maxUploadSize);
    }
    
    /**
     * Get the maximum upload file size (in kB)
     *
     * @return the maximum file upload size. If no value has been set, this will
     * be equal to Integer.MIN_VALUE. 
     */
    public final int getMaxUploadSize()
    {
        if ( swordMaxUploadSize == null )
        {
            return Integer.MIN_VALUE;
        }
        return swordMaxUploadSize.getContent();
    }
 
    public final Generator getGenerator()
    {
        return generator;
    }
    
    public final void setGenerator(Generator generator)
    {
        this.generator = generator; 
    }
 
    /**
     * Get an Iterator over the workspaces. 
     * 
     * @return The workspace. 
     */
    public final Iterator<Workspace> getWorkspaces()
    {
        return workspaces.iterator();
    }
    
    /**
     * Get a List of workspaces
     * 
     * @return The workspaces in a List
     */
    public final List<Workspace> getWorkspacesList()
    {
        return workspaces;
    }
 
    /**
     * Add a workspace. 
     * 
     * @param workspace The workspace. 
     */
    public final void addWorkspace(Workspace workspace)
    {
        this.workspaces.add(workspace);
    }
    
    /**
     * Clear the list of workspaces. 
     */
    public final void clearWorkspaces()
    {
        this.workspaces.clear();
    }
    
    /**
     * Marshal the data in this object to an Element object. 
     * 
     * @return A XOM Element that holds the data for this Content element. 
     */
    public final Element marshall( )
    {
        Element service = new Element(getQualifiedName(), Namespaces.NS_APP);
        service.addNamespaceDeclaration(Namespaces.PREFIX_ATOM, Namespaces.NS_ATOM);
        service.addNamespaceDeclaration(Namespaces.PREFIX_DC_TERMS, Namespaces.NS_DC_TERMS);
        service.addNamespaceDeclaration(Namespaces.PREFIX_SWORD, Namespaces.NS_SWORD);
 
        if ( swordVersion != null )
        {
            service.appendChild(swordVersion.marshall());
        }
 
        if ( swordVerbose != null )
        {
           service.appendChild(swordVerbose.marshall());
        }
 
        if ( swordNoOp != null )
        {
            service.appendChild(swordNoOp.marshall());
        }
 
        if ( swordMaxUploadSize != null )
        {
           service.appendChild(swordMaxUploadSize.marshall());
        }
 
        if ( generator != null )
        {
            service.appendChild(generator.marshall());
        }
 
        for (Workspace item : workspaces)
        {
            service.appendChild(item.marshall());
        }
        
        return service;    
    }
      
    /**
     * Unmarshal the content element into the data in this object. 
     * 
     * @throws UnmarshallException If the element does not contain a
     *      content element or if there are problems
     *      accessing the data. 
     */
    public final void unmarshall( Element service )
    throws UnmarshallException
    {
        unmarshall(service, null);
    }
 
    /**
     * 
     * @param service
     *     element to unmarshall
     * @param validationProperties
     *     FIXME: PLEASE DOCUMENT.
     * @return SWORD validation info
     * @throws UnmarshallException If the element does not contain a
     *      content element or if there are problems
     *      accessing the data. 
     */
    public final SwordValidationInfo unmarshall(Element service, Properties validationProperties)
    throws UnmarshallException
    {
        if (!isInstanceOf(service, xmlName))
        {
            return handleIncorrectElement(service, validationProperties);
        }
 
        ArrayList<SwordValidationInfo> validationItems =
                new ArrayList<SwordValidationInfo>(); 
 
        try
        {
            initialise(); 
 
            // Retrieve all of the sub-elements
            Elements elements = service.getChildElements();
            Element element = null;
            int length = elements.size();
 
            for (int i = 0; i < length; i++ )
            {
                element = elements.get(i);
 
                if (isInstanceOf(element, SwordVersion.elementName() ) )
                {
                    //validationItems.add(unmarshallVersion(element, validate));
                    if ( swordVersion == null )
                    {
                        swordVersion = new SwordVersion();
                        validationItems.add(swordVersion.unmarshall(element, validationProperties));
                    }
                    else if ( validationProperties != null )
                    {
                        SwordValidationInfo info = new SwordValidationInfo(SwordVersion.elementName(),
                            SwordValidationInfo.DUPLICATE_ELEMENT,
                            SwordValidationInfoType.WARNING);
                        info.setContentDescription(element.getValue());
                        validationItems.add(info);
                    }
                }
                else if (isInstanceOf(element, SwordVerbose.elementName()))
                {
                    if ( swordVerbose == null )
                    {
                        swordVerbose = new SwordVerbose();
                        validationItems.add(swordVerbose.unmarshall(element, validationProperties));
                    }
                    else if ( validationProperties != null )
                    {
                        SwordValidationInfo info = new SwordValidationInfo(SwordVerbose.elementName(),
                            SwordValidationInfo.DUPLICATE_ELEMENT,
                            SwordValidationInfoType.WARNING);
                        info.setContentDescription(element.getValue());
                        validationItems.add(info);
                    }
                }
                else if (isInstanceOf(element, SwordNoOp.elementName()) )
                {
                    if ( swordNoOp == null )
                    {
                        swordNoOp = new SwordNoOp();
                        validationItems.add(swordNoOp.unmarshall(element, validationProperties));
                    }
                    else if ( validationProperties != null )
                    {
                        SwordValidationInfo info = new SwordValidationInfo(SwordNoOp.elementName(),
                             SwordValidationInfo.DUPLICATE_ELEMENT,
                             SwordValidationInfoType.WARNING);
                        info.setContentDescription(element.getValue());
                        validationItems.add(info);
                    }
                }
                else if (isInstanceOf(element, SwordMaxUploadSize.elementName()))
                {
                    if ( swordMaxUploadSize == null )
                    {
                        swordMaxUploadSize = new SwordMaxUploadSize();
                        validationItems.add(swordMaxUploadSize.unmarshall(element, validationProperties));
                    }
                    else if ( validationProperties != null )
                    {
                         SwordValidationInfo info = new SwordValidationInfo(SwordNoOp.elementName(),
                             SwordValidationInfo.DUPLICATE_ELEMENT,
                             SwordValidationInfoType.WARNING);
                         info.setContentDescription(element.getValue());
                         validationItems.add(info);
                    }
                }
                else if (isInstanceOf(element, Generator.elementName()))
                {
                    if ( generator == null ) 
                    {
                        generator = new Generator();
                        validationItems.add(generator.unmarshall(element, validationProperties));
                    }
                    else if ( validationProperties != null ) 
                    {
                        SwordValidationInfo info = new SwordValidationInfo(Generator.elementName(),
                            SwordValidationInfo.DUPLICATE_ELEMENT,
                            SwordValidationInfoType.WARNING);
                        info.setContentDescription(element.getValue());
                        validationItems.add(info);
                    }
                }
                else if (isInstanceOf(element, Workspace.elementName() ))
                {
                    Workspace workspace = new Workspace( );
                    validationItems.add(workspace.unmarshall(element, validationProperties));
                    workspaces.add(workspace);
                }
                else if ( validationProperties != null )
                {
                    // report on any additional items. They are permitted because of
                    // the Atom/APP specification. Report the items for information 
                    XmlName name = new XmlName(element.getNamespacePrefix(), 
                                               element.getLocalName(), 
                                               element.getNamespaceURI());
                    
                    validationItems.add(new SwordValidationInfo(name,
                               SwordValidationInfo.UNKNOWN_ELEMENT,
                               SwordValidationInfoType.INFO));
                }
            }
        }
        catch ( Exception ex )
        {
            log.error("Unable to parse an element in Service: " + ex.getMessage());
            ex.printStackTrace();
            throw new UnmarshallException("Unable to parse element in Service", ex);
        }
 
        // now process the validation information
        SwordValidationInfo result = null;
        if ( validationProperties != null )
        {
            result = validate(validationItems, validationProperties);
        }
        return result;
 
    }
 
 
    public SwordValidationInfo validate(Properties validationContext)
    {
        return validate(null, validationContext);
    }
 
    /**
     *
     * @param existing add results to this.
     * @param validationContext FIXME: PLEASE DOCUMENT.
     * @return validation information
     */
    protected SwordValidationInfo validate(List<SwordValidationInfo> existing,
            Properties validationContext)
    {
        boolean validateAll = (existing != null);
        
        SwordValidationInfo result = new SwordValidationInfo(xmlName);
        
        // process the basic rules
        if ( swordVersion == null )
        {
            SwordValidationInfo info = new SwordValidationInfo(SwordVersion.elementName(),
                SwordValidationInfo.MISSING_ELEMENT_WARNING,
                SwordValidationInfoType.WARNING);
            result.addValidationInfo(info);
        }
        
        if ( generator == null )
        {
            SwordValidationInfo info = new SwordValidationInfo(Generator.elementName(),
                SwordValidationInfo.MISSING_ELEMENT_WARNING,
                SwordValidationInfoType.WARNING);
            result.addValidationInfo(info);
        }
 
 
        if ( workspaces == null || workspaces.size() == 0 )
        {
            SwordValidationInfo info = new SwordValidationInfo(Workspace.elementName(),
                "This element SHOULD be included unless the authenticated user does not have permission to deposit.",
                SwordValidationInfoType.WARNING);
            result.addValidationInfo(info);
        }
 
        if ( validateAll )
        {
            if ( swordVersion != null )
            {
                result.addValidationInfo(swordVersion.validate(validationContext));
            }
 
            if ( swordNoOp != null )
            {
                result.addValidationInfo(swordNoOp.validate(validationContext));
            }
 
            if ( swordVerbose != null )
            {
                result.addValidationInfo(swordVerbose.validate(validationContext));
            }
 
            if ( swordMaxUploadSize != null )
            {
                result.addValidationInfo(swordMaxUploadSize.validate(validationContext));
            }
 
            if ( generator != null )
            {
                result.addValidationInfo(generator.validate(validationContext));
            }
 
            Iterator<Workspace> iterator = workspaces.iterator();
            while ( iterator.hasNext() )
            {
                result.addValidationInfo(iterator.next().validate(validationContext));
            }
        }
 
        result.addUnmarshallValidationInfo(existing, null);
        return result; 
    }
}
