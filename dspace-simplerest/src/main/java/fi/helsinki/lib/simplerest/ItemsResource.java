/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.stubs.StubItem;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import org.dspace.content.WorkspaceItem;
import org.dspace.content.ItemIterator;

import org.dspace.content.*;

import org.restlet.ext.xml.DomRepresentation;
import org.restlet.ext.fileupload.RestletFileUpload;
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

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Priority;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.utils.DSpace;

public class ItemsResource extends BaseResource {

    private static Logger log = Logger.getLogger(ItemResource.class);
    
    private Item[] items;
    private Context context;
    private int collectionId;

    static public String relativeUrl(int collectionId) {
        return "collection/" + collectionId + "/items";
    }
    
    public ItemsResource(Item[] i, int colelctionId){
        this.items = i;
        this.collectionId = colelctionId;
    }
    
    public ItemsResource(){
        this.collectionId = 0;
        this.items = null;
        try{
            this.context = new Context();
        }catch(SQLException e){
            log.log(Priority.FATAL, e);
        }
    }
    
    @Override
    protected final void doInit() throws ResourceException {
        try {
            String s = (String)getRequest().getAttributes().get("collectionId");
            this.collectionId = Integer.parseInt(s);
            
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "Collection ID must be a number.");
            throw resourceException;
        }
    }

    @Get("html|xhtml|xml")
    public Representation toXml() {
        Collection collection = null;
        DomRepresentation representation = null;
        Document d = null;
        try {
            collection = Collection.find(context, this.collectionId);
            if (collection == null) {
                return errorNotFound(context, "Could not find the collection.");
            }

            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
        }
        catch (SQLException e) {
            return errorInternal(context, e.toString());
        }catch(IOException e){
            return errorInternal(context, e.toString());
        }
        
        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("Items for collection " +
                                           collection.getName()));

        Element body = d.createElement("body");
        html.appendChild(body);
	
        Element ulItems = d.createElement("ul");
        setId(ulItems, "items");
        body.appendChild(ulItems);
        
        String base = baseUrl();
        try {
            ItemIterator ii = collection.getItems();
            while (ii.hasNext()) {
                Item item = ii.next();
                Element li = d.createElement("li");
                Element a = d.createElement("a");
                String name = item.getName();
                if (name == null) {
                    // FIXME: Should we really give names for items with no
                    // FIXME: name? (And if so does "Untitled" make sense?)
                    // FIXME: Anyway, this would break with null values.
                    name = "Untitled";
                }
                a.appendChild(d.createTextNode(name));
                String href = base + ItemResource.relativeUrl(item.getID());
                setAttribute(a, "href", href);
                li.appendChild(a);
                ulItems.appendChild(li);
            }
        }
        catch (SQLException e) {
            String errMsg =
                "SQLException while trying to items of the collection. "+e.getMessage();
            return errorInternal(context, errMsg);
        }

        Element form = d.createElement("form");
        form.setAttribute("enctype", "multipart/form-data");
        form.setAttribute("method", "post");
        makeInputRow(d, form, "title", "Title");
        makeInputRow(d, form, "lang", "Language");

        Element submitButton = d.createElement("input");
        submitButton.setAttribute("type", "submit");
        submitButton.setAttribute("value", "Create a new item");
        form.appendChild(submitButton);
        
        body.appendChild(form);

        try{
        context.abort(); /* We did not make any changes to the database, so we could
                      call context.complete() instead (only it can potentially raise
                      SQLexception). */
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }

        return representation;
    }
    
    @Get("json")
    public String toJson() throws SQLException{
        ItemIterator items;
        Collection collection = null;
        try{
            context = new Context();
        }catch(Exception ex){
            log.log(Priority.FATAL, ex);
        }
        try{
            collection = Collection.find(context, collectionId);
        }catch(Exception e){
            return errorInternal(context, e.toString()).getText();
        }
        items = collection.getItems();
        Gson gson = new Gson();
        
        ArrayList<StubItem> al = new ArrayList<StubItem>(10);
        
        while(items.hasNext()){
            al.add(new StubItem(items.next()));
        }
                
        try{
            items.close();
            context.abort();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        
        return gson.toJson(al);   
    }

    @Put
    public Representation put(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.POST);
        setAllowedMethods(allowed);
        return error(null,
                     "Items resource does not allow PUT method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    @Post
    public Representation addItem(InputRepresentation rep) throws AuthorizeException, SQLException, IdentifierException {
	Collection collection = null;
        Context addItemContext = null;
	try {
	    addItemContext = getAuthenticatedContext();
	    collection = Collection.find(addItemContext, this.collectionId);
	    if (collection == null) {
		return errorNotFound(addItemContext, "Could not find the collection.");
	    }
	}
	catch (SQLException e) {
	    return errorInternal(addItemContext, "SQLException");
	}
        catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
	String title = null;
	String lang = null;

	try {
	    RestletFileUpload rfu =
		new RestletFileUpload(new DiskFileItemFactory());
	    FileItemIterator iter = rfu.getItemIterator(rep);

	    while (iter.hasNext()) {
		FileItemStream fileItemStream = iter.next();
		if (fileItemStream.isFormField()) {
		    String key = fileItemStream.getFieldName();
		    String value =
			IOUtils.toString(fileItemStream.openStream(), "UTF-8");

		    if (key.equals("title")) {
			title = value;
		    }
		    else if (key.equals("lang")) {
			lang = value;
		    }
		    else if (key.equals("in_archive")) {
			;
		    }
		    else if (key.equals("withdrawn")) {
			;
		    }
		    else {
			return error(addItemContext, "Unexpected attribute: " + key,
				     Status.CLIENT_ERROR_BAD_REQUEST);
		    }
		}
	    }
	}
	catch (FileUploadException e) {
	    return errorInternal(addItemContext, e.toString());
	}catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }catch(IOException e){
            return errorInternal(context, e.toString());
        }

	if (title == null) {
	    return error(addItemContext, "There was no title given.",
			 Status.CLIENT_ERROR_BAD_REQUEST);
	}

	Item item = null;
	try {
	    WorkspaceItem wsi = WorkspaceItem.create(addItemContext, collection, false);
            item = InstallItem.installItem(addItemContext, wsi);
	    item.addMetadata("dc", "title", null, lang, title);
            item.update();
	    addItemContext.complete();
	}
	catch (AuthorizeException e) {
            log.log(Priority.FATAL, e, e);
	    return errorInternal(addItemContext, e.toString());
	}catch(SQLException e){
            log.log(Priority.FATAL, e, e);
	    return errorInternal(addItemContext, e.toString());
        }catch(IOException e){
            log.log(Priority.FATAL, e, e);
	    return errorInternal(addItemContext, e.toString());
        }
        
        try{
            addItemContext.abort();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e, e);
        }

	return successCreated("Created a new item.",
			      baseUrl() +
			      ItemResource.relativeUrl(item.getID()));
    }

    @Delete
    public Representation delete(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.POST);
        setAllowedMethods(allowed);
        return error(null,
                     "Items resource does not allow DELETE method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

}
