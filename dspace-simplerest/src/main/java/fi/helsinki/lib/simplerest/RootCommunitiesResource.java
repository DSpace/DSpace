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
import fi.helsinki.lib.simplerest.options.GetOptions;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;

import org.dspace.core.Context;
import org.dspace.content.Community;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;

import org.restlet.ext.xml.DomRepresentation;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.Representation;
import org.restlet.representation.InputRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.Post;
import org.restlet.resource.Delete;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.data.Method;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Priority;

public class RootCommunitiesResource extends BaseResource {

    private static Logger log = Logger.getLogger(RootCommunitiesResource.class);
    
    private Community[] communities;
    private Context context;
    
    public RootCommunitiesResource(Community[] communities){
        this.communities = communities;
    }
    
    public RootCommunitiesResource(){
        this.communities = null;
        try{
            this.context = new Context();
        }catch(SQLException e){
            log.log(Priority.INFO, e, e);
        }
        try{
            this.communities = Community.findAllTop(context);
        }catch(Exception e){
            log.log(Priority.INFO, e, e);
        }
    }

    static public String relativeUrl(int dummy) {
        return "rootcommunities";
    }

    @Get("html|xhtml|xml")
    public Representation toXml() {
        DomRepresentation representation = null;
        Document d = null;
        try {
            representation = new DomRepresentation(MediaType.ALL);
            d = representation.getDocument();
        } catch (IOException ex) {
            log.log(Priority.INFO, ex, ex);
        }

        representation.setIndenting(true);

        Element html = d.createElement("html");
        d.appendChild(html);

        Element head = d.createElement("head");
        html.appendChild(head);

        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("Root communities"));

        Element body = d.createElement("body");
        html.appendChild(body);

        Element ul = d.createElement("ul");
        setId(ul, "rootcommunities");
        body.appendChild(ul);

        String url = "";
        try{
            url = getRequest().getResourceRef().getIdentifier();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e, e);
        }
        url = url.substring(0, url.lastIndexOf('/') + 1);
        url += "community/";
        for (Community community : communities) {

            Element li = d.createElement("li");
            Element a = d.createElement("a");

            a.setAttribute("href", url + Integer.toString(community.getID()));
            a.appendChild(d.createTextNode(community.getName()));
            li.appendChild(a);
            ul.appendChild(li);
        }

        Element form = d.createElement("form");
        form.setAttribute("enctype", "multipart/form-data");
        form.setAttribute("method", "post");
        makeInputRow(d, form, "name", "Name");
        makeInputRow(d, form, "short_description", "Short description");
        makeInputRow(d, form, "introductory_text", "Introductory text");
        makeInputRow(d, form, "copyright_text", "Copyright text");
        makeInputRow(d, form, "side_bar_text", "Side bar text");
        makeInputRow(d, form, "logo", "Logo", "file");

        Element submitButton = d.createElement("input");
        submitButton.setAttribute("type", "submit");
        submitButton.setAttribute("value", "Create a new root community.");

        form.appendChild(submitButton);
        body.appendChild(form);

        try{
            context.abort();
        }catch(NullPointerException e){
            log.log(Priority.INFO, e, e);
        }
        return representation;
    }
    
    @Get("json")
    public String toJson() {
        GetOptions.allowAccess(getResponse());
        /*Community class from DSpace-api won't work for Serialization to json,
        so we use StubCommunity, and use a slow loop to create new StubCommunity array,
        which will be Serializable and converted to json. */
        Gson gson = new Gson();
        StubCommunity[] toJsonCommunities = new StubCommunity[communities.length];
        for(int i = 0; i < communities.length; i++){
            toJsonCommunities[i] = new StubCommunity(communities[i].getID(), communities[i].getName(), communities[i].getMetadata("short_description"),
                    communities[i].getMetadata("introductory_text"), communities[i].getMetadata("copyright_text"), communities[i].getMetadata("side_bar_text"));
        }
        
        try{
            context.abort();
        }catch(Exception e){
            log.log(Priority.FATAL, e);
        }
                
        return gson.toJson(toJsonCommunities);
    }

    @Put
    public Representation put(Representation dummy) {
        return errorUnallowedMethod("PUT");
    }

    @Post
    public Representation addCommunity(InputRepresentation rep) {
        Context c = null;
        Community community;
        try {
            c = getAuthenticatedContext();
            community = Community.create(null, c);

            RestletFileUpload rfu =
                    new RestletFileUpload(new DiskFileItemFactory());
            FileItemIterator iter = rfu.getItemIterator(rep);

            String name = null;
            String shortDescription = null;
            String introductoryText = null;
            String copyrightText = null;
            String sideBarText = null;
            Bitstream bitstream = null;
            String bitstreamMimeType = null;
            while (iter.hasNext()) {
                FileItemStream fileItemStream = iter.next();
                if (fileItemStream.isFormField()) {
                    String key = fileItemStream.getFieldName();
                    String value =
                            IOUtils.toString(fileItemStream.openStream(), "UTF-8");

                    if (key.equals("name")) {
                        name = value;
                    } else if (key.equals("short_description")) {
                        shortDescription = value;
                    } else if (key.equals("introductory_text")) {
                        introductoryText = value;
                    } else if (key.equals("copyright_text")) {
                        copyrightText = value;
                    } else if (key.equals("side_bar_text")) {
                        sideBarText = value;
                    } else {
                        return error(c, "Unexpected attribute: " + key,
                                Status.CLIENT_ERROR_BAD_REQUEST);
                    }
                } else {
                    if (bitstream != null) {
                        return error(c, "The community can have only one logo.",
                                Status.CLIENT_ERROR_BAD_REQUEST);
                    }

                    // I did not manage to use FormatIdentifier.guessFormat
                    // here, so let's do it by ourselves... I would prefer to
                    // use the actual file content and not its name, but let's
                    // keep the code simple...
                    String fileName = fileItemStream.getName();
                    if (fileName.length() == 0) {
                        continue;
                    }
                    int lastDot = fileName.lastIndexOf('.');
                    if (lastDot != -1) {
                        String extension = fileName.substring(lastDot + 1);
                        extension = extension.toLowerCase();
                        if (extension.equals("jpg")
                                || extension.equals("jpeg")) {
                            bitstreamMimeType = "image/jpeg";
                        } else if (extension.equals("png")) {
                            bitstreamMimeType = "image/png";
                        } else if (extension.equals("gif")) {
                            bitstreamMimeType = "image/gif";
                        }
                    }
                    if (bitstreamMimeType == null) {
                        String err =
                                "The logo filename extension was not recognised.";
                        return error(c, err, Status.CLIENT_ERROR_BAD_REQUEST);
                    }

                    bitstream = community.setLogo(fileItemStream.openStream());
                    // We don't set the format of the logo (bitstream) here,
                    // it's done in the code below...
                }
            }

            community.setMetadata("name", name);
            community.setMetadata("short_description", shortDescription);
            community.setMetadata("introductory_text", introductoryText);
            community.setMetadata("copyright_text", copyrightText);
            community.setMetadata("side_bar_text", sideBarText);

            community.update();

            // Set the format (jpeg, png, or gif) of logo:
            Bitstream logo = community.getLogo();
            if (logo != null) {
                BitstreamFormat bf =
                        BitstreamFormat.findByMIMEType(c, bitstreamMimeType);
                logo.setFormat(bf);
                logo.update();
            }

            c.complete();
        } catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successCreated("Community created.",
                baseUrl()
                + CommunityResource.relativeUrl(community.getID()));
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
        return error(null, "Root communities resource does not allow "
                + unallowedMethod + " method.",
                Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
    }
}
