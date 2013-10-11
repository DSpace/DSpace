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
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import org.apache.log4j.Logger;

public class GroupResource extends BaseResource {

    private static Logger log = Logger.getLogger(GroupResource.class);

    private int groupId;

    @Override
    protected void doInit() throws ResourceException {
        try {
            String s = (String)getRequest().getAttributes().get("groupId");
            this.groupId = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "Group ID must be a number.");
            throw resourceException;
        }
    }

    static public String relativeUrl(int groupId) {
        return "group/" + Integer.toString(groupId);
    }

    @Get("xml")
    public Representation toXml() {
        Context c = null;
        DomRepresentation representation = null;
        Document d = null;
	Group group = null;
	Group groups[] = null;
	EPerson epersons[] = null;
	
        try {
            c = new Context();
            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
	    group = Group.find(c,groupId);
	    groups = group.getMemberGroups();
	    epersons = group.getMembers();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        title.appendChild(d.createTextNode(group.getName()));
        head.appendChild(title);

        Element body = d.createElement("body");
        html.appendChild(body);
	
	Element h1 = d.createElement("h1");
        body.appendChild(h1);
	h1.appendChild(d.createTextNode(group.getName()));

	Element ul1 = d.createElement("ul");
	ul1.setAttribute("id","membergroupss");
        body.appendChild(ul1);

	for (Group subgroup : groups) {
	    Element li = d.createElement("li");
	    Element a = d.createElement("a");
	    ul1.appendChild(li);
	    li.appendChild(a);
	    String url = baseUrl() +
		GroupResource.relativeUrl(subgroup.getID());
	    a.setAttribute("href",url);
	    Text text = d.createTextNode(subgroup.getName());
	    a.appendChild(text);
	}

	Element ul2 = d.createElement("ul");
	ul2.setAttribute("id","members");
        body.appendChild(ul2);

	for (EPerson eperson : epersons) {
	    Element li = d.createElement("li");
	    Element a = d.createElement("a");
	    ul2.appendChild(li);
	    li.appendChild(a);
	    String url = baseUrl() +
		UserResource.relativeUrl(eperson.getID());
	    a.setAttribute("href",url);
	    Text text = d.createTextNode(eperson.getName());
	    a.appendChild(text);
	}
	c.abort(); // Same as c.complete() because we didn't modify the db.

        return representation;
    }

}
