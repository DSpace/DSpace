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
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;

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

public class RootResource extends BaseResource {

    private static Logger log = Logger.getLogger(UserResource.class);

    public String relativeUrl(int x) {
	return "";
    }

    @Get("xml")
    public Representation toXml() {
        Context c = null;
        DomRepresentation representation = null;
        Document d = null;
	DSpaceObject site;
	
        try {
            c = new Context();
	    site = Site.find(c,0);
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
        title.appendChild(d.createTextNode(site.getName()));
        head.appendChild(title);

        Element body = d.createElement("body");
        html.appendChild(body);
	
	Element ul = d.createElement("ul");
	ul.setAttribute("id","attributes");
        body.appendChild(ul);

	createItem(d,ul,"Root communities",RootCommunitiesResource.relativeUrl(0));
	createItem(d,ul,"Groups",GroupsResource.relativeUrl(0));
	createItem(d,ul,"Users",UsersResource.relativeUrl(0));

	c.abort(); // Same as c.complete() because we didn't modify the db.

        return representation;
    }

    private void createItem(Document d, Element parent, String name,
				    String relativeUrl) {
	Element li = d.createElement("li");
	Element a = d.createElement("a");
	String url = baseUrl() + relativeUrl;
	li.appendChild(a);
	a.setAttribute("href",url);
	a.appendChild(d.createTextNode(name));
	parent.appendChild(li);
    }

}
