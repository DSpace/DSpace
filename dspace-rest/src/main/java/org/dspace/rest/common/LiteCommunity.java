package org.dspace.rest.common;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/29/13
 * Time: 11:27 AM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "community")
public class LiteCommunity {
    @XmlElement(name = "type", required = true)
    final String type = "community";

    @XmlElement(name = "communityID", required = true)
    Integer communityID;

    @XmlElement(name = "handle", required = true)
    String handle;

    @XmlElement(name = "name", required = true)
    String name;

    @XmlElement(name = "link", required = true)
    private String link;


    public LiteCommunity() {

    }

    public LiteCommunity(org.dspace.content.Community community) {
        this.communityID = community.getID();
        this.handle = community.getHandle();
        this.name = community.getName();
        this.link = "/communities/" + this.communityID;
    }


    String getType() {
        return type;
    }

    String getHandle() {
        return handle;
    }

    void setHandle(String handle) {
        this.handle = handle;
    }

    Integer getCommunityID() {
        return communityID;
    }

    void setCommunityID(Integer communityID) {
        this.communityID = communityID;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }
}
