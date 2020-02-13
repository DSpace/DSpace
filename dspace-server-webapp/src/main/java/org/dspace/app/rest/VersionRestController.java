package org.dspace.app.rest;

import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.hateoas.EPersonResource;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/versioning/versions/{id}")
public class VersionRestController {

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private ConverterService converterService;

    @Autowired
    private Utils utils;

    @RequestMapping(value = "/eperson", method = RequestMethod.GET)
    public EPersonResource retrieve(@PathVariable Integer id,
                                    HttpServletResponse response,
                                    HttpServletRequest request,
                                    Pageable pageable,
                                    PagedResourcesAssembler assembler) throws SQLException {
        Context context = ContextUtil.obtainContext(request);
        Version version = versioningService.getVersion(context, id);
        EPerson ePerson = version.getEPerson();
        if (ePerson == null) {
            throw new ResourceNotFoundException("The EPerson for version with id: " + id + " couldn't be found");
        }
        return converterService.toResource(converterService.toRest(ePerson, utils.obtainProjection()));


    }
}
