/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.controller;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.signposting.converter.LinksetRestMessageConverter;
import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRest;
import org.dspace.app.rest.signposting.model.TypedLinkRest;
import org.dspace.app.rest.signposting.service.LinksetService;
import org.dspace.app.rest.signposting.utils.LinksetMapper;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This RestController takes care of the retrieval of {@link LinksetRest}.
 * This class will receive the UUID of an {@link Item} or {@link Bitstream}.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
@RestController
@RequestMapping("/${signposting.path:signposting}")
@ConditionalOnProperty("signposting.enabled")
public class LinksetRestController {

    @Autowired
    private Utils utils;
    @Autowired
    private BitstreamService bitstreamService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ConverterService converter;
    @Autowired
    private LinksetService linksetService;
    @Autowired
    private ConfigurationService configurationService;
    private final PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    @PreAuthorize("permitAll()")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity getAll() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ')")
    @RequestMapping(
            value = "/linksets" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/json",
            method = RequestMethod.GET,
            produces = "application/linkset+json"
    )
    public LinksetRest getJson(HttpServletRequest request, @PathVariable UUID uuid) {
        try {
            Context context = ContextUtil.obtainContext(request);

            Item item = itemService.find(context, uuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such Item: " + uuid);
            }
            verifyItemIsDiscoverable(item);
            List<List<LinksetNode>> linksetNodes = linksetService
                    .createLinksetNodesForMultipleLinksets(request, context, item);
            List<Linkset> linksets = linksetNodes.stream().map(LinksetMapper::map).collect(Collectors.toList());
            return converter.toRest(linksets, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ')")
    @RequestMapping(
            value = "/linksets" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID,
            method = RequestMethod.GET,
            produces = "application/linkset"
    )
    public String getLset(HttpServletRequest request, @PathVariable UUID uuid) {
        try {
            Context context = ContextUtil.obtainContext(request);
            Item item = itemService.find(context, uuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such Item: " + uuid);
            }
            verifyItemIsDiscoverable(item);
            List<List<LinksetNode>> linksetNodes = linksetService
                    .createLinksetNodesForMultipleLinksets(request, context, item);
            return LinksetRestMessageConverter.convert(linksetNodes);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // In @PreAuthorize(...) we're using "&&" (and) instead of "||" (or) because if hasPermission() is unable
    // to find object of specified type with specified uuid it returns "true".
    // For example: if we pass uuid of Bitstream: hasPermission(#uuid, 'ITEM', 'READ') returns "true", because
    // it will use ItemService with uuid of bitstream.
    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ') && hasPermission(#uuid, 'BITSTREAM', 'READ')")
    @RequestMapping(value = "/links" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID, method = RequestMethod.GET)
    public List<TypedLinkRest> getHeader(HttpServletRequest request, @PathVariable UUID uuid) {
        Context context = ContextUtil.obtainContext(request);
        DSpaceObject dso = findObject(context, uuid);
        List<LinksetNode> linksetNodes = linksetService.createLinksetNodesForSingleLinkset(request, context, dso);
        return linksetNodes.stream()
                .map(node -> new TypedLinkRest(node.getLink(), node.getRelation(), node.getType()))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ')")
    @RequestMapping(value = "/describedby" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID, method = RequestMethod.GET)
    public String getDescribedBy(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable UUID uuid
    ) throws SQLException, AuthorizeException, IOException, CrosswalkException {
        Context context = ContextUtil.obtainContext(request);
        String xwalkName = configurationService.getProperty("signposting.describedby.crosswalk-name");
        String responseMimeType = configurationService.getProperty("signposting.describedby.mime-type");
        response.addHeader("Content-Type", responseMimeType);

        DSpaceObject object = findObject(context, uuid);
        DisseminationCrosswalk xwalk = (DisseminationCrosswalk)
                pluginService.getNamedPlugin(DisseminationCrosswalk.class, xwalkName);
        List<Element> elements = xwalk.disseminateList(context, object);
        XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
        return outputter.outputString(elements);
    }

    private DSpaceObject findObject(Context context, UUID uuid) {
        try {
            DSpaceObject object = itemService.find(context, uuid);
            if (isNull(object)) {
                object = bitstreamService.find(context, uuid);
                if (isNull(object)) {
                    throw new ResourceNotFoundException("No such resource: " + uuid);
                }
            } else {
                verifyItemIsDiscoverable((Item) object);
            }
            return object;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void verifyItemIsDiscoverable(Item item) {
        if (!item.isDiscoverable()) {
            String message = format("Item with uuid [%s] is not Discoverable", item.getID().toString());
            throw new AccessDeniedException(message);
        }
    }
}
