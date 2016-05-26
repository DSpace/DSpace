/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing;

import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Include metadata in the resulting DRI document as derived from the sitemap
 * parameters.
 *
 * <p>Parameters should consist of a Dublin Core name and value. The format for
 * a parameter name must follow the form: "{@code <element>.<qualifier>.<language>#order}"
 * The qualifier, language, and order are all optional components. The order
 * component is an integer and is needed to ensure that parameter names are
 * unique. Since Cocoon's parameters are {@code Hash}es, duplicate names are not allowed.
 * The {@code order} syntax allows the sitemap programmer to specify an order in which
 * these metadata values should be placed inside the document.
 *
 * <p>The following are a valid examples:
 *
 * <ul>
 *  <li>{@code <map:parameter name="theme.name.en" value="My Theme"/>}</li>
 *
 *  <li>{@code <map:parameter name="theme.path" value="/MyTheme/"/>}</li>
 *
 *  <li>{@code <map:parameter name="theme.css#1" value="style.css"/>}</li>
 *
 *  <li>{@code <map:parameter name="theme.css#2" value="style.css-ie"/>}</li>
 *
 *  <li>{@code <map:parameter name="theme.css#2" value="style.css-ff"/>}</li>
 * </ul>
 *
 * @author Scott Phillips
 * @author Roel Van Reeth (roel at atmire dot com)
 * @author Art Lowel (art dot lowel at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class IncludePageMeta extends AbstractWingTransformer implements CacheableProcessingComponent
{

    /** The metadata loaded from the sitemap parameters. */
    private List<Metadata> metadataList;

    /**
     * Generate the unique key.
     * This key must be unique inside the space of this component.
     *
     * @return The generated key hashes the src
     */
    @Override
    public Serializable getKey()
    {
        String key = "";

        for (Metadata metadata : metadataList)
        {
            key = "-" + metadata.getName() + "=" + metadata.getValue();
        }
        return HashUtil.hash(key);
    }

    /**
     * Generate the validity object.
     *
     * @return The generated validity object or <code>null</code> if the
     *         component is currently not cacheable.
     */
    @Override
    public SourceValidity getValidity()
    {
        return NOPValidity.SHARED_INSTANCE;
    }



    /**
     * Extract the metadata name value pairs from the sitemap parameters.
     * @param resolver resolver.
     * @param objectModel objectModel.
     * @param src source.
     * @param parameters parameters.
     * @throws org.apache.cocoon.ProcessingException passed through.
     * @throws org.xml.sax.SAXException passed through.
     * @throws java.io.IOException passed through.
     */
    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters parameters) throws ProcessingException, SAXException,
            IOException
    {
        try
        {
            String[] names = parameters.getNames();
            metadataList = new ArrayList<>();
            for (String name : names)
            {
            	String[] nameParts = name.split("#");

            	String dcName = null;
            	int order = -1;
                switch (nameParts.length)
                {
                case 1:
                    dcName = nameParts[0];
                    order = 1;
                    break;
                case 2:
                    dcName = nameParts[0];
                    order = Integer.valueOf(nameParts[1]);
                    break;
                default:
                    throw new ProcessingException("Unable to parse page metadata name, '" + name + "', into parts.");
                }

                String[] dcParts = dcName.split("\\.");
                String element = null;
                String qualifier = null;
                String language = null;
                switch (dcParts.length)
                {
                case 1:
                    element = dcParts[0];
                    break;
                case 2:
                    element = dcParts[0];
                    qualifier = dcParts[1];
                    break;
                case 3:
                    element = dcParts[0];
                    qualifier = dcParts[1];
                    language = dcParts[2];
                    break;
                default:
                    throw new ProcessingException("Unable to parse page metadata name, '" + name + "', into parts.");
                }

                String value = parameters.getParameter(name);

                Metadata metadata = new Metadata(element,qualifier,language,order,value);
                metadataList.add(metadata);
            }

            Collections.sort(metadataList);
        }
        catch (ParameterException pe)
        {
            throw new ProcessingException(pe);
        }

        // Initialize the Wing framework.
        try
        {
            this.setupWing();
        }
        catch (WingException we)
        {
            throw new ProcessingException(we);
        }

        // concatenation
        if (DSpaceServicesFactory.getInstance().getConfigurationService().getBooleanProperty("xmlui.theme.enableConcatenation",false)) {
            metadataList = enableConcatenation();
        }
    }

    /**
     * Alters the URL to CSS, JS or JSON files to concatenate them.
     * Enable the ConcatenationReader in the theme sitemap for
     * concatenation to work correctly
     */
    private List<Metadata> enableConcatenation() {
        Metadata last = null;
        List<Metadata> newMetadataList = new ArrayList<>();

        for (Metadata metadata : metadataList)
        {
            // only try to concatenate css and js
            String curfile = metadata.getValue();
            if (curfile.lastIndexOf('?') != -1) {
                curfile = curfile.substring(0, curfile.lastIndexOf('?'));
            }
            if (curfile.endsWith(".css") || curfile.endsWith(".js") || curfile.endsWith(".json")) {
                String curval = metadata.getValue();
                // check if this metadata and the last one are compatible
                if(last != null && checkConcatenateMerge(last, metadata)) {
                    // merge
                    String lastval = last.getValue();
                    curval = metadata.getValue();
                    String newval = lastval.substring(0,lastval.lastIndexOf('.')) + ",";
                    newval += curval.substring(curval.lastIndexOf('/')+1,curval.lastIndexOf('.'));
                    newval += lastval.substring(lastval.lastIndexOf('.'));
                    last.value = newval;
                } else {
                    // no merge, so add to list
                    newMetadataList.add(metadata);
                    // handle query string cases
                    if(curval.lastIndexOf('?') != -1) {
                        if(curval.substring(curval.lastIndexOf('?')).equals("?nominify")) {
                            // concat should still be possible, so set last
                            last = metadata;
                        } else if(curval.substring(curval.lastIndexOf('?')).equals("?noconcat")) {
                            // no concat should be possible so set last to null
                            last = null;
                            // query string can be removed
                            curval = curval.substring(0, curval.lastIndexOf('?'));
                            metadata.value = curval;
                        } else {
                            // no concat should be possible so set last to null
                            last = null;
                            // query string should be set to "nominify"
                            curval = curval.substring(0, curval.lastIndexOf('?')) + "?nominify";
                            metadata.value = curval;
                        }
                    } else {
                        // multiple possibilities:
                        // * last == null, so set it
                        // * no merge is possible, so change last to this metadata
                        // no query string, so concat and merge should be possible later on
                        last = metadata;
                    }
                }
            } else {
                // wrong extension
                newMetadataList.add(metadata);
            }
        }
        return newMetadataList;
    }

    private boolean checkConcatenateMerge(Metadata last, Metadata current) {
        // check if elements are equal
        if(last.getElement() == null) {
            if(current.getElement() != null) {
                return false;
            }
        } else if(!last.getElement().equals(current.getElement())) {
            return false;
        }
        // check if qualifiers are equal
        if(last.getQualifier() == null) {
            if(current.getQualifier() != null) {
                return false;
            }
        } else if(!last.getQualifier().equals(current.getQualifier())) {
            return false;
        }
        // check if languages are equal
        if(last.getLanguage() == null) {
            if(current.getLanguage() != null) {
                return false;
            }
        } else if(!last.getLanguage().equals(current.getLanguage())) {
            return false;
        }


        String curval = current.getValue();
        String lastval = last.getValue();
        // check if extensions and query strings are equal
        if(!lastval.substring(lastval.lastIndexOf('.')).equals(curval.substring(curval.lastIndexOf('.')))) {
            return false;
        }
        // check if paths are equal
        if(!lastval.substring(0,lastval.lastIndexOf('/')+1).equals(curval.substring(0,curval.lastIndexOf('/')+1))) {
            return false;
        }

        // only valid nonempty query string is "nominify"
        return !(curval.lastIndexOf('?') != -1
                && !"?nominify".equals(curval.substring(curval.lastIndexOf('?'))));

    }
    /**
     * Include the metadata in the page metadata.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        for (Metadata metadata : metadataList)
        {
        	String element = metadata.getElement();
        	String qualifier = metadata.getQualifier();
        	String language = metadata.getLanguage();
            String value = metadata.getValue();

            // Add our new metadata.
            pageMeta.addMetadata(element, qualifier, language)
                    .addContent(value);
        }
    }


    /**
     * Private class to keep track of metadata name/value pairs.
     */
    static class Metadata implements Comparable<Metadata> {

    	private final String element;
    	private final String qualifier;
    	private final String language;
    	private final int order;
    	private String value;

    	public Metadata(String element,String qualifier, String language, int order, String value)
    	{
    		this.element = element;
    		this.qualifier = qualifier;
    		this.language = language;
    		this.order = order;
    		this.value = value;
    	}

    	public String getElement()
    	{
    		return this.element;
    	}

    	public String getQualifier()
    	{
    		return this.qualifier;
    	}

    	public String getLanguage()
    	{
    		return this.language;
    	}

    	public int getOrder()
    	{
    		return this.order;
    	}

    	public String getName()
    	{
    		String name = this.element;
    		if (this.qualifier != null)
    		{
    			name += "." + this.qualifier;
    			if (this.language != null)
    			{
    				name += "." + this.language;
    			}
    		}

    		name += "#" + order;
    		return name;
    	}

    	public String getValue()
    	{
    		return this.value;
    	}

        @Override
    	public int compareTo(Metadata other)
    	{
    		String myName = this.element     + "." +this.qualifier   + "." + this.language;
    		String otherName = other.element + "." + other.qualifier + "." + other.language;

    		int result = myName.compareTo(otherName);
    		if (result == 0)
    		{
    			if (this.order == other.order )
    			{
    				result = 0; // These two metadata element's names are completely identical.
    			}
    			else if (this.order > other.order)
    			{
    				result = 1; // The other metadata element belongs AFTER this element.
    			}
    			else
    			{
    				result = -1; // The other metadata element belongs BEFORE this element.
    			}
    		}

    		return result;
    	}
    }

}
