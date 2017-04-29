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
import java.util.HashSet;
import java.util.LinkedList;

import org.dspace.core.Context;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataField;
import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.EPerson;
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

public class MetadataFieldResource extends BaseResource {

    private static Logger log = Logger.getLogger(MetadataFieldResource.class);
    private int metadataFieldId;

    static public String relativeUrl(int metadataFieldId) {
        return "metadatafield/" + Integer.toString(metadataFieldId);
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
        Context c = null;
        MetadataField metadataField = null;
        DomRepresentation representation = null;
        Document d = null;
        try {
            c = new Context();
            metadataField = MetadataField.find(c, this.metadataFieldId);
            if (metadataField == null) {
                return errorNotFound(c, "Could not find the metadataField.");
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


        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("MetadataField " + s));

        Element body = d.createElement("body");
        html.appendChild(body);
	
        Element dl = d.createElement("dl");
        setId(dl, "attributes");
        body.appendChild(dl);

        addDtDd(d, dl, "schema", metadataSchema.getName());
        addDtDd(d, dl, "element", metadataField.getElement());
        addDtDd(d, dl, "qualifier", metadataField.getQualifier());
        addDtDd(d, dl, "scopenote", metadataField.getScopeNote());

	c.abort(); // Same as c.complete() because we didn't modify the db.

        return representation;
    }

    @Put
    public Representation edit(InputRepresentation rep) {
        Context c = null;
        MetadataField metadataField;
        try {
            c = getAuthenticatedContext();
            metadataField = MetadataField.find(c, this.metadataFieldId);
            if (metadataField == null) {
                return errorNotFound(c, "Could not find the metadata field.");
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
            return error(c, "The number of <dt> and <dd> elements do not match.",
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
                return error(c, "Unexpected data in attributes: " + dt,
                             Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        if (schema == null || element == null) {
            return error(c, "At least schema and element must be given.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        MetadataSchema metadataSchema;
        try {
            metadataSchema = MetadataSchema.find(c, schema);
        }
        catch (SQLException e) {
            return errorInternal(c, "SQLException");
        }
        if (metadataSchema == null) {
            return error(c, "The schema " + schema + " does not exist.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        metadataField.setSchemaID(metadataSchema.getSchemaID());
        metadataField.setElement(element);
        metadataField.setQualifier(qualifier);
        metadataField.setScopeNote(scopeNote);
        
        try {
            metadataField.update(c);
            c.complete();
        }
        catch (NonUniqueMetadataException e) {
            return error(c, "Non unique metadata field.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
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
