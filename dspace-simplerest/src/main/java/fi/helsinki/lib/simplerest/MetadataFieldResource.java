/**
 A RESTful web service on top of DSpace.
 The contents of this file are subject to the license and copyright
 detailed in the LICENSE and NOTICE files at the root of the source
 tree and available online at
 http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.options.GetOptions;
import fi.helsinki.lib.simplerest.stubs.StubMetadata;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;

import org.dspace.core.Context;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataField;
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

public class MetadataFieldResource extends BaseResource {

    private static Logger log = Logger.getLogger(MetadataFieldResource.class);
    private int metadataFieldId;
    private Context context;
    private MetadataField mfield;
    private MetadataSchema mschema;
    
    public MetadataFieldResource(MetadataSchema mschema, MetadataField mfield, int metadataFieldId){
        this.metadataFieldId = metadataFieldId;
        this.mfield = mfield;
        this.mschema = mschema;
    }
    
    public MetadataFieldResource(){
        this.metadataFieldId = 0;
        this.mfield = null;
        this.mschema = null;
        try{
            this.context = new Context();
        }catch(SQLException e){
            log.log(Priority.FATAL, e);
        }
    }

    static public String relativeUrl(int metadataFieldId) {
        return "metadatafield/" + metadataFieldId;
    }
    
    @Override
    protected void doInit() throws ResourceException {
        try {
            String id =
                (String)getRequest().getAttributes().get("metadataFieldId");
            this.metadataFieldId = Integer.parseInt(id);
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "MetadataField ID must be a number.");
            throw resourceException;
        }
    }

    @Get("xml")
    public Representation toXml() {
        DomRepresentation representation = null;
        Document d = null;
        if(mfield == null){
            try {
                mfield = MetadataField.find(context, this.metadataFieldId);
            }
            catch (Exception e) {
                if (mfield == null) {
                    return errorNotFound(context, "Could not find the metadataField.");
                }
                log.log(Priority.INFO, e);
            }
        }
        
        try{
            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();
        }catch(Exception e){
            errorInternal(context, e.toString());
        }

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        if(mschema == null){
            try {
                mschema =
                    MetadataSchema.find(context, mfield.getSchemaID());
            }
            catch (SQLException e) {
                log.log(Priority.INFO, e);
            }
        }
        
        String s =
            mschema.getName() + "." +
            mfield.getElement() + "." +
            mfield.getQualifier();


        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("MetadataField " + s));

        Element body = d.createElement("body");
        html.appendChild(body);
	
        Element dl = d.createElement("dl");
        setId(dl, "attributes");
        body.appendChild(dl);

        addDtDd(d, dl, "schema", mschema.getName());
        addDtDd(d, dl, "element", mfield.getElement());
        addDtDd(d, dl, "qualifier", mfield.getQualifier());
        addDtDd(d, dl, "scopenote", mfield.getScopeNote());
        
        try{
            context.abort(); // Same as c.complete() because we didn't modify the db.
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }

        return representation;
    }
    
    @Get("json")
    public String toJson() throws SQLException{
        GetOptions.allowAccess(getResponse());
        Gson gson = new Gson();
        //For testing purposes we check if the mfield or mschema variables are null
        if(mfield == null){
            try{
                mfield = MetadataField.find(context, metadataFieldId);
            }catch(Exception e){
                log.log(Priority.INFO, e);
            }
        }
        
        if(mschema == null){
            try{
                mschema = MetadataSchema.find(context, mfield.getSchemaID());
            }catch(SQLException e){
                log.log(Priority.INFO, e);
            }
        }
        
        StubMetadata sm = new StubMetadata(metadataFieldId, mschema.getName(), mfield.getElement(), mfield.getQualifier(), mfield.getScopeNote());
        
        try{
            context.abort();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        
        return gson.toJson(sm);
    }

    @Put
    public Representation edit(InputRepresentation rep) {
        try {
            context = getAuthenticatedContext();
            mfield = MetadataField.find(context, this.metadataFieldId);
            if (mfield == null) {
                return errorNotFound(context, "Could not find the metadata field.");
            }
        }
        catch (SQLException e) {
            log.log(Priority.INFO, e);
        }

        DomRepresentation dom = new DomRepresentation(rep);

        Node attributesNode = dom.getNode("//dl[@id='attributes']");
        if (attributesNode == null) {
            return error(context, "Did not find dl tag with an id 'attributes'.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
	
        // schema and element are mandatory, qualifier and scopeNote are
        // optional (because they can be empty).
        String schema = null;
        String element = null;
        String qualifier = ""; 
        String scopeNote = "";

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
            return error(context, "The number of <dt> and <dd> elements do not match.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        int size = dtList.size();
        for (int i=0; i < size; i++) {
            String dt = dtList.get(i);
            String dd = ddList.get(i);

            if      (dt.equals("schema"))    {    schema = dd; }
            else if (dt.equals("element"))   {   element = dd; }
            else if (dt.equals("qualifier")) { qualifier = dd; }
            else if (dt.equals("scopenote")) { scopeNote = dd; }
            else {
                return error(context, "Unexpected data in attributes: " + dt,
                             Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        if (schema == null || element == null) {
            return error(context, "At least schema and element must be given.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        try {
            mschema = MetadataSchema.find(context, schema);
        }
        catch (SQLException e) {
            log.log(Priority.INFO, e);
        }
        if (mschema == null) {
            return error(context, "The schema " + schema + " does not exist.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        mfield.setSchemaID(mschema.getSchemaID());
        mfield.setElement(element);
        mfield.setQualifier(qualifier);
        mfield.setScopeNote(scopeNote);
        
        try {
            mfield.update(context);
            context.complete();
        }
        catch (NonUniqueMetadataException e) {
            return error(context, "Non unique metadata field.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
        catch (NullPointerException e) {
            log.log(Priority.INFO, e);
        }
        catch(Exception e){
            return errorInternal(context, e.toString());
        }

        return successOk("Metadata field updated.");
    }

    @Post
    public Representation post(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.DELETE);
        allowed.add(Method.PUT);
        setAllowedMethods(allowed);
        return error(null,
                     "Metadata field resource does not allow POST method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    @Delete
    public Representation delete() {
        Context c = null;
        MetadataField metadataField;
        try {
            c = getAuthenticatedContext();
            metadataField = MetadataField.find(c, this.metadataFieldId);
            if (metadataField == null) {
                return errorNotFound(c, "Could not find the metadataField.");
            }

            metadataField.delete(c);
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Metadata field deleted.");
    }
}
