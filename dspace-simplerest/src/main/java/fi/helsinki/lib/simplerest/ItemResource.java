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

import fi.helsinki.lib.simplerest.stubs.StubItem;
import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.options.GetOptions;
import java.sql.SQLException;
import java.util.LinkedList;

import org.dspace.core.Context;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.DCValue;

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
import org.restlet.data.Form;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public class ItemResource extends BaseResource {

    private static Logger log = Logger.getLogger(ItemResource.class);
    
    private int itemId;
    private Item item;
    private Context context;
    
    public ItemResource(Item i, int itemId){
        this.item = i;
        this.itemId = itemId;
    }
    
    public ItemResource(){
        this.itemId = 0;
        this.item = null;
        try{
            this.context = new Context();
        }catch(SQLException e){
            log.log(Priority.FATAL, e);
        }
    }

    static public String relativeUrl(int itemId) {
        return "item/" + itemId;
    }

    
    @Override
    protected void doInit() throws ResourceException {
        try {
            String s = (String)getRequest().getAttributes().get("itemId");
            this.itemId = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "Item ID must be a number.");
            throw resourceException;
        }
    }

    @Get("html|xhtml|xml")
    public Representation toXml() {
        Bundle[] bundles = null;
	int owningCollectionID = 0;
	Collection[] collections = null;
        DomRepresentation representation = null;
        Document d = null;
        try {
            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();
            item = Item.find(context, this.itemId);
            if (item == null) {
                return errorNotFound(context,
                                     "Could not find the item with given ID.");
            }
        }
        catch (Exception e) {
            log.log(Priority.FATAL, e);
        }
        try {
            bundles = item.getBundles();
            collections = item.getCollections();
            owningCollectionID = item.getOwningCollection().getID();
        } catch (Exception ex) {
            log.log(Priority.FATAL, ex);
        }


        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("Item " + item.getName()));

        Element body = d.createElement("body");
        html.appendChild(body);
        
        // attributes
        Element dlAttributes = d.createElement("dl");
        setId(dlAttributes, "attributes");
        body.appendChild(dlAttributes);
        addDtDd(d, dlAttributes, "in_archive", item.isArchived() ? "1" : "0");
        addDtDd(d, dlAttributes, "withdrawn", item.isWithdrawn() ? "1" : "0");

        // metadata
        DCValue[] metadata = item.getMetadata(Item.ANY, Item.ANY, Item.ANY,
                                              Item.ANY);
        Element dlMetadata = d.createElement("dl");
        setId(dlMetadata, "metadata");
        body.appendChild(dlMetadata);
        for (DCValue metadataValue : metadata) {
            Element dt = d.createElement("dt");
            String fieldName;
            fieldName = (metadataValue.schema + "." + metadataValue.element +
                         ((metadataValue.qualifier != null) ?
                          ("." + metadataValue.qualifier) : ""));
            dt.appendChild(d.createTextNode(fieldName));
            dlMetadata.appendChild(dt);

            Element dd = d.createElement("dd");
            String lang = metadataValue.language;
            if (lang != null &&	lang.length() > 0) {
                dd.setAttribute("lang", lang);
            }
            dd.appendChild(d.createTextNode(metadataValue.value));
            dlMetadata.appendChild(dd);

        }

	//Collections the item belongs to
        String base = "";
        try{
            base = baseUrl();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        Element ulCollections = d.createElement("ul");
        setId(ulCollections, "collections");
        body.appendChild(ulCollections);
        if(collections != null){
            for (Collection collection : collections) {
                Element li = d.createElement("li");
                Element a = d.createElement("a");
                String href = base + CollectionResource.relativeUrl(collection.getID());
                setAttribute(a, "href", href);
                if (collection.getID() == owningCollectionID)
                    setAttribute(a, "id", "owning");
                a.appendChild(d.createTextNode(collection.getName()));
                li.appendChild(a);
                ulCollections.appendChild(li);
            }
        }

        // bundles
        Element ulBundles = d.createElement("ul");
        setId(ulBundles, "bundles");
        body.appendChild(ulBundles);
        if(bundles != null){
            for (Bundle bundle : bundles) {
                Element li = d.createElement("li");
                Element a = d.createElement("a");
                String href = base + BundleResource.relativeUrl(bundle.getID());
                setAttribute(a, "href", href);
                a.appendChild(d.createTextNode(bundle.getName()));
                li.appendChild(a);
                ulBundles.appendChild(li);
            }
        }

        // form
        Element form = d.createElement("form");
        form.setAttribute("method", "post");

        Element label = d.createElement("label");
        label.setAttribute("for", "name");
        label.appendChild(d.createTextNode("Bundle name"));
        form.appendChild(label);

        Element select = d.createElement("select");
        select.setAttribute("id", "name");
        select.setAttribute("name", "name");
        
        Element option1 = d.createElement("option");
        option1.appendChild(d.createTextNode("ORIGINAL"));
        select.appendChild(option1);
        Element option2 = d.createElement("option");
        option2.appendChild(d.createTextNode("TEXT"));
        select.appendChild(option2);
        Element option3 = d.createElement("option");
        option3.appendChild(d.createTextNode("THUMBNAIL"));
        select.appendChild(option3);

        form.appendChild(select);

        Element submitButton = d.createElement("input");
        submitButton.setAttribute("type", "submit");
        submitButton.setAttribute("value", "Create a new bundle");
        form.appendChild(submitButton);
        
        body.appendChild(form);
        
        try{
            context.abort(); // Same as c.complete() because we didn't modify the db.
        }catch(Exception e){
            log.log(Priority.INFO, e);
        }

        return representation;
    }
    
    @Get("json")
    public String toJson(){
        GetOptions.allowAccess(getResponse());
        try {
            this.item = Item.find(context, this.itemId);
        } catch (Exception ex) {
            if(context != null)
                context.abort();
            log.log(Priority.INFO, ex);
        }
        
        StubItem stub = null;
        try {
            stub = new StubItem(this.item);
        } catch (SQLException ex) {
            log.log(Priority.INFO, ex);
        }
        try{
            context.abort();
        }catch(NullPointerException e){
            log.log(Priority.FATAL, e);
        }
        Gson gson = new Gson();
        return gson.toJson(stub);
    }

    /* Input to this a little bit more complicated than to other PUT methods:
       Usually we just read data inside dl tag with id 'attributes'. Here we
       addiotionally read data inside dl tag with id 'metadata'.
     */
    @Put
    public Representation editItem(InputRepresentation rep) {
        try {
            context = getAuthenticatedContext();
            item = Item.find(context, this.itemId);
        }
        catch (Exception e) {
            log.log(Priority.INFO, e);
        }
        
       if (item == null) {
           return error(context, "Could not find the item.",
                   Status.CLIENT_ERROR_NOT_FOUND);
       }
	
        DomRepresentation dom = new DomRepresentation(rep);
        
	// -------- collections ----------------------------------------------
	/*
	// Juho: Started writing this, but didn't finnish...
        Node collectionsNode = dom.getNode("//ul[@id='collections']");
        if (collectionsNode == null) {
            return error(c, "Did not find ul tag with an id 'collections'.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
	Collection[] collections = item.getCollections();
	for (Node node : collectionsNode.getChildNodes()) {
	    Node a = node.getFirstChild();
	    NamedNodeMap attributes = a.getAttributes();
	    String id = attributes.getNamedItem("id").getNodeValue();
	    String href = attributes.getNamedItem("href").getNodeValue();
	    String[] hrefElements = href.split("/");
	    int collectionID = int(hrefElements[hrefElements.length-1]);
	    if ((id != null) && ("owning".equals(id)){
		item.setOwningCollection(collectionId)
	    }
	    a.
	}
	*/

        // -------- attributes -----------------------------------------------

        Node attributesNode = dom.getNode("//dl[@id='attributes']");
        if (attributesNode == null) {
            return error(context, "Did not find dl tag with an id 'attributes'.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        /* According to the RESTful philosophy, we should transfer a complete
           reprentation, not just a 'diff' to the current state... so we
           require that in_archive and withdrawn status are given in the
           request. (The other possibility would haven been to decide some
           default values for them...)
        */
        int inArchiveFound = 0;
        int withdrawnFound = 0;

        NodeList attributeNodes = attributesNode.getChildNodes();
        LinkedList<String> dtList = new LinkedList();
        LinkedList<String> ddList = new LinkedList();
        int nAttributeNodes = attributeNodes.getLength();
        for (int i=0; i < nAttributeNodes; i++) {
            Node node = attributeNodes.item(i);
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

            if (dt.equals("in_archive")) {
                inArchiveFound = 1;
                if (dd.equals("1")) {
                    item.setArchived(true);
                }
                else if (dd.equals("0")) {
                    item.setArchived(false);
                }
                else {
                    return error(context, "in_archive should be 1 or 0",
                                 Status.CLIENT_ERROR_BAD_REQUEST);
                }
            }
            else if (dt.equals("withdrawn")) {
                withdrawnFound = 1;
                try {
                    if (dd.equals("1")) {
                        item.withdraw();
                    }
                    else if (dd.equals("0")) {
                        item.reinstate();
                    }
                    else {
                        return error(context, "withdrawn should be 1 or 0",
                                     Status.CLIENT_ERROR_BAD_REQUEST);
                    }
                }
                catch (Exception e) {
                    return errorInternal(context, e.toString());
                }
            }
            else {
                return error(context, "Unexpected data in attributes: " + dt,
                             Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        // If the was data missing, report it:
        String[] problems = {"'in_archive' and 'withdrawn'", "'in_archive'",
                             "'withdrawn'", ""};
        String problem = problems[withdrawnFound + 2*inArchiveFound];
        if (!problem.equals("")) {
            return error(context, problem + " was not found from the request.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        // -------- metadata -------------------------------------------------

        Node metadataNode = dom.getNode("//dl[@id='metadata']");
        if (metadataNode == null) {
            return error(context, "Did not find dl tag with an id 'metadata'.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
        NodeList nodes = metadataNode.getChildNodes();
        int nNodes = nodes.getLength();
	
        // First delete all the metadata, then add metadata as defined in the
        // PUT request:
        item.clearMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        LinkedList<Node> dtNodeList = new LinkedList();
        LinkedList<Node> ddNodeList = new LinkedList();
        for (int i=0; i < nNodes; i++) {
            Node node = nodes.item(i);
            String nodeName = node.getNodeName();
            if (nodeName.equals("dt")) {
                dtNodeList.add(node);
            }
            else if (nodeName.equals("dd")) {
                ddNodeList.add(node);
            }
        }
        if (dtNodeList.size() != ddNodeList.size()) {
            return error(context, "The number of <dt> and <dd> elements do not match.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
        size = dtNodeList.size();
        for (int i=0; i < size; i++) {
            Node dtNode = dtNodeList.get(i);
            Node ddNode = ddNodeList.get(i);
            String dt = dtNode.getTextContent();
            String dd = ddNode.getTextContent();
            String[] parts = dt.split("\\.");
            int lenParts = parts.length;
            String schema    = (lenParts > 0) ? parts[0] : null;
            String element   = (lenParts > 1) ? parts[1] : null;
            String qualifier = (lenParts > 2) ? parts[2] : null;

            String lang = null;
            NamedNodeMap namedNodeMap = ddNode.getAttributes();
            if (namedNodeMap != null) {
                Node node = namedNodeMap.getNamedItem("lang");
                if (node != null) {
                    lang = node.getTextContent();
                }
            }

            item.addMetadata(schema, element, qualifier, lang, dd);
        }

        try {
            item.update();
            context.complete();
        }
        catch (Exception e) {
            log.log(Priority.FATAL, e);
            if(context != null){
                return errorInternal(context, e.toString());
            }
        }
	
        return successOk("Item updated.");
    }

    @Post
    public Representation addBundle(Representation rep) {
        Bundle bundle = null;
        try {
            context = getAuthenticatedContext();
            item = Item.find(context, this.itemId);
            if (item == null) {
                return error(context, "Could not find the item.",
                             Status.CLIENT_ERROR_NOT_FOUND);
            }

            Form form = new Form(rep);
            String name = form.getFirstValue("name");
        
            if (name == null) {
                return error(context, "There was no name given.",
                             Status.CLIENT_ERROR_BAD_REQUEST);
            }

            bundle = item.createBundle(name);
            context.complete();
        }
        catch (Exception e) {
            log.log(Priority.INFO, e);
            if(context != null){
                return errorInternal(context, e.toString());
            }
        }

        return successCreated("Created a new bundle.",
                              baseUrl() +
                              BundleResource.relativeUrl(bundle.getID()));
    }

    // NOTE: This removes the item from all the collections it's belong to, so
    // NOTE: it's a real delete!!!
    @Delete
    public Representation deleteItem() {
        try {
            context = getAuthenticatedContext();
            item = Item.find(context, this.itemId);
            if (item == null) {
                return error(context, "Could not find the item.",
                             Status.CLIENT_ERROR_NOT_FOUND);
            }

            Collection[] collections = item.getCollections();

            // FIXME: We would like to that the item is removed from all the
            // FIXME: collections it belongs to... or if we get an exception,
            // FIXME: not from any collection.... but this code might remove
            // FIXME: the item only from some collections before raising an
            // FIXME: exception. :-(
            for (Collection collection : collections) {
                collection.removeItem(item);
            }
            context.complete();
        }
        catch (Exception e) {
            log.log(Priority.INFO, e);
            if(context != null){
                return errorInternal(context, e.toString());
            }
        }

        return successOk("Item deleted.");
    }
}
