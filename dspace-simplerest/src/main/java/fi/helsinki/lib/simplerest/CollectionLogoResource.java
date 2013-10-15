/**
 A RESTful web service on top of DSpace.
 The contents of this file are subject to the license and copyright
 detailed in the LICENSE and NOTICE files at the root of the source
 tree and available online at
 http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import java.sql.SQLException;
import java.io.InputStream;
import java.util.HashSet;

import org.dspace.core.Context;
import org.dspace.content.Collection;
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

public class CollectionLogoResource extends BaseResource {

    private static Logger log = Logger.getLogger(CollectionLogoResource.class);
    
    private int collectionId;

    static public String relativeUrl(int collectionId) {
        return "collection/" + collectionId + "/logo";
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
                                      "Could not convert collection id " +
                                      "to an integer.");
            throw resourceException;
        }
    }

    @Get
    public Representation get() {
        Context c = null;
        Collection collection = null;
        try {
            c = new Context();
            collection = Collection.find(c, this.collectionId);
            if (collection == null) {
                return errorNotFound(c, "Could not find the collection.");
            }
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }
        
        InputStream inputStream = null;
        Bitstream logo = null;
        try {
            logo = collection.getLogo();
            if (logo == null) {
                return errorNotFound(c, "The collection has no logo.");
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
        Collection collection;
        try {
            c = getAuthenticatedContext();
            collection = Collection.find(c, this.collectionId);
            if (collection == null) {
                return errorNotFound(c, "Could not find the collection.");
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
            
                    collection.setLogo(inputStream);
                    Bitstream logo = collection.getLogo();
                    BitstreamFormat bf =
                        BitstreamFormat.findByMIMEType(c,
                                                       item.getContentType());
                    logo.setFormat(bf);
                    logo.update();
                    collection.update();
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
                     "Collection logo resource does not allow POST method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }
    
    @Delete
    public Representation delete() {
        Context c = null;
        Collection collection;
        try {
            c = getAuthenticatedContext();
            collection = Collection.find(c, this.collectionId);
            if (collection == null) {
                return errorNotFound(c, "Could not find the collection.");
            }

            collection.setLogo(null);
            collection.update();
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Logo deleted.");
    }

}
