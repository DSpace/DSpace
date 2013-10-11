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

import java.sql.SQLException;
import java.io.InputStream;
import java.util.HashSet;

import org.dspace.core.Context;
import org.dspace.content.Community;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;

import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.Post;
import org.restlet.resource.Delete;
import org.restlet.resource.ResourceException;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.data.Method;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

public class CommunityLogoResource extends BaseResource {

    private static Logger log = Logger.getLogger(CommunityLogoResource.class);
    
    private int communityId;

    static public String relativeUrl(int communityId) {
        return "community/" + communityId + "/logo";
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
                                      "Could not convert community id " +
                                      "to an integer.");
            throw resourceException;
        }
    }

    @Get
    public Representation get() {
        Context c = null;
        Community community;
        try {
            c = new Context();
            community = Community.find(c, this.communityId);
            if (community == null) {
                return errorNotFound(c, "Could not find the community.");
            }
        }
        catch (SQLException e) {
            return errorInternal(c, e.toString());
        }

        InputStream inputStream = null;
        Bitstream logo = null;
        try {
            logo = community.getLogo();
            if (logo == null) {
                return errorNotFound(c, "The community has no logo.");
            }
            inputStream = logo.retrieve();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        MediaType mediaType = MediaType.valueOf(logo.getFormat().getMIMEType());
        c.abort();
        return new BinaryRepresentation(mediaType, inputStream);
    }

    @Put
    public Representation put(Representation logoRepresentation) {
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
            return errorInternal(c, e.toString());
        }

        try {
            RestletFileUpload rfu =
                new RestletFileUpload(new DiskFileItemFactory());
            FileItemIterator iter = rfu.getItemIterator(logoRepresentation);
            if (iter.hasNext()) {
                FileItemStream item = iter.next();
                if (!item.isFormField()) {
                    InputStream inputStream = item.openStream();
            
                    community.setLogo(inputStream);
                    Bitstream logo = community.getLogo();
                    BitstreamFormat bf =
                        BitstreamFormat.findByMIMEType(c,
                                                       item.getContentType());
                    logo.setFormat(bf);
                    logo.update();
                    community.update();
                }
            }
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Logo set.");
    }

    @Post
    public Representation post(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.PUT);
        allowed.add(Method.DELETE);
        setAllowedMethods(allowed);
        return error(null,
                     "Community logo resource does not allow POST method.",
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

            community.setLogo(null);
            community.update();
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Logo deleted.");
    }

}
