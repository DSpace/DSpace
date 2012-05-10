package org.dspace.springmvc;

import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.utils.DSpace;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowRequirementsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;


/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 8/29/11
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */

@Controller
@RequestMapping(value="/resource/**", method=RequestMethod.GET)
public class ResourceIdentifierController {

    public static final String DSPACE_OBJECT = "dspace.object";
    private static final String RESOURCE = "/resource";
    private static final String METS = "mets";
    private static final String DRI = "DRI";

    private static final int STATUS_OK=200;
    private static final int STATUS_FORBIDDEN=400;


    public String processHandle(HttpServletRequest request) {
        String resourceIdentifier=null;
        try {

            String requestUri = request.getRequestURI().toString();

            resourceIdentifier = requestUri.substring(requestUri.indexOf(RESOURCE) + RESOURCE.length() + 1);

            Context context = ContextUtil.obtainContext(request);

            IdentifierService dis = new DSpace().getSingletonService(IdentifierService.class);

            if (dis == null)
                throw new RuntimeException("Cannot instantiate IdentifierService. Problem with spring configuration!");

            DSpaceObject dso = dis.resolve(context, resourceIdentifier);

            if (dso == null) throw new RuntimeException("Cannot find Item!");

            request.setAttribute(DSPACE_OBJECT, dso);

            return "forward:/handle/" + dso.getHandle();

        } catch (SQLException e) {
            return "forward:/error";

        } catch (IdentifierNotResolvableException e) {
            return "forward:/tombstone";

        } catch (IdentifierNotFoundException e) {
            request.setAttribute("identifier", resourceIdentifier);
            return "forward:/identifier-not-found";
        }
    }

    @RequestMapping("/**/mets.xml")
    public String processMETSHandle(HttpServletRequest request) {
        try {

            String requestUri = request.getRequestURI().toString();

            String resourceIdentifier = requestUri.substring(requestUri.indexOf(RESOURCE) + RESOURCE.length() + 1);
            resourceIdentifier = resourceIdentifier.substring(0, resourceIdentifier.indexOf(METS) - 1);

            Context context = ContextUtil.obtainContext(request);

            IdentifierService dis = new DSpace().getSingletonService(IdentifierService.class);

            DSpaceObject dso = dis.resolve(context, resourceIdentifier);

            if (dso == null) return null;

            request.setAttribute(DSPACE_OBJECT, dso);

            return "forward:/metadata/handle/" + dso.getHandle() + "/mets.xml";

        } catch (SQLException e) {
            return "forward:/error";
        } catch (IdentifierNotResolvableException e) {
            return "forward:/tombstone";

        } catch (IdentifierNotFoundException e) {
            return "forward:/identifier-not-found";
        }
    }

    @RequestMapping("/**/DRI")
    public String processDRIHandle(HttpServletRequest request) {
        try {

            String requestUri = request.getRequestURI().toString();

            String resourceIdentifier = requestUri.substring(requestUri.indexOf(RESOURCE) + RESOURCE.length() + 1);
            resourceIdentifier = resourceIdentifier.substring(0, resourceIdentifier.indexOf(DRI) - 1);

            Context context = ContextUtil.obtainContext(request);

            IdentifierService dis = new DSpace().getSingletonService(IdentifierService.class);

            DSpaceObject dso = dis.resolve(context, resourceIdentifier);

            if (dso == null) return null;

            request.setAttribute(DSPACE_OBJECT, dso);

            return "forward:/DRI/handle/" + dso.getHandle();
        } catch (SQLException e) {
            return "forward:/error";

        } catch (IdentifierNotResolvableException e) {
            return "forward:/tombstone";

        } catch (IdentifierNotFoundException e) {
            return "forward:/identifier-not-found";
        }
    }



    @RequestMapping("/resource/{prefix}/{suffix}.dap")
    public ModelAndView genBankDapRequestDP(@PathVariable String prefix, @PathVariable String suffix, HttpServletRequest request, HttpServletResponse response) {

        String resourceIdentifier = prefix + "/" + suffix;

        int status = validate(resourceIdentifier, request);
        response.setStatus(status);
        if(status==STATUS_FORBIDDEN)
            return null;

        return new ModelAndView(new DapView());
    }


