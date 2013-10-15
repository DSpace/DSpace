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
import fi.helsinki.lib.simplerest.stubs.StubUser;
import java.sql.SQLException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public class UserResource extends BaseResource {

    private static Logger log = Logger.getLogger(UserResource.class);

    private int userId;
    private EPerson eperson;
    private Context context;
    
    public UserResource(EPerson p, int userId){
        this.eperson = p;
        this.userId = userId;
    }
    
    public UserResource(){
        this.userId = 0;
        this.eperson = null;
        try{
            this.context = new Context();
        }catch(SQLException e){
            log.log(Priority.FATAL, e);
        }
    }

    @Override
    protected void doInit() throws ResourceException {
        try {
            String s = (String)getRequest().getAttributes().get("userId");
            this.userId = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "User ID must be a number.");
            throw resourceException;
        }
    }

    static public String relativeUrl(int userId) {
        return "user/" + userId;
    }

    @Get("html|xhtml|xml")
    public Representation toXml() {
        DomRepresentation representation;
        Document d = null;
        
        try{
            this.eperson = EPerson.find(this.context, this.userId);
        }catch(Exception e){
            if(this.eperson == null){
                return errorNotFound(this.context, "Could not find the user.");
            }
            log.log(Priority.INFO, e);
        }
	
        try {
            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
        }
        catch (Exception e) {
            return errorInternal(this.context, e.toString());
        }

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        title.appendChild(d.createTextNode("User" + Integer.toString(userId)));
        head.appendChild(title);

        Element body = d.createElement("body");
        html.appendChild(body);
	
	Element dl = d.createElement("dl");
	dl.setAttribute("id","attributes");
        body.appendChild(dl);

	createAttribute(d,dl,"email",eperson.getEmail().replace("@", "(a)")); //Against those pesky bots
	createAttribute(d,dl,"id",Integer.toString(eperson.getID()));
	createAttribute(d,dl,"language",eperson.getLanguage());
	createAttribute(d,dl,"netid",eperson.getNetid());
	createAttribute(d,dl,"fullname",eperson.getFullName());
	createAttribute(d,dl,"firstname",eperson.getFirstName());
	createAttribute(d,dl,"lastname",eperson.getLastName());
	createAttribute(d,dl,"can login",Boolean.toString(eperson.canLogIn()));
	createAttribute(d,dl,"require certificate",Boolean.toString(eperson.getRequireCertificate()));
	createAttribute(d,dl,"self registered",Boolean.toString(eperson.getSelfRegistered()));	
        try{
        this.context.abort(); // Same as c.complete() because we didn't modify the db.
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        return representation;
    }
    
    @Get("json")
    public String toJson(){
        GetOptions.allowAccess(getResponse());
        Gson gson = new Gson();
        try{
            this.eperson = EPerson.find(context, userId);
        }catch(Exception e){
            log.log(Priority.INFO, e);
        }
        
        StubUser su = new StubUser(eperson.getID(), eperson.getEmail(), eperson.getLanguage(),
                eperson.getNetid(), eperson.getFullName(), eperson.getFirstName(), eperson.getLastName(),
                eperson.canLogIn(), eperson.getRequireCertificate(), eperson.getSelfRegistered());
        
        try{
            context.abort();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        
        return gson.toJson(su);
    }

    private void createAttribute(Document d, Element parent, String name,
				    String content) {
	if (content == null) return;
	Element dt = d.createElement("dt");
	Element dd = d.createElement("dd");
	dt.appendChild(d.createTextNode(name));
	dd.appendChild(d.createTextNode(content));
	parent.appendChild(dt);
	parent.appendChild(dd);
    }

}
