/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import org.dspace.core.Context;
import org.dspace.eperson.Group;

import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.data.MediaType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import org.apache.log4j.Logger;

public class GroupsResource extends BaseResource {

    private static Logger log = Logger.getLogger(GroupsResource.class);

    static public String relativeUrl(int dummy) {
        return "groups";
    }

    @Get("xml")
    public Representation toXml() {
        Context c = null;
        DomRepresentation representation = null;
        Document d = null;
	Group groups[] = null;
	
        try {
            c = new Context();
            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
	    groups = Group.findAll(c,0);
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
	title.appendChild(d.createTextNode("Groups"));
        head.appendChild(title);

        Element body = d.createElement("body");
        html.appendChild(body);
	
	Element ul = d.createElement("ul");
	ul.setAttribute("id","groups");
        body.appendChild(ul);

	for (Group group : groups) {
	    Element li = d.createElement("li");
	    Element a = d.createElement("a");
	    ul.appendChild(li);
	    li.appendChild(a);
	    String url = baseUrl() +
		GroupResource.relativeUrl(group.getID());
	    a.setAttribute("href",url);
	    Text text = d.createTextNode(group.getName());
	    a.appendChild(text);
	}

	c.abort(); // Same as c.complete() because we didn't modify the db.

        return representation;
    }

}
