/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import fi.helsinki.lib.simplerest.stubs.StubCommunity;
import com.google.gson.Gson;
import org.dspace.core.Context;
import org.dspace.content.Community;
import org.dspace.content.Bitstream;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.logging.Level;

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

public class CommunityResource extends BaseResource {

    private static Logger log = Logger.getLogger(CommunityResource.class);
    private int communityId;
    private Community comm;
    private Context context;
    
    public CommunityResource(Community co, int communityId){
        this.communityId = communityId;
        this.comm = co;
    }
    
    public CommunityResource(){
        this.communityId = 0;
        this.comm = null;
        try {
            this.context = new Context();
        } catch (SQLException ex) {
            log.log(Priority.FATAL, ex);
        }
    }
    
    static public String relativeUrl(int communityId) {
        return "community/" + communityId;
    }
    
    @Override
    protected void doInit() throws ResourceException {
        try {
            String id = (String)getRequest().getAttributes().get("communityId");
            this.communityId = Integer.parseInt(id);
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "Community ID must be a number.");
            throw resourceException;
        }
    }

    // TODO: parent?
    @Get("html|xhtml|xml")
    public Representation toXml() {
        DomRepresentation representation;
        Document d;
        try{
            comm = Community.find(context, communityId);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(CommunityResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            representation = new DomRepresentation(MediaType.ALL);  
            d = representation.getDocument();
        }
        catch (Exception e) {
            return errorInternal(context, e.toString());
        }

        Element html = d.createElement("html");  
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("Community " + comm.getName()));

        Element body = d.createElement("body");
        html.appendChild(body);
	
        Element dl = d.createElement("dl");
        setId(dl, "attributes");
        body.appendChild(dl);

        Element dtName = d.createElement("dt");
        dtName.appendChild(d.createTextNode("name"));
        dl.appendChild(dtName);
        Element ddName = d.createElement("dd");
        ddName.appendChild(d.createTextNode(comm.getName()));
        dl.appendChild(ddName);

        String[] attributes = { "short_description", "introductory_text",
                                "copyright_text", "side_bar_text" };
        for (String attribute : attributes) {
            Element dt = d.createElement("dt");
            dt.appendChild(d.createTextNode(attribute));
            dl.appendChild(dt);

            Element dd = d.createElement("dd");
            dd.appendChild(d.createTextNode(comm.getMetadata(attribute)));
            dl.appendChild(dd);
        }

        Bitstream logo = comm.getLogo();
        if (logo != null) {
            Element aLogo = d.createElement("a");
            String url = baseUrl() +
                CommunityLogoResource.relativeUrl(this.communityId);
            //getRequest().getResourceRef().getIdentifier() + "/logo";
            setAttribute(aLogo, "href", url);
            setId(aLogo, "logo");
            aLogo.appendChild(d.createTextNode("Community logo"));
            body.appendChild(aLogo);
        }
        
        String url = null;
        
        try{
            url = getRequest().getResourceRef().getIdentifier();
        }catch(NullPointerException e){
            url = "";
        }

	// A link to sub communities
        Element pSubCommunities = d.createElement("p");
        Element aSubCommunities = d.createElement("a");
	setAttribute(aSubCommunities, "href", url + "/communities");
        setId(aSubCommunities, "communities");
	aSubCommunities.appendChild(d.createTextNode("communities"));
        pSubCommunities.appendChild(aSubCommunities);
        body.appendChild(pSubCommunities);

	// A link to child collections
        Element pSubCollections = d.createElement("p");
        Element aSubCollections = d.createElement("a");
	setAttribute(aSubCollections, "href", url + "/collections");
        setId(aSubCollections, "collections");
	aSubCollections.appendChild(d.createTextNode("collections"));
        pSubCollections.appendChild(aSubCollections);
        body.appendChild(pSubCollections);
        
        try{
            context.abort();
        }catch(NullPointerException e){
            Logger.getLogger(CommunitiesResource.class.getName()).log(url, Priority.WARN, e.toString(), e);
        }

        return representation;
    }
    
    @Get("json")
    public String toJson(){
        Gson gson = new Gson();
        try{
            comm = Community.find(context, this.communityId);
        }catch(Exception e){
            if(context != null)
                context.abort();
            log.log(Priority.INFO, e, e);
        }finally{
            if(context != null)
                context.abort();
        }
        StubCommunity s = new StubCommunity(comm.getID(), comm.getName(), comm.getMetadata("short_description"),
                    comm.getMetadata("introductory_text"), comm.getMetadata("copyright_text"), comm.getMetadata("side_bar_text"));
        return gson.toJson(s);
    }

    @Put
    public Representation edit(InputRepresentation rep) {
        try {
            context = getAuthenticatedContext();
            comm = Community.find(context, this.communityId);
            if (comm == null) {
                return errorNotFound(context, "Could not find the community.");
            }
        }
        catch (SQLException e) {
            return errorInternal(context, "SQLException "+e.getMessage());
        }
        catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }

        DomRepresentation dom = new DomRepresentation(rep);

        Node attributesNode = dom.getNode("//dl[@id='attributes']");
        if (attributesNode == null) {
            return error(context, "Did not find dl tag with an id 'attributes'.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }
	
        comm.setMetadata("name", null);
        comm.setMetadata("short_description", null);
        comm.setMetadata("introductory_text", null);
        comm.setMetadata("copyright_text", null);
        comm.setMetadata("side_bar_text", null);

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
            if (dt.equals("name") ||
                dt.equals("short_description") ||
                dt.equals("introductory_text") ||
                dt.equals("copyright_text") ||
                dt.equals("side_bar_text")) {
                comm.setMetadata(dt, dd);
            }
            else {
                return error(context, "Unexpected data in attributes: " + dt,
                             Status.CLIENT_ERROR_BAD_REQUEST);
	    }
	}

        try {
            comm.update();
            context.complete();
        }
        catch(NullPointerException e){
            log.log(Priority.INFO, e);
        }
        catch (Exception e) {
            return errorInternal(context, e.toString());
        }

        return successOk("Community updated.");
    }

    @Post
    public Representation post(Representation dummy) {
        HashSet<Method> allowed = new HashSet();
        allowed.add(Method.GET);
        allowed.add(Method.PUT);
        allowed.add(Method.DELETE);
        setAllowedMethods(allowed);
        return error(null,
                     "Community resource does not allow POST method.",
                     Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }

    @Delete
    public Representation delete() {
        Context c = null;
        Community community;
        try {
            c = getAuthenticatedContext();
            community = Community.find(c, this.communityId);
            if (community == null) {
                return errorNotFound(c, "Could not find the community.");
            }

            community.delete();
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Community deleted.");
    }
}
