/**
 * A RESTful web service on top of DSpace.
 * Copyright (C) 2010-2011 National Library of Finland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package fi.helsinki.lib.simplerest;

import org.dspace.core.Context;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.EPerson;

import java.sql.SQLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.HashSet;

import org.restlet.resource.ServerResource;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.representation.InputRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put; 
import org.restlet.resource.Post;
import org.restlet.resource.Delete;
import org.restlet.resource.ResourceException;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.data.Method;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;

public class CommunityResource extends BaseResource {

    private static Logger log = Logger.getLogger(CommunityResource.class);
    private int communityId;

    static public String relativeUrl(int communityId) {
        return "community/" + Integer.toString(communityId);
    }
    
    @Override
    protected void doInit() throws ResourceException {
        try {
            String id = (String)getRequest().getAttributes().get("communityId");
            this.communityId = Integer.parseInt(id);
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "Community ID must be a number.");
            throw resourceException;
        }
    }

    // TODO: parent?
    @Get("xml")
    public Representation toXml() {
        Context c = null;
        Community community = null;
        DomRepresentation representation = null;
        Document d = null;
        try {
            c = new Context();
            community = Community.find(c, this.communityId);
            if (community == null) {
                return errorNotFound(c, "Could not find the community.");
            }

            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("Community " + community.getName()));

        Element body = d.createElement("body");
        html.appendChild(body);
	
        Element dl = d.createElement("dl");
        setId(dl, "attributes");
        body.appendChild(dl);

        Element dtName = d.createElement("dt");
        dtName.appendChild(d.createTextNode("name"));
        dl.appendChild(dtName);
        Element ddName = d.createElement("dd");
        ddName.appendChild(d.createTextNode(community.getName()));
        dl.appendChild(ddName);

        String[] attributes = { "short_description", "introductory_text",
                                "copyright_text", "side_bar_text" };
        for (String attribute : attributes) {
            Element dt = d.createElement("dt");
            dt.appendChild(d.createTextNode(attribute));
            dl.appendChild(dt);

            Element dd = d.createElement("dd");
            dd.appendChild(d.createTextNode(community.getMetadata(attribute)));
            dl.appendChild(dd);
        }

        Bitstream logo = community.getLogo();
        if (logo != null) {
            Element aLogo = d.createElement("a");
            String url = baseUrl() +
                CommunityLogoResource.relativeUrl(this.communityId);
            //getRequest().getResourceRef().getIdentifier() + "/logo";
            setAttribute(aLogo, "href", url);
            setId(aLogo, "logo");
            aLogo.appendChild(d.createTextNode("Community logo"));
            body.appendChild(aLogo);
        }

        String url = getRequest().getResourceRef().getIdentifier();

	// A link to sub communities
        Element pSubCommunities = d.createElement("p");
        Element aSubCommunities = d.createElement("a");
	setAttribute(aSubCommunities, "href", url + "/communities");
        setId(aSubCommunities, "communities");
	aSubCommunities.appendChild(d.createTextNode("communities"));
        pSubCommunities.appendChild(aSubCommunities);
        body.appendChild(pSubCommunities);

	// A link to child collections
        Element pSubCollections = d.createElement("p");
        Element aSubCollections = d.createElement("a");
	setAttribute(aSubCollections, "href", url + "/collections");
        setId(aSubCollections, "collections");
	aSubCollections.appendChild(d.createTextNode("collections"));
        pSubCollections.appendChild(aSubCollections);
        body.appendChild(pSubCollections);

	c.abort(); // Same as c.complete() because we didn't modify the db.

        return representation;
    }

    @Put
    public Representation edit(InputRepresentation rep) {
        Context c = null;
        Community community;
        try {
            c = getAuthenticatedContext();
            community = Community.find(c, this.communityId);
            if (community == null) {
                return errorNotFound(c, "Could not find the community.");
            }
        }
        catch (SQLException e) {
            return errorInternal(c, "SQLException");
        }

        DomRepresentation dom = new DomRepresentation(rep);

        Node attributesNode = dom.getNode("//dl[@id='attributes']");
        if (attributesNode == null) {
            return error(c, "Did not find dl tag with an id 'attributes'.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
	
        // TODO: Comment!
        community.setMetadata("name", null);
        community.setMetadata("short_description", null);
        community.setMetadata("introductory_text", null);
        community.setMetadata("copyright_text", null);
        community.setMetadata("side_bar_text", null);

        NodeList nodes = attributesNode.getChildNodes();
	LinkedList<String> dtList = new LinkedList();
	LinkedList<String> ddList = new LinkedList();
	int nNodes = nodes.getLength();
	for (int i=0; i < nNodes; i++) {
	    Node node = nodes.item(i);
	    String nodeName = node.getNodeName();
	    if (nodeName.equals("dt")) {
		dtList.add(node.getTextContent());
	    }
	    else if (nodeName.equals("dd")) {
		ddList.add(node.getTextContent());
	    }
	}
	if (dtList.size() != ddList.size()) {
	    return error(c, "The number of <dt> and <dd> elements do not match.",
			 Status.CLIENT_ERROR_BAD_REQUEST);
	}
        int size = dtList.size();
        for (int i=0; i < size; i++) {
            String dt = dtList.get(i);
            String dd = ddList.get(i);
            if (dt.equals("name") ||
                dt.equals("short_description") ||
                dt.equals("introductory_text") ||
                dt.equals("copyright_text") ||
                dt.equals("side_bar_text")) {
                community.setMetadata(dt, dd);
            }
            else {
                return error(c, "Unexpected data in attributes: " + dt,
                             Status.CLIENT_ERROR_BAD_REQUEST);
	    }
	}

        try {
            community.update();
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Community updated.");
    }

    @Post
    public Representation post(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.PUT);
        allowed.add(Method.DELETE);
        setAllowedMethods(allowed);
        return error(null,
                     "Community resource does not allow POST method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    @Delete
    public Representation delete() {
        Context c = null;
        Community community;
        try {
            c = getAuthenticatedContext();
            community = Community.find(c, this.communityId);
            if (community == null) {
                return errorNotFound(c, "Could not find the community.");
            }

            community.delete();
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Community deleted.");
    }
}
