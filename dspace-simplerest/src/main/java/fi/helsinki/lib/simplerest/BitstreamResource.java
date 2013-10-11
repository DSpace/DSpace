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

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.options.GetOptions;
import fi.helsinki.lib.simplerest.stubs.StubBitstream;
import java.sql.SQLException;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashSet;

import org.dspace.core.Context;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;

import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public class BitstreamResource extends BaseResource {

    private static Logger log = Logger.getLogger(BitstreamResource.class);
    
    private int bitstreamId;
    private Bitstream bitstream;
    private Context context;
    private boolean isBinary;

    static public String relativeUrl(int bitstreamId) {
        return "bitstream/" + bitstreamId;
    }
    
    public BitstreamResource(Bitstream b, int bitstreamId){
        this.bitstream = b;
        this.bitstreamId = bitstreamId;
    }
    
    public BitstreamResource(){
        this.bitstreamId = 0;
        this.bitstream = null;
        try{
            this.context = new Context();
        }catch(SQLException e){
            log.log(Priority.FATAL, e);
        }
    }
    
    @Override
    protected void doInit() throws ResourceException {
        try {
            String s = (String)
                getRequest().getAttributes().get("bitstreamIdDotFormat");
            if (s.endsWith(".bin")) {
                this.isBinary = true;
                s = s.substring(0, s.length()-4);
            }
            else {
                this.isBinary = false;
            };
            this.bitstreamId = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            String err = "Could not convert bitstream id to an integer.";
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, err);
        }
    }
    
    @Get("xml")
    public Representation get() {
        try {
            bitstream = Bitstream.find(context, this.bitstreamId);
            if (bitstream == null) {
                return errorNotFound(context, "Could not find the bitstream.");
            }
        }
        catch (Exception e) {
            if(this.bitstream == null){
                return errorNotFound(context, "Could not find the bitstream.");
            }
            log.log(Priority.INFO, e);
        }

        // When a bitstream is deleted, its row in the database is not removed,
        // instead its 'deleted' column is set to true. Unfortunaly, it seems
        // that DSpace API does contain method to read a value of 'deleted', so
        // as a workaround we try to retrieve the contents of the bitstream and
        // if that fails, we assume that bitstream does not exist anymore.
        // FIXME: *Maybe* we should do a similar check in other
        // FIXME: methods beside GET.
        
        log.log(Priority.INFO, bitstream.getType());
        log.log(Priority.INFO, bitstream.getSource());
        log.log(Priority.INFO, bitstream.isRegisteredBitstream());
        
        InputStream inputStream = null;
        try {
            inputStream = bitstream.retrieve();
        }
        catch (Exception e) {
            return errorNotFound(context, "The bitstream is probably deleted.");
        }

        Representation r =
            (this.isBinary) ? getBinary(bitstream) : getXml(bitstream);
        try{
            context.abort();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        return r;
    }

    private Representation getBinary(Bitstream bitstream) {
        String error = null;
        InputStream inputStream = null;
        try {
            inputStream = bitstream.retrieve();
        }
        catch (Exception e) {
            return errorInternal(null, e.toString());
        }

        MediaType mediaType = 
            MediaType.valueOf(bitstream.getFormat().getMIMEType());
        return new BinaryRepresentation(mediaType, inputStream);
    }

    private Representation getXml(Bitstream bitstream) {
        Bitstream[] bitstreams;
        DomRepresentation representation;
        Document d;
        try {
            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
        }
        catch(IOException e) {
            return errorInternal(null, "IOException");
        }

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("Bitstream " + bitstream.getName()));

        Element body = d.createElement("body");
        html.appendChild(body);

        Element dlAttributes = d.createElement("dl");
        setId(dlAttributes, "attributes");
        body.appendChild(dlAttributes);
	
        addDtDd(d, dlAttributes, "name", bitstream.getName());
        
        //We have to enclose this in a try statement as Mockito wont mock final methods (getMIMEType)
        //and we really want to test the rest of this method. We're using incorrect mimetype delibaretely for testing.
        String mime = "application/pdfs";
        try{
                mime = bitstream.getFormat().getMIMEType();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        addDtDd(d, dlAttributes, "mimetype", mime);
        addDtDd(d, dlAttributes, "description", bitstream.getDescription());
        addDtDd(d, dlAttributes, "userformatdescription",
                bitstream.getUserFormatDescription());
        addDtDd(d, dlAttributes, "source", bitstream.getSource());
        addDtDd(d, dlAttributes, "sequenceid",
                Integer.toString(bitstream.getSequenceID()));
        addDtDd(d, dlAttributes, "sizebytes",
                Long.toString(bitstream.getSize()));
        
        return representation;
    }
    
    @Get("json")
    public String toJson(){
        GetOptions.allowAccess(getResponse());
        try{
            bitstream = Bitstream.find(context, bitstreamId);
        }catch(Exception e){
            if(context != null)
                context.abort();
            log.log(Priority.FATAL, e);
        }finally{
            if(context != null)
                context.abort();
        }
        
        Gson gson = new Gson();
        //We have to enclose this in a try statement as Mockito wont mock final methods (getMIMEType)
        //and we really want to test the rest of this method. We're using incorrect mimetype deliberately for testing.
        String mime = "application/pdfs";
        try{
            mime = bitstream.getFormat().getMIMEType();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        StubBitstream s = new StubBitstream(bitstreamId, bitstream.getName(), mime, bitstream.getDescription(),
                bitstream.getUserFormatDescription(), bitstream.getSequenceID(), bitstream.getSize());
        return gson.toJson(s);
    }

    @Put
    public Representation put(Representation rep) {
        return (this.isBinary) ? putBinary(rep) : putXml(rep);
    }

    // At least currently DSpace does not support modifying the contents of
    // a bitstream.
    private Representation putBinary(Representation bitstreamRepresentation) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.DELETE);
        setAllowedMethods(allowed);
        return error(null, "Bitstream binary data can not be edited.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    private Representation putXml(Representation bitstreamRepresentation) {
        Context c = null;
        Bitstream bitstream = null;
        try {
            c = getAuthenticatedContext();
            bitstream = Bitstream.find(c, this.bitstreamId);
            if (bitstream == null) {
                return errorNotFound(c, "Could not find the bitstream.");
            }
        }
        catch (SQLException e) {
            return errorInternal(c, "SQLException");
        }

        DomRepresentation dom = new DomRepresentation(bitstreamRepresentation);
        Node attributesNode = dom.getNode("//dl[@id='attributes']");
        if (attributesNode == null) {
            return error(c, "Did not find dl tag with id 'attributes'.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        bitstream.setName(null);
        bitstream.setDescription(null);
        try {
            bitstream.setUserFormatDescription(null);
        }
        catch (SQLException e) {
            return errorInternal(c, "SQLException");
        }
        bitstream.setSource(null);
        bitstream.setSequenceID(0);

        NodeList nodes = attributesNode.getChildNodes();
        int nNodes = nodes.getLength();
        BitstreamFormat bf = null;
        String userFormatDescription = null;
        for (int i=0; i < nNodes; i += 2) {
            String dt = nodes.item(i).getTextContent();
            String dd = nodes.item(i+1).getTextContent();

            if (dt.equals("name")) {
                bitstream.setName(dd);
            } else if (dt.equals("mimetype")) {
                try {
                    bf = BitstreamFormat.findByMIMEType(c, dd);
                    if (bf == null) {
                        // TODO: Maybe we should create a new bitstream format
                        // TODO: instead of just returning an error?
                        return error(c, "Could not find the bitstream format",
                                     Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
                    }
                }
                catch (SQLException e) {
                    return errorInternal(c, "SQLException while trying to " +
                                         "the bitstream format.");
                }
            } else if (dt.equals("description")) {
                bitstream.setDescription(dd);
            } else if (dt.equals("userformatdescription")) {
                userFormatDescription = dd;
            } else if (dt.equals("source")) {
                bitstream.setSource(dd);
            } else if (dt.equals("sequenceid")) {
                try {
                    bitstream.setSequenceID(Integer.parseInt(dd));
                }
                catch (NumberFormatException e) {
                    return error(c, "Sequence ID must be an integer.",
                                 Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
                }
            } else if (dt.equals("sizebytes")) {
                // Do nothing!
            } else {
                return error(c, "Unexpected data in attributes: " + dt,
                             Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        String error = null;
        try {

            // Calling setUserFormatDescription method of Bitstream sets format
            // to "unknown", so we call it only when we don't know the format!
            // (If that was unclear, please see DSpace API documentation.)
            if (bf != null) {
                bitstream.setFormat(bf);
            } else if (userFormatDescription != null) {
                bitstream.setUserFormatDescription(userFormatDescription);
            }

            bitstream.update();
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Bitstream updated.");
    }

    @Post
    public Representation post(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.DELETE);
        if (!this.isBinary) {
            allowed.add(Method.PUT);
        }
        setAllowedMethods(allowed);
        return error(null, "Bitstream resource does not allow POST method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    // NOTE: Deletes the bitstream permanently, even if it belongs to several
    // NOTE: bundles!!!
    @Delete
    public Representation delete() {
        Context c = null;
        Bitstream bitstream = null;
        try {
            c = getAuthenticatedContext();
            bitstream = Bitstream.find(c, this.bitstreamId);
            if (bitstream == null) {
                return errorNotFound(c, "Could not find the bitstream.");
            }
        }
        catch (SQLException e) {
            return errorInternal(c, "SQLException");
        }

        Bundle[] bundles;
        try {
            bundles = bitstream.getBundles();
            for (Bundle bundle : bundles) {
                bundle.removeBitstream(bitstream);
                // FIXME: This code assumes that either:
                // FIXME: a) all calls to removeBitstream() success
                // FIXME: b) the first one raises exception
                // FIXME:
                // FIXME: Of course, there is no reason for (at least in theory)
                // FIXME: this assumption to be true, so we might remove the
                // FIXME: bitstream only from some of the bundles. :-(
            }
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Bitstream deleted.");
    }
    
}
