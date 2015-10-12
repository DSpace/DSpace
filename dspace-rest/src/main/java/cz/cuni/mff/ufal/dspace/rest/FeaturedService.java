package cz.cuni.mff.ufal.dspace.rest;

import java.util.Hashtable;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "featuredService")
public class FeaturedService{
    public String name;
    public String url;
    public String description;
    
    public Map<String, String> links;

    public FeaturedService() {}

    public FeaturedService(String name, String url, String description){
        this.name = name;
        this.url = url;
        this.description = description;
        links = new Hashtable<String, String>();
    }
    
    public void addLink(String key, String value) {
    	links.put(key, value);
    }
}
