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

import java.util.HashSet;

import org.dspace.core.Context;
import org.dspace.content.Community;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.FormatIdentifier;
import org.dspace.authorize.AuthorizeException;

import org.restlet.ext.xml.DomRepresentation;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.representation.InputRepresentation;
import org.restlet.resource.Get; 
import org.restlet.resource.Put;
import org.restlet.resource.Post;
import org.restlet.resource.Delete;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.data.Method;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMException;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;

public class RootCommunitiesResource extends BaseResource {

    private static Logger log = Logger.getLogger(RootCommunitiesResource.class);

    static public String relativeUrl(int dummy) {
        return "rootcommunities";
    }
    
    @Get("xml")
    public Representation toXml() {
        Context c = null;
        Community[] communities;
        DomRepresentation representation;
        Document d;
        try {
            c = new Context();
            communities = Community.findAllTop(c);

            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        representation.setIndent(true);

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("Root communities"));

        Element body = d.createElement("body");
        html.appendChild(body);

        Element ul = d.createElement("ul");
        setId(ul, "rootcommunities");
        body.appendChild(ul);

        String url = getRequest().getResourceRef().getIdentifier();
        url = url.substring(0, url.lastIndexOf('/') + 1);
        url += "community/";
        for (Community community : communities) {

            Element li = d.createElement("li");
            Element a = d.createElement("a");

            a.setAttribute("href", url + Integer.toString(community.getID()));
            a.appendChild(d.createTextNode(community.getName()));
            li.appendChild(a);
            ul.appendChild(li);
        }

        Element form = d.createElement("form");
        form.setAttribute("enctype", "multipart/form-data");
        form.setAttribute("method", "post");
        makeInputRow(d, form, "name", "Name");
        makeInputRow(d, form, "short_description", "Short description");
        makeInputRow(d, form, "introductory_text", "Introductory text");
        makeInputRow(d, form, "copyright_text", "Copyright text");
        makeInputRow(d, form, "side_bar_text", "Side bar text");
        makeInputRow(d, form, "logo", "Logo", "file");

        Element submitButton = d.createElement("input");
        submitButton.setAttribute("type", "submit");
        submitButton.setAttribute("value", "Create a new root community.");
	
        form.appendChild(submitButton);
        body.appendChild(form);

        c.abort();
        return representation;
    }

    @Put
    public Representation put(Representation dummy) {
        return errorUnallowedMethod("PUT");
    }

    @Post
    public Representation addCommunity(InputRepresentation rep) {
        Context c = null;
        Community community;
        try {
            c = getAuthenticatedContext();
            community = Community.create(null, c);

            RestletFileUpload rfu =
                new RestletFileUpload(new DiskFileItemFactory());
            FileItemIterator iter = rfu.getItemIterator(rep);

            String name = null;
            String shortDescription = null;
            String introductoryText = null;
            String copyrightText = null;
            String sideBarText = null;
            Bitstream bitstream = null;
            String bitstreamMimeType = null;
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
                    else {
                        return error(c, "Unexpected attribute: " + key,
                                     Status.CLIENT_ERROR_BAD_REQUEST);
                    }
                } else {
                    if (bitstream != null) {
                        return error(c, "The community can have only one logo.",
                                     Status.CLIENT_ERROR_BAD_REQUEST);
                    }

                    // I did not manage to use FormatIdentifier.guessFormat
                    // here, so let's do it by ourselves... I would prefer to
                    // use the actual file content and not its name, but let's
                    // keep the code simple...
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

                    bitstream = community.setLogo(fileItemStream.openStream());
                    // We don't set the format of the logo (bitstream) here,
                    // it's done in the code below...
                }
            }

            community.setMetadata("name", name);
            community.setMetadata("short_description", shortDescription);
            community.setMetadata("introductory_text", introductoryText);
            community.setMetadata("copyright_text", copyrightText);
            community.setMetadata("side_bar_text", sideBarText);

            community.update();

            // Set the format (jpeg, png, or gif) of logo:
            Bitstream logo = community.getLogo();
            if (logo != null) {
                BitstreamFormat bf =
                    BitstreamFormat.findByMIMEType(c, bitstreamMimeType);
                logo.setFormat(bf);
                logo.update();
            }
	    
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successCreated("Community created.",
                              baseUrl() +
                              CommunityResource.relativeUrl(community.getID()));
    }
    
    @Delete
    public Representation delete() {
        return errorUnallowedMethod("DELETE");
    }

    private Representation errorUnallowedMethod(String unallowedMethod) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.POST);
        setAllowedMethods(allowed);
        return error(null, "Root communities resource does not allow " +
                     unallowedMethod + " method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }
}
