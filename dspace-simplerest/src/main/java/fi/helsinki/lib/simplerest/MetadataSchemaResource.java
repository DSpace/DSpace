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
import fi.helsinki.lib.simplerest.stubs.StubSchema;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;

import org.dspace.core.Context;
import org.dspace.content.MetadataSchema;
import org.dspace.content.NonUniqueMetadataException;

import org.restlet.ext.xml.DomRepresentation;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public class MetadataSchemaResource extends BaseResource {

    private static Logger log = Logger.getLogger(MetadataSchemaResource.class);
    private int metadataSchemaId;
    private MetadataSchema mschema;
    private Context context;
    
    public MetadataSchemaResource(MetadataSchema mschema, int id){
        this.mschema = mschema;
        this.metadataSchemaId = id;
    }
    
    public MetadataSchemaResource(){
        this.mschema = null;
        this.metadataSchemaId = 0;
        try{
            this.context = new Context();
        }catch(SQLException e){
            log.log(Priority.INFO, e);
        }
    } 

    static public String relativeUrl(int metadataSchemaId) {
        return "metadataschema/" + metadataSchemaId;
    }
    
    @Override
    protected void doInit() throws ResourceException {
        try {
            String id =
                (String)getRequest().getAttributes().get("metadataSchemaId");
            this.metadataSchemaId = Integer.parseInt(id);
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "MetadataSchema ID must be a number.");
            throw resourceException;
        }
    }

    @Get("xml")
    public Representation toXml() {
        DomRepresentation representation = null;
        Document d = null;
        
        //For testing purposes we check if mschema is null, try to find it through DSpace api,
        //and if even after that it isn't found we return a error.
        if(mschema == null){
            try {
                // IMHO there is a bug in DSpace, because after calling
                // MetadataSchema.delete() the method MetadataSchema.find() will
                // still return the deleted metadata schema. (Because delete()
                // does not call decache() method...)
                // So as a workaround, we call findAll() instead of find().
                MetadataSchema[] metadataSchemas = MetadataSchema.findAll(context);
                for (MetadataSchema mds : metadataSchemas) {
                    if (mds.getSchemaID() == this.metadataSchemaId) {
                        mschema = mds;
                        break;
                    }
                }
            }
            catch (Exception e) {
                if (mschema == null) {
                    return errorNotFound(context, "Could not find the metadata schema.");
                }
                log.log(Priority.INFO, e);
            }
        }
        
        try{
            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
        }catch(Exception e){
            return errorInternal(context, e.toString());
        }

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("MetadataSchema " +
                                           mschema.getName()));

        Element body = d.createElement("body");
        html.appendChild(body);
	
        Element dl = d.createElement("dl");
        setId(dl, "attributes");
        body.appendChild(dl);

        addDtDd(d, dl, "name", mschema.getName());
        addDtDd(d, dl, "namespace", mschema.getNamespace());
        
        try{
            context.abort(); // Same as c.complete() because we didn't modify the db.
        }catch(NullPointerException e){
            log.log(Priority.FATAL, e);
        }

        return representation;
    }
    
    @Get("json")
    public String toJson() throws SQLException{
        Gson gson = new Gson();
        if(mschema == null){
            try{
                MetadataSchema[] metadataSchemas = MetadataSchema.findAll(context);
                for (MetadataSchema mds : metadataSchemas) {
                    if (mds.getSchemaID() == this.metadataSchemaId) {
                        mschema = mds;
                        break;
                    }
            }
        }catch(Exception e){
            if(mschema == null){
                return errorNotFound(context, "Could not find the schema").getText();
            }
            log.log(Priority.INFO, e);
        }
        }
        
        StubSchema ss = new StubSchema(mschema.getSchemaID(), mschema.getName(), mschema.getNamespace());
        
        try{
            context.abort();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        
        return gson.toJson(ss);
    }

    @Put
    public Representation edit(InputRepresentation rep) {
        Context c = null;
        MetadataSchema metadataSchema;
        try {
            c = getAuthenticatedContext();
            metadataSchema = MetadataSchema.find(c, this.metadataSchemaId);
            if (metadataSchema == null) {
                return errorNotFound(c, "Could not find the metadata schema.");
            }
        }
        catch (SQLException e) {
            return errorInternal(c, "SQLException");
        }

        DomRepresentation dom = new DomRepresentation(rep);

        Node attributesNode = dom.getNode("//dl[@id='attributes']");
        if (attributesNode == null) {
            return error(c, "Did not find dl tag with an id 'attributes'.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
	
        String name = null;
        String namespace = null;

        NodeList nodes = attributesNode.getChildNodes();
        LinkedList<String> dtList = new LinkedList();
        LinkedList<String> ddList = new LinkedList();
        int nNodes = nodes.getLength();
        for (int i=0; i < nNodes; i++) {
            Node node = nodes.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals("dt")) {
                dtList.add(node.getTextContent());
            }
            else if (nodeName.equals("dd")) {
                ddList.add(node.getTextContent());
            }
        }
        if (dtList.size() != ddList.size()) {
            return error(c, "The number of <dt> and <dd> elements do not match.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
        int size = dtList.size();
        for (int i=0; i < size; i++) {
            String dt = dtList.get(i);
            String dd = ddList.get(i);

            if      (dt.equals("name"))      { name = dd; }
            else if (dt.equals("namespace")) { namespace = dd; }
            else {
                return error(c, "Unexpected data in attributes: " + dt,
                             Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        if (name == null || namespace == null) {
            return error(c, "Both name and namespace must be given.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        metadataSchema.setName(name);
        metadataSchema.setNamespace(namespace);
        
        try {
            metadataSchema.update(c);
            c.complete();
        }
        catch (NonUniqueMetadataException e) {
            return error(c, "Name and namespace must be unique.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Metadata schema updated.");
    }



    @Post
    public Representation post(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.DELETE);
        allowed.add(Method.PUT);
        setAllowedMethods(allowed);
        return error(null,
                     "Metadata schema resource does not allow POST method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    @Delete
    public Representation delete() {
        Context c = null;
        MetadataSchema metadataSchema;
        try {
            c = getAuthenticatedContext();
            metadataSchema = MetadataSchema.find(c, this.metadataSchemaId);
            if (metadataSchema == null) {
                return errorNotFound(c, "Could not find the metadataSchema.");
            }

            metadataSchema.delete(c);
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Metadata schema deleted.");
    }
}
