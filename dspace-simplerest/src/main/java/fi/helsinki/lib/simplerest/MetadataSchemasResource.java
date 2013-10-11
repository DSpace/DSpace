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
import java.util.HashSet;
import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.content.MetadataSchema;

import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.InputRepresentation;
import org.restlet.resource.Get; 
import org.restlet.resource.Put;
import org.restlet.resource.Post;
import org.restlet.resource.Delete;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.data.Method;
import org.restlet.data.Form;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.log4j.Logger;

public class MetadataSchemasResource extends BaseResource {

    private static Logger log = Logger.getLogger(MetadataSchemasResource.class);

    static public String relativeUrl(int dummy) {
        return "metadataschemas";
    }
    
    @Get("xml")
    public Representation toXml() {
        Context c = null;
        MetadataSchema[] metadataSchemas;
        DomRepresentation representation;
        Document d;
        try {
            c = new Context();
            metadataSchemas = MetadataSchema.findAll(c);

            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }
        
        representation.setIndenting(true);

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("Metadata schemas"));

        Element body = d.createElement("body");
        html.appendChild(body);

        Element ul = d.createElement("ul");
        setId(ul, "metadataschemas");
        body.appendChild(ul);

        String url = getRequest().getResourceRef().getIdentifier();
        url = url.substring(0, url.lastIndexOf('/') + 1);
        url += "metadataschema/";
        for (MetadataSchema metadataSchema : metadataSchemas) {

            Element li = d.createElement("li");
            Element a = d.createElement("a");

            a.setAttribute("href", url +
                           Integer.toString(metadataSchema.getSchemaID()));
            a.appendChild(d.createTextNode(metadataSchema.getName()));
            li.appendChild(a);
            ul.appendChild(li);
        }

        Element form = d.createElement("form");
        form.setAttribute("method", "post");
        makeInputRow(d, form, "name", "Name");
        makeInputRow(d, form, "namespace", "Namespace");

        Element submitButton = d.createElement("input");
        submitButton.setAttribute("type", "submit");
        submitButton.setAttribute("value", "Create a new metadata schema.");
	
        form.appendChild(submitButton);
        body.appendChild(form);

        c.abort();
        return representation;
    }
    
    @Get("json")
    public String toJson() throws SQLException{
        MetadataSchema[] schemas;
        Context c = null;
        try{
            c = new Context();
            schemas = MetadataSchema.findAll(c);
        }catch(Exception e){
            return errorInternal(c, e.toString()).getText();
        }finally{
            c.abort();
        }
        
        Gson gson = new Gson();
        
        StubSchema[] toJsonSchemas = new StubSchema[schemas.length];
        for(int i = 0; i < schemas.length; i++){
            toJsonSchemas[i] = new StubSchema(schemas[i].getSchemaID(), schemas[i].getName(), schemas[i].getNamespace());
        }
        
        return gson.toJson(toJsonSchemas);
    }

    @Put
    public Representation put(Representation dummy) {
        return errorUnallowedMethod("PUT");
    }

    @Post
    public Representation addCommunity(InputRepresentation rep) {
        Context c = null;
        try {
            c = getAuthenticatedContext();
        }
        catch (SQLException e) {
            return errorInternal(c, "SQLException");
        }

        Form form = new Form(rep);
        String name = form.getFirstValue("name");
        String namespace = form.getFirstValue("namespace");
        
        if (name == null) {
            return error(c, "There was no name given.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
        if (namespace == null) {
            return error(c, "There was no namespace given.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
        // TODO: Some additional input checks?

        MetadataSchema metadataSchema = new MetadataSchema(namespace, name);

        try {
            metadataSchema.create(c);
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        String url = baseUrl() +
            MetadataSchemaResource.relativeUrl(metadataSchema.getSchemaID());
        return successCreated("Created a new metadata schema.", url);
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
        return error(null, "Metadata schemas resource does not allow " +
                     unallowedMethod + " method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }
}
