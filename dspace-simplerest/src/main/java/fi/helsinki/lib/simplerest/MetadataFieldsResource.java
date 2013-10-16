/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.stubs.StubMetadata;
import java.util.HashSet;
import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataField;

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

public class MetadataFieldsResource extends BaseResource {

    private static Logger log = Logger.getLogger(MetadataFieldsResource.class);

    static public String relativeUrl(int dummy) {
        return "metadatafields";
    }
    
    @Get("xml")
    public Representation toXml() {
        Context c = null;
        MetadataField[] metadataFields;
        DomRepresentation representation;
        Document d;
        try {
            c = new Context();
            metadataFields = MetadataField.findAll(c);

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
        title.appendChild(d.createTextNode("Metadata fields"));

        Element body = d.createElement("body");
        html.appendChild(body);

        Element ul = d.createElement("ul");
        setId(ul, "metadatafields");
        body.appendChild(ul);

        String url = getRequest().getResourceRef().getIdentifier();
        url = url.substring(0, url.lastIndexOf('/') + 1);
        url += "metadatafield/";
        for (MetadataField metadataField : metadataFields) {

            Element li = d.createElement("li");
            Element a = d.createElement("a");

            MetadataSchema metadataSchema;

            try {
                metadataSchema =
                    MetadataSchema.find(c, metadataField.getSchemaID());
            }
            catch (SQLException e) {
                return errorInternal(c, "SQLException");
            }

            String s =
                metadataSchema.getName() + "." +
                metadataField.getElement() + "." +
                metadataField.getQualifier();
            

            a.setAttribute("href", url +
                           Integer.toString(metadataField.getFieldID()));
            a.appendChild(d.createTextNode(s));
            li.appendChild(a);
            ul.appendChild(li);
        }

        Element form = d.createElement("form");
        form.setAttribute("method", "post");
        makeInputRow(d, form, "schema", "Schema");
        makeInputRow(d, form, "element", "Element");
        makeInputRow(d, form, "qualifier", "Qualifier");
        makeInputRow(d, form, "scopenote", "Scope note");

        Element submitButton = d.createElement("input");
        submitButton.setAttribute("type", "submit");
        submitButton.setAttribute("value", "Create a new metadata field.");
	
        form.appendChild(submitButton);
        body.appendChild(form);

        c.abort();
        return representation;
    }
    
    @Get("json")
    public String toJson(){
        MetadataField[] fields;
        Context c = null;
        try{
            c = new Context();
            fields = MetadataField.findAll(c);
        }catch(Exception e){
            return errorInternal(c, e.toString()).getText();
        }finally{
            c.abort();
        }
        
        Gson gson = new Gson();
        
        StubMetadata[] toJsonFields = new StubMetadata[fields.length];
        MetadataSchema schema = null;
        for(int i = 0; i < fields.length; i++){
            try{
                schema = MetadataSchema.find(c, fields[i].getSchemaID());
            }catch(Exception e){
                return errorInternal(c, e.toString()).getText();
            }
            toJsonFields[i] = new StubMetadata(fields[i].getFieldID(), schema.getName(), fields[i].getElement(), fields[i].getQualifier(),
                    fields[i].getScopeNote());
        }
        return gson.toJson(toJsonFields);
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
        String schema = form.getFirstValue("schema");
        String element = form.getFirstValue("element");
        String qualifier = form.getFirstValue("qualifier");
        String scopeNote = form.getFirstValue("scopenote");
        
        if (schema == null) {
            return error(c, "There was no schema.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
        if (element == null) {
            return error(c, "There was no element.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
        if (qualifier == null) { qualifier = ""; }
        if (scopeNote == null) { scopeNote = ""; }

        // TODO: Some additional input checks?

        MetadataSchema metadataSchema = null;
        
        try {
            metadataSchema =
                MetadataSchema.find(c, schema);
        }
        catch (SQLException e) {
            return errorInternal(c, "SQLException");
        }
        if (metadataSchema == null) {
            return error(c, "The schema " + schema + " does not exist.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }


        MetadataField metadataField;
        try {
            metadataField = new MetadataField(metadataSchema,
                                              element, qualifier, scopeNote);
            metadataField.create(c);
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        String url = baseUrl() +
            MetadataFieldResource.relativeUrl(metadataField.getFieldID());
        return successCreated("Created a new metadata field.", url);
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
        return error(null, "Metadata fields resource does not allow " +
                     unallowedMethod + " method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }
}