    @RequestMapping("/resource/{prefix}/{suffix}/{count}.dap")
    public ModelAndView genBankDapRequestDF(@PathVariable String prefix, @PathVariable String suffix, @PathVariable String count, HttpServletRequest request, HttpServletResponse response) {

        String resourceIdentifier = prefix + "/" + suffix + "/" + count;


        int status = validate(resourceIdentifier, request);
        response.setStatus(status);
        if(status==STATUS_FORBIDDEN)
            return null;

        return new ModelAndView(new DapView());
    }


    @RequestMapping("/resource/{prefix}/{suffix}/citation/ris")
    public ModelAndView genRisRepresentation(@PathVariable String prefix, @PathVariable String suffix, HttpServletRequest request, HttpServletResponse response) {
        String resourceIdentifier = prefix + "/" + suffix;
        request.setAttribute(DSPACE_OBJECT, getDSO(request, resourceIdentifier));
        return new ModelAndView(new RisView());
    }

    @RequestMapping("/resource/{prefix}/{suffix}/citation/bib")
    public ModelAndView genBibTexRepresentation(@PathVariable String prefix, @PathVariable String suffix, HttpServletRequest request, HttpServletResponse response) {
        String resourceIdentifier = prefix + "/" + suffix;
        request.setAttribute(DSPACE_OBJECT, getDSO(request, resourceIdentifier) );
        return new ModelAndView(new BibTexView());
    }


    private DSpaceObject getDSO(HttpServletRequest request, String resourceIdentifier) {
        DSpaceObject dso=null;
        IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
        Context context =null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            dso = identifierService.resolve(context, resourceIdentifier);
            if(dso==null) throw new RuntimeException("Invalid DOI! " + resourceIdentifier);

            return dso;
        }catch (IdentifierNotFoundException e) {
            throw new RuntimeException(e);

        } catch (IdentifierNotResolvableException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);

        }
    }


    private int validate(String doi, HttpServletRequest request){
        String token = request.getParameter("token");

        if(token==null || "".equals(token)) return STATUS_FORBIDDEN;

        if(doi==null || "".equals(doi)) return STATUS_FORBIDDEN;

        // try to resolve DOI
        DSpaceObject dso=null;
        IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
        Context context =null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            dso = identifierService.resolve(context, doi);
            request.setAttribute(DSPACE_OBJECT, dso);

            if(!(dso instanceof Item)) return STATUS_FORBIDDEN;

            Item item = (Item)dso;

            // if item is a dataFile retrieve the token from the dataPacakge
            String collection = getCollection(context, item);
            String myDataPkgColl = ConfigurationManager.getProperty("stats.datapkgs.coll");
            DCValue[] values=null;
            if(!collection.equals(myDataPkgColl)){
                Item i = DryadWorkflowUtils.getDataPackage(context, item);
                // compare token_in_input with the token_in__database
                 values= i.getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA + ".genbank.token");
            }
            else values= item.getMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA + ".genbank.token");


            if(values == null || values.length==0) return STATUS_FORBIDDEN;

            String itemToken = values[0].value;
            if(!itemToken.equals(token)) return STATUS_FORBIDDEN;

            return STATUS_OK;

        }catch (SQLException e) {
            return STATUS_FORBIDDEN;
        }catch (IdentifierNotFoundException e) {
            return STATUS_FORBIDDEN;

        } catch (IdentifierNotResolvableException e) {
            return STATUS_FORBIDDEN;
        }
    }

    private String getCollection(Context context, Item item) throws SQLException {
        String collectionResult = null;

        if(item.getOwningCollection()!=null)
            return item.getOwningCollection().getHandle();

        // If our item is a workspaceitem it cannot have a collection, so we will need to get our collection from the workspace item
        return getCollectionFromWI(context, item.getID()).getHandle();
    }

    private Collection getCollectionFromWI(Context context, int itemId) throws SQLException {

        TableRow row = DatabaseManager.querySingleTable(context, "workspaceitem", "SELECT collection_id FROM workspaceitem WHERE item_id= ?", itemId);
        if (row != null) return Collection.find(context, row.getIntColumn("collection_id"));

        row = DatabaseManager.querySingleTable(context, "workflowitem", "SELECT collection_id FROM workflowitem WHERE item_id= ?", itemId);
        if (row != null) return Collection.find(context, row.getIntColumn("collection_id"));

        throw new RuntimeException("Collection not found for item: " + itemId);

    }




}
