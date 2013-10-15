/**
 A RESTful web service on top of DSpace.
 The contents of this file are subject to the license and copyright
 detailed in the LICENSE and NOTICE files at the root of the source
 tree and available online at
 http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Site;

import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.data.MediaType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

	createItem(d,ul,"Root communities", "simplerest/"+RootCommunitiesResource.relativeUrl(0));
	createItem(d,ul,"Groups", "simplerest/"+GroupsResource.relativeUrl(0));
	createItem(d,ul,"Users", "simplerest/"+UsersResource.relativeUrl(0));

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
