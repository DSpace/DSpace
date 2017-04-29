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

public class UserResource extends BaseResource {

    private static Logger log = Logger.getLogger(UserResource.class);

    private int userId;

    @Override
    protected void doInit() throws ResourceException {
        try {
            String s = (String)getRequest().getAttributes().get("userId");
            this.userId = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "User ID must be a number.");
            throw resourceException;
        }
    }

    static public String relativeUrl(int userId) {
        return "user/" + Integer.toString(userId);
    }

    @Get("xml")
    public Representation toXml() {
        Context c = null;
        DomRepresentation representation = null;
        Document d = null;
	EPerson eperson = null;
	
        try {
            c = new Context();
            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
	    eperson = EPerson.find(c,userId);
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        title.appendChild(d.createTextNode("User" + Integer.toString(userId)));
        head.appendChild(title);

        Element body = d.createElement("body");
        html.appendChild(body);
	
	Element dl = d.createElement("dl");
	dl.setAttribute("id","attributes");
        body.appendChild(dl);

	createAttribute(d,dl,"email",eperson.getEmail());
	createAttribute(d,dl,"id",Integer.toString(eperson.getID()));
	createAttribute(d,dl,"language",eperson.getLanguage());
	createAttribute(d,dl,"netid",eperson.getNetid());
	createAttribute(d,dl,"fullname",eperson.getFullName());
	createAttribute(d,dl,"firstname",eperson.getFirstName());
	createAttribute(d,dl,"lastname",eperson.getLastName());
	createAttribute(d,dl,"can login",Boolean.toString(eperson.canLogIn()));
	createAttribute(d,dl,"require certificate",Boolean.toString(eperson.getRequireCertificate()));
	createAttribute(d,dl,"self registered",Boolean.toString(eperson.getSelfRegistered()));
	createAttribute(d,dl,"password",eperson.getMetadata("password"));
	c.abort(); // Same as c.complete() because we didn't modify the db.

        return representation;
    }

    private void createAttribute(Document d, Element parent, String name,
				    String content) {
	if (content == null) return;
	Element dt = d.createElement("dt");
	Element dd = d.createElement("dd");
	dt.appendChild(d.createTextNode(name));
	dd.appendChild(d.createTextNode(content));
	parent.appendChild(dt);
	parent.appendChild(dd);
    }

}
