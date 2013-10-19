/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package fi.helsinki.lib.simplerest;

import com.google.gson.Gson;
import fi.helsinki.lib.simplerest.options.GetOptions;
import fi.helsinki.lib.simplerest.stubs.StubBundle;
import java.sql.SQLException;
import java.util.LinkedList;

import org.dspace.core.Context;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.FormatIdentifier;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public class BundleResource extends BaseResource {

    private static Logger log = Logger.getLogger(BundleResource.class);
    
    private int bundleId;

    static public String relativeUrl(int bundleId) {
        return "bundle/" + bundleId;
    }

    @Override
    protected void doInit() throws ResourceException {
        try {
            String s = (String)getRequest().getAttributes().get("bundleId");
            this.bundleId = Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            ResourceException resourceException =
                new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
                                      "Could not convert bundle id to an " +
                                      "integer.");
            throw resourceException;
        }
    }

    @Get("xml")
    public Representation get() {
        Context c = null;
        Bundle bundle = null;
        DomRepresentation representation = null;
        Document d = null;
        try {
            c = new Context();
            bundle = Bundle.find(c, this.bundleId);
            if (bundle == null) {
                return errorNotFound(c, "Could not find the bundle.");
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

        Element title = d.createElement("title");
        head.appendChild(title);
        title.appendChild(d.createTextNode("Bundle " + bundle.getName()));

        Element body = d.createElement("body");
        html.appendChild(body);
	

        Element dl = d.createElement("dl");
        setId(dl, "attributes");
        body.appendChild(dl);

        addDtDd(d, dl, "primarybitstreamid",
                Integer.toString(bundle.getPrimaryBitstreamID()));
        addDtDd(d, dl, "name", bundle.getName());

        Element ulBitstreams = d.createElement("ul");
        setId(ulBitstreams, "bitstreams");
        body.appendChild(ulBitstreams);

        String base = baseUrl();
        for (Bitstream bitstream : bundle.getBitstreams()) {
            Element li = d.createElement("li");
            Element a = d.createElement("a");
            String href = base +
                BitstreamResource.relativeUrl(bitstream.getID());
            setAttribute(a, "href", href);
            a.appendChild(d.createTextNode(bitstream.getName()));
            li.appendChild(a);
            ulBitstreams.appendChild(li);
        }

        c.abort(); // Same as c.complete() because we didn't modify the db.
        return representation;
    }
    
    @Get("json")
    public String toJson(){
        GetOptions.allowAccess(getResponse());
        Bundle bu = null;
        Context c = null;
        try{
            c = new Context();
            bu = Bundle.find(c, bundleId);
        }catch(Exception e){
            if(c != null)
                c.abort();
            return errorInternal(c, e.toString()).getText();
        }finally{
            if(c != null)
                c.abort();
        }
        
        Gson gson = new Gson();
        StubBundle s = new StubBundle(bu.getID(), bu.getName(), bu.getPrimaryBitstreamID(), bu.getBitstreams());
        try{
           c.abort();
        }catch(Exception e){
            log.log(Priority.FATAL, e);
        }
        return gson.toJson(s);
    }

    @Put
    public Representation editBundle(InputRepresentation rep) {
        Context c = null;
        Bundle bundle = null;
        try {
            c = getAuthenticatedContext();
            bundle = Bundle.find(c, this.bundleId);
            if (bundle == null) {
                return errorNotFound(c, "Could not find the bundle.");
            }
        }
        catch (SQLException e) {
            return errorInternal(c, "SQLException");
        }
	
        DomRepresentation dom = new DomRepresentation(rep);
        Node attributesNode = dom.getNode("//dl[@id='attributes']");
        if (attributesNode == null) {
            return error(c, "Did not find dl tag with a id 'attributes'.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        int nameFound = 0;
        int primarybitstreamidFound = 0;
        
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
            if (dt.equals("name")) {
                nameFound = 1;
                bundle.setName(dd);
            } else if (dt.equals("primarybitstreamid")) {
                primarybitstreamidFound = 1;
                Integer id = Integer.parseInt(dd);

                boolean validBitstreamId = false;
                Bitstream[] bitstreams = bundle.getBitstreams();

                if (id == -1) {              // -1 means that we do not want to
                    validBitstreamId = true; // specify the primary bitstream.
                }
                else {
                    for (Bitstream bitstream : bitstreams) {
                        if (id == bitstream.getID()) {
                            validBitstreamId = true;
                            break;
                        }
                    }
                }
                if (!validBitstreamId) {
                    return error(c, "Invalid primarybitstreamid.",
                                 Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
                }

                if (id == -1) {
                    bundle.unsetPrimaryBitstreamID();
                }
                else {
                    bundle.setPrimaryBitstreamID(id);
                }
            } else {
                return error(c, "Unexpected data in attributes: " + dt,
                             Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        // If the was data missing, report it:
        String[] problems = {"'nameFound' and 'primarybitstreamid'",
                             "'nameFound'", "'primarybitstreamid'", ""};
        String problem = problems[primarybitstreamidFound + 2*nameFound];
        if (!problem.equals("")) {
            return error(c, problem + " was not found from the request.",
                         Status.CLIENT_ERROR_BAD_REQUEST);
        }

        try {
            bundle.update();
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }
	
        return successOk("Bundle updated.");
    }

    @Post
    public Representation addBitstream(InputRepresentation rep) {
        Context c = null;
        Bundle bundle = null;
        Bitstream bitstream = null;
        try {
            c = getAuthenticatedContext();
            bundle = Bundle.find(c, this.bundleId);
            if (bundle == null) { 
                return errorNotFound(c, "Could not find the bundle.");
            }
        
            Item[] items = bundle.getItems();

            RestletFileUpload rfu =
                new RestletFileUpload(new DiskFileItemFactory());
            FileItemIterator iter = rfu.getItemIterator(rep);

            String description = null;
            while (iter.hasNext()) {
                FileItemStream fileItemStream = iter.next();
                if (fileItemStream.isFormField()) {
                    String key = fileItemStream.getFieldName();
                    String value =
                        IOUtils.toString(fileItemStream.openStream(), "UTF-8");
                
                    if (key.equals("description")) {
                        description = value;
                    } else {
                        return error(c, "Unexpected attribute: " + key,
                                     Status.CLIENT_ERROR_BAD_REQUEST);
                    }
                } else {
                    if (bitstream != null) {
                        return error(c,
                                     "Only one file can added in one request.",
                                     Status.CLIENT_ERROR_BAD_REQUEST);
                    }
                    String name = fileItemStream.getName();
                    bitstream =
                        bundle.createBitstream(fileItemStream.openStream());
                    bitstream.setName(name);
                    bitstream.setSource(name);
                    BitstreamFormat bf =
                        FormatIdentifier.guessFormat(c, bitstream);
                    bitstream.setFormat(bf);
                }
            }

            if (bitstream == null) {
                return error(c, "Request does not contain file(?)",
                             Status.CLIENT_ERROR_BAD_REQUEST);
            }
            if (description != null) {
                bitstream.setDescription(description);
            }
            bitstream.update();
            items[0].update(); // This updates at least the
                               // sequence ID of the bitstream.

            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successCreated("Bitstream created.",
                              baseUrl() +
                              BitstreamResource.relativeUrl(bitstream.getID()));
    }

    
    // NOTE: This removes the bundle from all the items it's belong to, so
    // NOTE: it's a real delete!!!
    @Delete
    public Representation deleteBundle() {
        Context c = null;
        Bundle bundle = null;
        try {
            c = getAuthenticatedContext();
            bundle = Bundle.find(c, this.bundleId);
            if (bundle == null) {
                return errorNotFound(c, "Could not find the bundle.");
            }

            Item[] items = bundle.getItems();

            // FIXME: We would like to that the bundle is removed from all the
            // FIXME: items it belongs to... or if we get an exception, not
            // FIXME: from any item.... but this code might remove the bundle
            // FIXME: only from some items before raising an exception. :-(
            for (Item item : items) {
                item.removeBundle(bundle);
            }
            c.complete();
        }
        catch (Exception e) {
            return errorInternal(c, e.toString());
        }

        return successOk("Bundle deleted.");
    }
}
