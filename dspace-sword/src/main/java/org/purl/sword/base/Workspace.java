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
    protected final void initialise()
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
    public final void setTitle( String title )
    {
        if ( this.title == null)
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
    public final String getTitle( )
    {
        if ( title == null ) 
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
     * @return A list.
     */
    public List<Collection> getCollections( )
    {
        return collections;
    }
 
    /**
     * Marshal the data in this element to an Element. 
     * 
     * @return An element that contains the data in this object. 
     */
    public Element marshall( ) 
    {
        // convert data into XOM elements and return the 'root', i.e. the one 
        // that represents the collection. 
        Element workspace = new Element(xmlName.getQualifiedName(), xmlName.getNamespace());
  
        if ( title != null )
        {
            workspace.appendChild(title.marshall());
        }
        
        for ( Collection item : collections )
        {
            workspace.appendChild(item.marshall());
        }
  
        return workspace;   
    }
 
    /**
     * Unmarshal the workspace element into the data in this object. 
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
     * @param workspace the element to unmarshall.
     * @param validationProperties FIXME: PLEASE DOCUMENT.
     * @return FIXME: PLEASE DOCUMENT.
     * @throws UnmarshallException If the element does not contain a
     *                             workspace element or if there are problems
     *                             accessing the data. 
     */
    public SwordValidationInfo unmarshall( Element workspace, Properties validationProperties )
    throws UnmarshallException
    {
        if ( ! isInstanceOf(workspace, xmlName))
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
 
            for (int i = 0; i < length; i++ )
            {
                element = elements.get(i);
                if ( isInstanceOf(element, Title.elementName() ) )
                {
                    if ( title == null )
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
                else if ( isInstanceOf(element, Collection.elementName() ))
                {
                   Collection collection = new Collection( );
                   validationItems.add(collection.unmarshall(element, validationProperties));
                   collections.add(collection); 
                }
                else if ( validationProperties != null )
                {
                    validationItems.add(new SwordValidationInfo(new XmlName(element),
                        SwordValidationInfo.UNKNOWN_ELEMENT,
                        SwordValidationInfoType.INFO));
                }
            }
        }
        catch ( Exception ex )
        {
            log.error("Unable to parse an element in workspace: " + ex.getMessage());
            throw new UnmarshallException("Unable to parse element in workspace.", ex);
        }
 
        SwordValidationInfo result = null;
        if ( validationProperties != null )
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
     * @param existing add results to this.
     * @param validationContext FIXME: PLEASE DOCUMENT.
     * @return FIXME: PLEASE DOCUMENT.
     */
    protected SwordValidationInfo validate(List<SwordValidationInfo> existing,
            Properties validationContext)
    {
        boolean validateAll = (existing == null );
 
        SwordValidationInfo result = new SwordValidationInfo(xmlName);
 
        if ( collections == null || collections.size() == 0 )
        {
            result.addValidationInfo(new SwordValidationInfo(Collection.elementName(),
                SwordValidationInfo.MISSING_ELEMENT_WARNING,
                SwordValidationInfoType.WARNING ));
        }
 
        if ( validateAll )
        {
            if ( title != null )
            {
                result.addValidationInfo(title.validate(validationContext));
            }
 
            if ( collections.size() > 0 )
            {
                Iterator<Collection> iterator = collections.iterator();
                while ( iterator.hasNext() )
                {
                    result.addValidationInfo(iterator.next().validate(validationContext));
                }
            }
        }
 
        result.addUnmarshallValidationInfo(existing, null);
        return result; 
    }
}
