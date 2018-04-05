/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.springmvc;

import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;

import org.dspace.core.Context;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.utils.DSpace;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;


/**
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */

@Controller
@RequestMapping(value={"/handle","/resource"})

public class ResourceIdentifierController {

    public static final String DSPACE_OBJECT = "dspace.object";
    private static final String RESOURCE = "/resource";
    private static final String METS = "mets";
    private static final String DRI = "DRI";

    private static final int STATUS_OK=200;
    private static final int STATUS_FORBIDDEN=400;

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD},value={"/{prefix:.*}"})
    public String processHandle(HttpServletRequest request, @PathVariable String prefix) {
        //String resourceIdentifier=null;
        try {

            //String requestUri = request.getRequestURI().toString();

            //resourceIdentifier = requestUri.substring(requestUri.indexOf(RESOURCE) + RESOURCE.length() + 1);

            Context context = ContextUtil.obtainContext(request);

            IdentifierService dis = new DSpace().getSingletonService(IdentifierService.class);

            if (dis == null)
                throw new RuntimeException("Cannot instantiate IdentifierService. Problem with spring configuration!");

            DSpaceObject dso = dis.resolve(context, prefix);

            if (dso == null) throw new IdentifierNotFoundException("Cannot find Item " + prefix + "!");

            request.setAttribute(DSPACE_OBJECT, dso);

            /** TODO: This is a temporary solution until we can adjust cocoon to not have a /handle URI */
            return "forward:/handle/" + dso.getHandle();

        } catch (SQLException e) {
            return "forward:/error";

        } catch (IdentifierNotResolvableException e) {
            return "forward:/tombstone";

        } catch (IdentifierNotFoundException e) {
            request.setAttribute("identifier", prefix);
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


    @RequestMapping("/{prefix}/{suffix}/citation/ris")
    public ModelAndView genRisRepresentation(@PathVariable String prefix, @PathVariable String suffix, HttpServletRequest request, HttpServletResponse response) {
        String resourceIdentifier = prefix + "/" + suffix;
        request.setAttribute(DSPACE_OBJECT, getDSO(request, resourceIdentifier));
        return new ModelAndView(new RisView(resourceIdentifier));
    }

    @RequestMapping("/{prefix}/{suffix}/citation/bib")
    public ModelAndView genBibTexRepresentation(@PathVariable String prefix, @PathVariable String suffix, HttpServletRequest request, HttpServletResponse response) {
        String resourceIdentifier = prefix + "/" + suffix;
        request.setAttribute(DSPACE_OBJECT, getDSO(request, resourceIdentifier) );
        return new ModelAndView(new BibTexView(resourceIdentifier));
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

        }
    }


    private int validate(String resourceID, HttpServletRequest request){
        String token = request.getParameter("token");

        if(token==null || "".equals(token)) return STATUS_FORBIDDEN;

        if(resourceID==null || "".equals(resourceID)) return STATUS_FORBIDDEN;

        // try to resolve DOI
        DSpaceObject dso=null;
        IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
        Context context =null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            dso = identifierService.resolve(context, resourceID);
            request.setAttribute(DSPACE_OBJECT, dso);

            if(!(dso instanceof Item)) return STATUS_FORBIDDEN;

            return STATUS_OK;

        }catch (IdentifierNotFoundException e) {
            return STATUS_FORBIDDEN;

        } catch (IdentifierNotResolvableException e) {
            return STATUS_FORBIDDEN;
        }
    }
}
