/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.data;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.lyncode.xoai.dataprovider.data.About;
import com.lyncode.xoai.dataprovider.data.Item;
import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Element.Field;
import org.dspace.core.ConfigurationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public abstract class DSpaceItem implements Item
{
	private static List<Element> filter (List<Element> input, String name) {
    	return Lists.newArrayList(Collections2.filter(input, new MetadataNamePredicate(name)));
    }
    
    private static List<Element> flat (List<Element> input) {
    	List<Element> elems = new ArrayList<Element>();
    	for (Element e : input) {
    		if (e.getElement() != null) {
    			elems.addAll(e.getElement());
    		}
    	}
    	return elems;
    }
    
    private static List<String> values (List<Element> input) {
    	List<String> elems = new ArrayList<String>();
    	for (Element e : input)
    		if (e.getElement() != null && !e.getElement().isEmpty() && e.getElement().get(0).getField() != null)
    			for (Field f : e.getElement().get(0).getField())
    				if (f.getName() != null && f.getName().equals("value"))
    					elems.add(f.getValue());
    	return elems;
    }
    

    private List<String> getMetadata (String schema, String element) {
    	List<Element> metadata = this.getMetadata().getMetadata().getElement();
    	return values(filter(flat(filter(metadata, schema)), element));
    }
    
    private List<String> getMetadata (String schema, String element, String qualifier) {
    	List<Element> metadata = this.getMetadata().getMetadata().getElement();
    	return values(filter(flat(filter(flat(filter(metadata, schema)), element)), qualifier));
    }

    
    private static String _prefix = null;
    public static String buildIdentifier (String handle) {
        if (_prefix == null)
        {
            _prefix = ConfigurationManager.getProperty("oai",
                    "identifier.prefix");
        }
        return "oai:" + _prefix + ":" + handle;
    }
    public static String parseHandle (String oaiIdentifier) {
    	String[] parts = oaiIdentifier.split(Pattern.quote(":"));
    	if (parts.length > 0) return parts[parts.length - 1];
    	else return null; // Contract
    }
    
    public List<String> getMetadata(String field)
    {
        String[] parts = field.split(Pattern.quote("."));
        if (parts.length == 2) return this.getMetadata(parts[0], parts[1]);
        else if (parts.length == 3) return this.getMetadata(parts[0], parts[1], parts[2]);
        else return new ArrayList<String>();
    }
    
    @Override
    public List<About> getAbout()
    {
        return new ArrayList<About>();
    }
    
    protected abstract String getHandle ();

    @Override
    public String getIdentifier()
    {
    	return buildIdentifier(getHandle());
    }

    private static class MetadataNamePredicate implements Predicate<Element> {
        private String name;

        public MetadataNamePredicate (String n) {
            name = n;
        }

        @Override
        public boolean apply(Element arg0) {
            if (name == null) return false;
            else if (name.equals(org.dspace.content.Item.ANY)) return true;
            else return (name.toLowerCase().equals(arg0.getName().toLowerCase()));
        }
    }
}
