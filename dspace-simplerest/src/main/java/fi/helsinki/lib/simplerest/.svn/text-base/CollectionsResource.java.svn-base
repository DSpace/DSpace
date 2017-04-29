/**
 * A RESTful web service on top of DSpace.
 * Copyright (C) 2010-2013 National Library of Finland
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

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;

public class CollectionsResource extends BaseResource {

    private static Logger log = Logger.getLogger(CollectionsResource.class);
    
    private int communityId;

    static public String relativeUrl(int communityId) {
        return "community/" + Integer.toString(communityId) + "/collections";
    }

    @Override
    protected void doInit() throws ResourceException {
        try {
            String s = (String)getRequest().getAttributes().get("communityId");
            this.communityId = Integer.parseInt(s);
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
	
        Collection[] collections;
        try {
            collections = community.getCollections();
        }
        catch (SQLException e) {
            return errorInternal(c, e.toString());
        }

        String url = getRequest().getResourceRef().getIdentifier();
	url = url.substring(0, url.lastIndexOf('/', url.lastIndexOf('/', url.lastIndexOf('/')-1)-1));
	url += "/collection/";

        Element ulCollections = d.createElement("ul");
        setId(ulCollections, "collections");
        body.appendChild(ulCollections);
        for (Collection collection : collections) {
            Element li = d.createElement("li");
            Element a = d.createElement("a");
            String href = url + Integer.toString(collection.getID());
            setAttribute(a, "href", href);
            a.appendChild(d.createTextNode(collection.getName()));
            li.appendChild(a);
            ulCollections.appendChild(li);
        }

        Element form = d.createElement("form");
        form.setAttribute("enctype", "multipart/form-data");
        form.setAttribute("method", "post");
        makeInputRow(d, form, "name", "Name");
        makeInputRow(d, form, "short_description", "Short description");
        makeInputRow(d, form, "introductory_text", "Introductory text");
        makeInputRow(d, form, "copyright_text", "Copyright text");
        makeInputRow(d, form, "side_bar_text", "Side bar text");
        makeInputRow(d, form, "provenance_description",
                     "Provenance Description");
        makeInputRow(d, form, "license", "License");
        makeInputRow(d, form, "logo", "Logo", "file");

        Element submitButton = d.createElement("input");
        submitButton.setAttribute("type", "submit");
        submitButton.setAttribute("value", "Create a new collection");
        form.appendChild(submitButton);

        body.appendChild(form);

	c.abort(); // Same as c.complete() because we didn't modify the db.

        return representation;
    }

    @Put
    public Representation put(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.POST);
        setAllowedMethods(allowed);
        return error(null,
                     "Collections resource does not allow PUT method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    @Post
    public Representation addCollection(InputRepresentation rep) {
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

        String msg = null;
        String url = baseUrl();
        try {
            RestletFileUpload rfu =
                new RestletFileUpload(new DiskFileItemFactory());
            FileItemIterator iter = rfu.getItemIterator(rep);

            // Logo
            String bitstreamMimeType = null;
            byte[] logoBytes = null;

            // Collection
            String name = null;
            String shortDescription = null;
            String introductoryText = null;
            String copyrightText = null;
            String sideBarText = null;
            String provenanceDescription = null;
            String license = null;

            while (iter.hasNext()) {
                FileItemStream fileItemStream = iter.next();
                if (fileItemStream.isFormField()) {
                    String key = fileItemStream.getFieldName();
                    String value =
                        IOUtils.toString(fileItemStream.openStream(), "UTF-8");

                    if (key.equals("name")) {
                        name = value;
                    }
                    else if (key.equals("short_description")) {
                        shortDescription= value;
                    }
                    else if (key.equals("introductory_text")) {
                        introductoryText = value;
                    }
                    else if (key.equals("copyright_text")) {
                        copyrightText = value;
                    }
                    else if (key.equals("side_bar_text")) {
                        sideBarText = value;
                    }
                    else if (key.equals("provenance_description")) {
                        provenanceDescription = value;
                    }
                    else if (key.equals("license")) {
                        license = value;
                    }
                    else {
                        return error(c, "Unexpected attribute: " + key,
                                     Status.CLIENT_ERROR_BAD_REQUEST);
                    }
                }
                else {
                    if (logoBytes != null) {
                        return error(c, "The collection can have only one logo.",
                                     Status.CLIENT_ERROR_BAD_REQUEST);
                    }

                    // TODO: Refer to comments in....
                    String fileName = fileItemStream.getName();
                    if (fileName.length() == 0) {
                        continue;
                    }
                    int lastDot = fileName.lastIndexOf('.');
                    if (lastDot != -1) {
                        String extension = fileName.substring(lastDot + 1);
                        extension = extension.toLowerCase();
                        if (extension.equals("jpg") ||
                            extension.equals("jpeg")) {
                            bitstreamMimeType = "image/jpeg";
                        }
                        else if (extension.equals("png")) {
                            bitstreamMimeType = "image/png";
                        }
                        else if (extension.equals("gif")) {
                            bitstreamMimeType = "image/gif";
                        }
                    }
                    if (bitstreamMimeType == null) {
                        String err = 
                            "The logo filename extension was not recognised.";
                        return error(c, err, Status.CLIENT_ERROR_BAD_REQUEST);
                    }

                    InputStream inputStream = fileItemStream.openStream();
                    logoBytes = IOUtils.toByteArray(inputStream);
                }
            }

	    Collection collection = community.createCollection();
	    collection.setMetadata("name", name);
	    collection.setMetadata("short_description", shortDescription);
	    collection.setMetadata("introductory_text", introductoryText);
	    collection.setMetadata("copyright_text", copyrightText);
	    collection.setMetadata("side_bar_text", sideBarText);
	    collection.setMetadata("provenance_description",
				   provenanceDescription);
	    collection.setMetadata("license", license);
	    if (logoBytes != null) {
		ByteArrayInputStream byteStream;
		byteStream = new ByteArrayInputStream(logoBytes);
		collection.setLogo(byteStream);
	    }
            
	    collection.update();
	    Bitstream logo = collection.getLogo();
	    if (logo != null) {
		BitstreamFormat bf =
		    BitstreamFormat.findByMIMEType(c, bitstreamMimeType);
		logo.setFormat(bf);
		logo.update();
	    }
	    url += CollectionResource.relativeUrl(collection.getID());
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successCreated("Collection created.", url);
    }

    @Delete
    public Representation delete(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.POST);
        setAllowedMethods(allowed);
        return error(null,
                     "Collections resource does not allow DELETE method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

}