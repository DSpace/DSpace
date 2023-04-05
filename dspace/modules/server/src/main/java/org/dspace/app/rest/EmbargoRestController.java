package org.dspace.app.rest;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.lib.dspace.content.EmbargoDTO;
import edu.umd.lib.dspace.content.service.EmbargoDTOService;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST/HAL Browser endpoint for retrieving a list of embargoed items.
 */
@RestController
public class EmbargoRestController implements InitializingBean {
    @Autowired
    private EmbargoDTOService embargoService;

    @Autowired
    DiscoverableEndpointsService discoverableEndpointsService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping("/api/embargo-list")
    public List<EmbargoDTO> embargoList(HttpServletResponse response, HttpServletRequest request) throws SQLException {
        Context context = obtainContext(request);
        List<EmbargoDTO> embargoes = embargoService.getEmbargoList(context);

        return embargoes;
    }

    @Override
    public void afterPropertiesSet() {
        List<Link> links = List.of(Link.of("/api/embargo-list", "embargo-list"));
        discoverableEndpointsService.register(this, links);
    }
}
