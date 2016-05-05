/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import java.util.HashMap;
import java.util.Map;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.ObjectManager;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A class representing a set of all referenced repositories in the DRI document.
 * 
 * @author Scott Phillips
 */
public class RepositoryMeta extends AbstractWingElement implements WingMergeableElement, MetadataElement
{
    /** The name of the ObjectMeta element */
    public static final String E_REPOSITORY_META = "repositoryMeta";

    /** The name of the repository element */
    public static final String E_REPOSITORY = "repository";

    /** The name of this repository identifier attribute*/
    public static final String A_REPOSITORY_ID = "repositoryID";
    
    /** The unique url of this repository */
    public static final String A_REPOSITORY_URL = "url";
    
    /** Has this repositoryMeta element been merged? */
    private boolean merged = false;
    
    /** The registered repositories on this page */
    private Map<String,String> repositories = new HashMap<>();

    /**
     * Construct a new RepositoryMeta
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected RepositoryMeta(WingContext context) throws WingException
    {
        super(context);
        
        ObjectManager objectManager = context.getObjectManager();

        if (!(objectManager == null))
        {
        	this.repositories = objectManager.getAllManagedRepositories();
        }
    }

    /**
     * Determine if the given SAX event is a ObjectMeta element.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return True if this WingElement is equivalent to the given SAX Event.
     */
    @Override
    public boolean mergeEqual(String namespace, String localName, String qName,
            Attributes attributes) throws SAXException, WingException
    {

        if (!WingConstants.DRI.URI.equals(namespace))
        {
            return false;
        }

        return E_REPOSITORY_META.equals(localName);
    }

    /**
     * Since we will only add to the object set and never modify an existing
     * object we do not merge any child elements.
     * 
     * However we will notify the object manager of each identifier we
     * encounter.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element *
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return The child element
     */
    @Override
    public WingMergeableElement mergeChild(String namespace, String localName,
            String qName, Attributes attributes) throws SAXException,
            WingException
    {
        // Check if it's in our name space and an options element.
        if (!WingConstants.DRI.URI.equals(namespace))
        {
            return null;
        }
        
        if (!E_REPOSITORY.equals(localName))
        {
            return null;
        }
        
        // Get the repositoryIdentefier
        String repositoryIdentifier = attributes.getValue(A_REPOSITORY_ID);

        if (repositories.containsKey(repositoryIdentifier))
        {
        	repositories.remove(repositoryIdentifier);
        }
   
        return null;
    }

    /**
     * Inform this element that it is being merged with an existing element.
     */
    @Override
    public Attributes merge(Attributes attributes) throws SAXException,
            WingException
    {
        this.merged = true;

        return attributes;
    }

    /**
     * Translate this element into SAX events.
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical 
     *            events (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     */
    @Override
    public void toSAX(ContentHandler contentHandler,
            LexicalHandler lexicalHandler, NamespaceSupport namespaces)
            throws SAXException
    {
        if (!merged)
        {
            startElement(contentHandler, namespaces, E_REPOSITORY_META, null);
        }
    
    	for (String identifier : repositories.keySet())
    	{
    		// add the repository XML
    		AttributeMap attributes = new AttributeMap();
    		attributes.put(A_REPOSITORY_ID, identifier);
    		attributes.put(A_REPOSITORY_URL, repositories.get(identifier));
    		
    		startElement(contentHandler,namespaces,E_REPOSITORY,attributes);
    		endElement(contentHandler,namespaces,E_REPOSITORY);
    	}
       

        if (!merged)
        {
            endElement(contentHandler, namespaces, E_REPOSITORY_META);
        }
    }
}
