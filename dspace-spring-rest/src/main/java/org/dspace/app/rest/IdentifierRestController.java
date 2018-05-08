/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;
import org.dspace.app.rest.link.HalLinkService;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.apache.log4j.Logger;
import org.springframework.hateoas.Link;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pid")
public class IdentifierRestController implements InitializingBean {

    private static final Logger log = Logger.getLogger(IdentifierRestController.class);

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @Autowired
    private HalLinkService halLinkService;

    @Override
    public void afterPropertiesSet() throws Exception {
	List<Link> links = new ArrayList<Link>();

        Link l = new Link("/api/pid/handles", "handles");
	links.add ( l );
    }

    /**
     *
     */
    @RequestMapping(method = {RequestMethod.GET,RequestMethod.HEAD}, value = "/handles/{prefix}/{suffix}")
    @SuppressWarnings("unchecked")
    public void getDSObyHandle (@PathVariable String prefix, 
				@PathVariable String suffix, 
				HttpServletResponse response, 
				HttpServletRequest request)  throws IOException, SQLException {

	HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
	Context context = null;
	DSpaceObject dso = null;
	try {
	    context = new Context();
	    dso = handleService.resolveToObject ( context, 
						  prefix + "/" + suffix );
	    if ( dso != null ) {
		int type = dso.getType();
		String model = getModel ( dso.getType() );
		response.setStatus ( HttpServletResponse.SC_FOUND );
		response.sendRedirect ( "/spring-rest/api/core/" 
					+ model + "/" + dso.getID() );
	    }
	    else {
		response.setStatus ( HttpServletResponse.SC_NOT_FOUND );
	    }
	}
	catch ( SQLException e ) {
	    log.error ( "DBG " + e.getMessage() );
	}
    }

    /**
     *
     */
    private String getModel ( int i ) {

	String model = new String();
	switch ( i ) {
	    case 2:
		model =  "items";
		break;
	    case 3:
		model = "collections";
		break;
	    case 4:
		model =  "communities";
		break;
	    default:
		model =  "items";
	}
	return model;
    }
}
