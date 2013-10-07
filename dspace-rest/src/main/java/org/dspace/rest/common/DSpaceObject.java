package org.dspace.rest.common;

import org.atteo.evo.inflector.English;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 10/7/13
 * Time: 12:11 PM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "dspaceobject")
public class DSpaceObject {
    @XmlElement()
    public Integer ID;

    @XmlElement()
    public String name;

    @XmlElement()
    public String handle;

    @XmlElement()
    public String type;

    @XmlElement()
    public String link;

    @XmlElement()
    public List<String> expand = new ArrayList<String>();

    public DSpaceObject() {

    }

    public DSpaceObject(org.dspace.content.DSpaceObject dso) {
        setID(dso.getID());
        setName(dso.getName());
        setHandle(dso.getHandle());
        setType(dso.getTypeText().toLowerCase());
    }

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public String getName(){
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getLink() {
        //TODO, I'm not 100% sure this pluralizer will work...
        //How to get actual contextPath of /rest/
        return "/rest/" + English.plural(getType()) + "/" + getID();
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public List<String> getExpand() {
        return expand;
    }

    public void setExpand(List<String> expand) {
        this.expand = expand;
    }

    public void addExpand(String expandableAttribute) {
        this.expand.add(expandableAttribute);
    }
}
