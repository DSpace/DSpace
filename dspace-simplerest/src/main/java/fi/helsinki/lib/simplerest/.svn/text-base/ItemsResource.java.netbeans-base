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

import java.sql.SQLException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.HashSet;

import org.dspace.core.Context;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.InstallItem;
import org.dspace.content.ItemIterator;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;
import org.dspace.authorize.AuthorizeException;

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
import org.restlet.data.Form;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;

public class ItemsResource extends BaseResource {

    private static Logger log = Logger.getLogger(ItemResource.class);
    
    private int collectionId;

    static public String relativeUrl(int collectionId) {
        return "collection/" + Integer.toString(collectionId) + "/items";
    }
    
    @Override
    protected void doInit() throws ResourceException {
        try {
            String s = (String)getRequest().getAttributes().get("collectionId");
            this.collectionId = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "Collection ID must be a number.");
            throw resourceException;
        }
    }

    @Get("xml")
    public Representation toXml() {
        Context c = null;
        Collection collection = null;
        DomRepresentation representation = null;
        Document d = null;
        try {
            c = new Context();
            collection = Collection.find(c, this.collectionId);
            if (collection == null) {
                return errorNotFound(c, "Could not find the collection.");
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
        title.appendChild(d.createTextNode("Items for collection " +
                                           collection.getName()));

        Element body = d.createElement("body");
        html.appendChild(body);
	
        Element ulItems = d.createElement("ul");
        setId(ulItems, "items");
        body.appendChild(ulItems);
        
        String base = baseUrl();
        try {
            ItemIterator ii = collection.getItems();
            while (ii.hasNext()) {
                Item item = ii.next();
                Element li = d.createElement("li");
                Element a = d.createElement("a");
                String name = item.getName();
                if (name == null) {
                    // FIXME: Should we really give names for items with no
                    // FIXME: name? (And if so does "Untitled" make sense?)
                    // FIXME: Anyway, this would break with null values.
                    name = "Untitled";
                }
                a.appendChild(d.createTextNode(name));
                String href = base + ItemResource.relativeUrl(item.getID());
                setAttribute(a, "href", href);
                li.appendChild(a);
                ulItems.appendChild(li);
            }
        }
        catch (SQLException e) {
            String errMsg =
                "SQLException while trying to items of the collection.";
            return errorInternal(c, errMsg);
        }

        Element form = d.createElement("form");
        form.setAttribute("enctype", "multipart/form-data");
        form.setAttribute("method", "post");
        makeInputRow(d, form, "title", "Title");
        makeInputRow(d, form, "lang", "Language");

        Element submitButton = d.createElement("input");
        submitButton.setAttribute("type", "submit");
        submitButton.setAttribute("value", "Create a new item");
        form.appendChild(submitButton);
        
        body.appendChild(form);

        c.abort(); /* We did not make any changes to the database, so we could
                      call c.complete() instead (only it can potentially raise
                      SQLexception). */

        return representation;
    }

    @Put
    public Representation put(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.POST);
        setAllowedMethods(allowed);
        return error(null,
                     "Items resource does not allow PUT method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    @Post
	public Representation addItem(InputRepresentation rep) {
	Context c = null;
	Collection collection;
	try {
	    c = getAuthenticatedContext();
	    collection = Collection.find(c, this.collectionId);
	    if (collection == null) {
		return errorNotFound(c, "Could not find the collection.");
	    }
	}
	catch (SQLException e) {
	    return errorInternal(c, "SQLException");
	}
	String title = null;
	String lang = null;

	try {
	    RestletFileUpload rfu =
		new RestletFileUpload(new DiskFileItemFactory());
	    FileItemIterator iter = rfu.getItemIterator(rep);

	    while (iter.hasNext()) {
		FileItemStream fileItemStream = iter.next();
		if (fileItemStream.isFormField()) {
		    String key = fileItemStream.getFieldName();
		    String value =
			IOUtils.toString(fileItemStream.openStream(), "UTF-8");

		    if (key.equals("title")) {
			title = value;
		    }
		    else if (key.equals("lang")) {
			lang = value;
		    }
		    else if (key.equals("in_archive")) {
			;
		    }
		    else if (key.equals("withdrawn")) {
			;
		    }
		    else {
			return error(c, "Unexpected attribute: " + key,
				     Status.CLIENT_ERROR_BAD_REQUEST);
		    }
		}
	    }
	}
	catch (Exception e) {
	    return errorInternal(c, e.toString());
	}

	if (title == null) {
	    return error(c, "There was no title given.",
			 Status.CLIENT_ERROR_BAD_REQUEST);
	}

	Item item = null;
	try {
	    WorkspaceItem wsi = WorkspaceItem.create(c, collection, false);
	    item = InstallItem.installItem(c, wsi);
	    item.addMetadata("dc", "title", null, lang, title);
	    item.update();
	    c.complete();
	}
	catch (Exception e) {
	    return errorInternal(c, e.toString());
	}

	return successCreated("Created a new item.",
			      baseUrl() +
			      ItemResource.relativeUrl(item.getID()));
    }

    @Delete
    public Representation delete(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.POST);
        setAllowedMethods(allowed);
        return error(null,
                     "Items resource does not allow DELETE method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

}
