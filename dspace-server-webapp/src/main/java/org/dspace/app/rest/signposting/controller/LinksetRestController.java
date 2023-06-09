/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.controller;

import static java.lang.String.format;
import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRest;
import org.dspace.app.rest.signposting.model.TypedLinkRest;
import org.dspace.app.rest.signposting.processor.bitstream.BitstreamSignpostingProcessor;
import org.dspace.app.rest.signposting.processor.item.ItemSignpostingProcessor;
import org.dspace.app.rest.signposting.utils.LinksetMapper;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
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

    @PreAuthorize("permitAll()")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity getAll() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/linksets" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID, method = RequestMethod.GET)
    public ResponseEntity getOne() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ')")
    @RequestMapping(value = "/linksets" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/json",
            method = RequestMethod.GET, produces = "application/linkset+json")
    public LinksetRest getJson(HttpServletRequest request, @PathVariable UUID uuid) {
        try {
            Context context = ContextUtil.obtainContext(request);

            Item item = itemService.find(context, uuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such Item: " + uuid);
            }
            verifyItemIsDiscoverable(item);

            List<LinksetNode> linksetNodes = new ArrayList<>();
            if (item.getType() == Constants.ITEM) {
                List<ItemSignpostingProcessor> ispp = new DSpace().getServiceManager()
                        .getServicesByType(ItemSignpostingProcessor.class);
                for (ItemSignpostingProcessor sp : ispp) {
                    sp.addLinkSetNodes(context, request, item, linksetNodes);
                }
            }
            return converter.toRest(LinksetMapper.map(linksetNodes), utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ')")
    @RequestMapping(value = "/linksets" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID,
            method = RequestMethod.GET, produces = "application/linkset")
    public LinksetRest getLset(HttpServletRequest request, @PathVariable UUID uuid) {
        try {
            Context context = ContextUtil.obtainContext(request);

            Item item = itemService.find(context, uuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such Item: " + uuid);
            }
            verifyItemIsDiscoverable(item);

            List<LinksetNode> linksetNodes = new ArrayList<>();
            List<ItemSignpostingProcessor> ispp = new DSpace().getServiceManager()
                    .getServicesByType(ItemSignpostingProcessor.class);
            for (ItemSignpostingProcessor sp : ispp) {
                sp.addLinkSetNodes(context, request, item, linksetNodes);
            }

            LinksetRest linksetRest = null;
            for (LinksetNode linksetNode : linksetNodes) {
                if (linksetRest == null) {
                    linksetRest = converter.toRest(linksetNode, utils.obtainProjection());
                } else {
                    linksetRest.getLinksetNodes().add(linksetNode);
                }
            }
            return linksetRest;
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
        try {
            Context context = ContextUtil.obtainContext(request);

            DSpaceObject dso = bitstreamService.find(context, uuid);
            if (dso == null) {
                dso = itemService.find(context, uuid);
                if (dso == null) {
                    throw new ResourceNotFoundException("No such resource: " + uuid);
                }
            }

            List<LinksetNode> linksetNodes = new ArrayList<>();
            if (dso.getType() == Constants.ITEM) {
                verifyItemIsDiscoverable((Item) dso);
                List<ItemSignpostingProcessor> ispp = new DSpace().getServiceManager()
                        .getServicesByType(ItemSignpostingProcessor.class);
                for (ItemSignpostingProcessor sp : ispp) {
                    sp.addLinkSetNodes(context, request, (Item) dso, linksetNodes);
                }
            } else {
                List<BitstreamSignpostingProcessor> bspp = new DSpace().getServiceManager()
                        .getServicesByType(BitstreamSignpostingProcessor.class);
                for (BitstreamSignpostingProcessor sp : bspp) {
                    sp.addLinkSetNodes(context, request, (Bitstream) dso, linksetNodes);
                }
            }

            return linksetNodes.stream()
                    .map(node -> new TypedLinkRest(node.getLink(), node.getRelation(), node.getType()))
                    .collect(Collectors.toList());
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
