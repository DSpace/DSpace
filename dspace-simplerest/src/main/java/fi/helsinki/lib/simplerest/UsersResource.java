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
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.data.MediaType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import org.apache.log4j.Logger;

public class UsersResource extends BaseResource {

    private static Logger log = Logger.getLogger(UsersResource.class);

    static public String relativeUrl(int dummy) {
        return "users";
    }

    @Get("xml")
    public Representation toXml() {
        Context c = null;
        DomRepresentation representation = null;
        Document d = null;
	EPerson epersons[] = null;
	
        try {
            c = new Context();
            representation = new DomRepresentation(MediaType.TEXT_HTML);  
            d = representation.getDocument();  
	    epersons = EPerson.findAll(c,0);
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
	title.appendChild(d.createTextNode("Users"));
        head.appendChild(title);

        Element body = d.createElement("body");
        html.appendChild(body);
	
	Element ul = d.createElement("ul");
	ul.setAttribute("id","users");
        body.appendChild(ul);

	for (EPerson eperson : epersons) {
	    Element li = d.createElement("li");
	    Element a = d.createElement("a");
	    ul.appendChild(li);
	    li.appendChild(a);
	    String url = baseUrl() +
		UserResource.relativeUrl(eperson.getID());
	    a.setAttribute("href",url);
	    Text text = d.createTextNode(eperson.getFullName());
	    a.appendChild(text);
	}

	c.abort(); // Same as c.complete() because we didn't modify the db.

        return representation;
    }
    
    @Get("json")
    public String toJson(){
        GetOptions.allowAccess(getResponse());
        EPerson[] users;
        Context c = null;
        try{
            c = new Context();
            users = EPerson.findAll(c, 0);
        }catch(Exception e){
            return errorInternal(c, e.toString()).getText();
        }finally{
            if(c != null)
                c.abort();
        }
        
        Gson gson = new Gson();
        StubUser[] toJsonUsers = new StubUser[users.length];
        
        for (int i = 0; i < users.length; i++) {
            toJsonUsers[i] = new StubUser(users[i].getID(), users[i].getEmail(), users[i].getLanguage(),
                    users[i].getNetid(), users[i].getFullName(), users[i].getFirstName(), users[i].getLastName(),
                    users[i].canLogIn(), users[i].getRequireCertificate(), users[i].getSelfRegistered());
        }
        
        return gson.toJson(toJsonUsers);
    }

}
