/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.apache.log4j.Logger;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 5/22/13
 * Time: 9:41 AM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "community")
public class Community extends DSpaceObject{
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    private static Logger log = Logger.getLogger(Community.class);

    //Exandable relationships
    private Bitstream logo;

    private Community parentCommunity;

    private String copyrightText, introductoryText, shortDescription, sidebarText;
    private Integer countItems;

    private List<Community> subcommunities = new ArrayList<Community>();

    private List<Collection> collections = new ArrayList<Collection>();

    public Community(){}

    public Community(org.dspace.content.Community community, ServletContext servletContext, String expand, Context context) throws SQLException, WebApplicationException{
        super(community, servletContext);
        setup(community,servletContext, expand, context);
    }

    private void setup(org.dspace.content.Community community, ServletContext servletContext, String expand, Context context) throws SQLException{
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        this.setCopyrightText(communityService.getMetadata(community, org.dspace.content.Community.COPYRIGHT_TEXT));
        this.setIntroductoryText(communityService.getMetadata(community, org.dspace.content.Community.INTRODUCTORY_TEXT));
        this.setShortDescription(communityService.getMetadata(community, org.dspace.content.Community.SHORT_DESCRIPTION));
        this.setSidebarText(communityService.getMetadata(community, org.dspace.content.Community.SIDEBAR_TEXT));
        this.setCountItems(itemService.countItems(context, community));

        if(expandFields.contains("parentCommunity") || expandFields.contains("all")) {
            org.dspace.content.Community parentCommunity = (org.dspace.content.Community) communityService.getParentObject(context, community);
            if(parentCommunity != null) {
                setParentCommunity(new Community(parentCommunity,servletContext, null, context));
            }
        } else {
            this.addExpand("parentCommunity");
        }

        if(expandFields.contains("collections") || expandFields.contains("all")) {
            List<org.dspace.content.Collection> collections = community.getCollections();
            List<org.dspace.rest.common.Collection> restCollections = new ArrayList<>();

            for(org.dspace.content.Collection collection : collections) {
                if(authorizeService.authorizeActionBoolean(context, collection, org.dspace.core.Constants.READ)) {
                    restCollections.add(new Collection(collection,servletContext, null, context, null, null));
                } else {
                    log.info("Omitted restricted collection: " + collection.getID() + " _ " + collection.getName());
                }
            }
            setCollections(restCollections);
        } else {
            this.addExpand("collections");
        }

        if(expandFields.contains("subCommunities") || expandFields.contains("all")) {
            List<org.dspace.content.Community> communities = community.getSubcommunities();
            subcommunities = new ArrayList<Community>();
            for(org.dspace.content.Community subCommunity : communities) {
                if(authorizeService.authorizeActionBoolean(context, subCommunity, org.dspace.core.Constants.READ)) {
                	subcommunities.add(new Community(subCommunity, servletContext, null, context));
                } else {
                    log.info("Omitted restricted subCommunity: " + subCommunity.getID() + " _ " + subCommunity.getName());
                }
            }
        } else {
            this.addExpand("subCommunities");
        }

        if(expandFields.contains("logo") || expandFields.contains("all")) {
            if(community.getLogo() != null) {
                logo = new Bitstream(community.getLogo(),servletContext, null, context);
            }
        } else {
            this.addExpand("logo");
        }

        if(!expandFields.contains("all")) {
            this.addExpand("all");
        }
    }

    public List<Collection> getCollections() {
        return collections;
    }

    public void setCollections(List<Collection> collections) {
        this.collections = collections;
    }

    public Integer getCountItems() {
        return countItems;
    }

    public void setCountItems(Integer countItems) {
        this.countItems = countItems;
    }

    public String getSidebarText() {
        return sidebarText;
    }

    public void setSidebarText(String sidebarText) {
        this.sidebarText = sidebarText;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getIntroductoryText() {
        return introductoryText;
    }

    public void setIntroductoryText(String introductoryText) {
        this.introductoryText = introductoryText;
    }

    public String getCopyrightText() {
        return copyrightText;
    }

    public void setCopyrightText(String copyrightText) {
        this.copyrightText = copyrightText;
    }

    public Community getParentCommunity() {
        return parentCommunity;
    }

    public void setParentCommunity(Community parentCommunity) {
        this.parentCommunity = parentCommunity;
    }

    public Bitstream getLogo() {
        return logo;
    }

    public void setLogo(Bitstream logo) {
        this.logo = logo;
    }

    // Renamed because of xml annotation exception with this attribute and getSubCommunities.
    @XmlElement(name = "subcommunities", required = true)
	public List<Community> getSubcommunities() {
		return subcommunities;
	}
	
	public void setSubcommunities(List<Community> subcommunities) {
		this.subcommunities = subcommunities;
	}
    
}
