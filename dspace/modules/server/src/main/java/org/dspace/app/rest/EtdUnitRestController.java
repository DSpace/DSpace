/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static java.util.regex.Pattern.compile;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.dspace.app.rest.utils.ContextUtil.obtainContext;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_UUID;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.EtdUnitRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EtdUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This will be the entry point for the api/eperson/EtdUnits endpoint with
 * additional paths to it
 */
@RestController
@RequestMapping("/api/" + EtdUnitRest.CATEGORY + "/" + EtdUnitRest.ETDUNITS)
public class EtdUnitRestController {

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private EtdUnitService etdunitService;

    @Autowired
    Utils utils;

    /**
     * Method to add one or more Collections to a EtdUnit
     * The Collections to be added should be provided in the request body as a
     * uri-list.
     *
     * @param uuid     the UUID of the EtdUnit to add the Collections to
     * @param response the HttpServletResponse object
     * @param request  the HttpServletRequest object
     * @throws SQLException       if a database error occurs
     * @throws AuthorizeException if user is not authorized for this action
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = POST, path = "/{uuid}/collections", consumes = { "text/uri-list" })
    public void addCollections(@PathVariable UUID uuid, HttpServletResponse response, HttpServletRequest request)
            throws SQLException, AuthorizeException {

        Context context = obtainContext(request);

        EtdUnit EtdUnit = etdunitService.find(context, uuid);
        if (EtdUnit == null) {
            throw new ResourceNotFoundException("EtdUnit is not found for uuid: " + uuid);
        }

        List<String> collectionLinks = utils.getStringListFromRequest(request);

        List<Collection> collections = new ArrayList<>();
        for (String collectionLink : collectionLinks) {
            Optional<Collection> collection = findCollection(context, collectionLink);
            if (!collection.isPresent() /* || !canAddCollection(context, EtdUnit, Collection.get()) */) {
                throw new UnprocessableEntityException("cannot add collection: " + collectionLink);
            }
            collections.add(collection.get());
        }

        for (Collection collection : collections) {
            etdunitService.addCollection(context, EtdUnit, collection);
        }

        etdunitService.update(context, EtdUnit);
        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }

    /**
     * Returns an Optional<Collection> for the given link
     *
     * @param context        the DSpace context
     * @param collectionLink the URL of the collection
     * @return an Optional<Collection> for the given link
     * @throws SQLException if a database error occurs.
     */
    private Optional<Collection> findCollection(Context context, String collectionLink) throws SQLException {
        Collection collection = null;

        Pattern linkPattern = compile("^.*/(" + REGEX_UUID + ")/?$");
        Matcher matcher = linkPattern.matcher(collectionLink);
        if (matcher.matches()) {
            collection = collectionService.find(context, UUID.fromString(matcher.group(1)));
        }

        return Optional.ofNullable(collection);
    }

    /**
     * Method to remove a collection from a EtdUnit.
     *
     * @param etdunitUUID    the UUID of the etdunit
     * @param collectionUUID the UUID of the collection to remove
     * @param response       the HttpServletResponse object
     * @param request        the HttpServletRequest object
     * @throws IOException        if an I/O error occurs
     * @throws SQLException       if a database error occurs
     * @throws AuthorizeException if user is not authorized for this action
     */
    @PreAuthorize("hasAuthority('ADMIN')")
    @RequestMapping(method = DELETE, path = "/{etdunitUUID}/collections/{collectionUUID}")
    public void removeCollection(@PathVariable UUID etdunitUUID, @PathVariable UUID collectionUUID,
            HttpServletResponse response, HttpServletRequest request)
            throws IOException, SQLException, AuthorizeException {

        Context context = obtainContext(request);

        EtdUnit etdunit = etdunitService.find(context, etdunitUUID);
        if (etdunit == null) {
            throw new ResourceNotFoundException("etdunit is not found for uuid: " + etdunitUUID);
        }

        Collection collection = collectionService.find(context, collectionUUID);
        if (collection == null) {
            response.sendError(SC_UNPROCESSABLE_ENTITY);
            return;
        }

        etdunitService.removeCollection(context, etdunit, collection);
        etdunitService.update(context, etdunit);
        ;
        context.complete();

        response.setStatus(SC_NO_CONTENT);
    }
}
