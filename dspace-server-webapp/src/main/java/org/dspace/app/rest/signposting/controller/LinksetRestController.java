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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.security.BitstreamMetadataReadPermissionEvaluatorPlugin;
import org.dspace.app.rest.signposting.converter.LinksetRestMessageConverter;
import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.model.LinksetRest;
import org.dspace.app.rest.signposting.model.TypedLinkRest;
import org.dspace.app.rest.signposting.processor.bitstream.BitstreamSignpostingProcessor;
import org.dspace.app.rest.signposting.processor.item.ItemSignpostingProcessor;
import org.dspace.app.rest.signposting.processor.metadata.MetadataSignpostingProcessor;
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

    private static final Logger log = Logger.getLogger(LinksetRestController.class);

    @Autowired
    private Utils utils;
    @Autowired
    private BitstreamService bitstreamService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ConverterService converter;
    @Autowired
    private BitstreamMetadataReadPermissionEvaluatorPlugin bitstreamMetadataReadPermissionEvaluatorPlugin;
    private List<BitstreamSignpostingProcessor> bitstreamProcessors = new DSpace().getServiceManager()
            .getServicesByType(BitstreamSignpostingProcessor.class);
    private List<ItemSignpostingProcessor> itemProcessors = new DSpace().getServiceManager()
            .getServicesByType(ItemSignpostingProcessor.class);
    private List<MetadataSignpostingProcessor> metadataProcessors = new DSpace().getServiceManager()
            .getServicesByType(MetadataSignpostingProcessor.class);

    @PreAuthorize("permitAll()")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity getAll() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ')")
    @RequestMapping(value = "/linksets" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/json",
            method = RequestMethod.GET)
    public LinksetRest getJson(HttpServletRequest request, @PathVariable UUID uuid) {
        try {
            Context context = ContextUtil.obtainContext(request);

            Item item = itemService.find(context, uuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such Item: " + uuid);
            }
            verifyItemIsDiscoverable(item);
            List<List<LinksetNode>> linksetNodes = createLinksetNodes(request, context, item);
            List<Linkset> linksets = linksetNodes.stream().map(LinksetMapper::map).collect(Collectors.toList());
            return converter.toRest(linksets, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @PreAuthorize("hasPermission(#uuid, 'ITEM', 'READ')")
    @RequestMapping(value = "/linksets" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID, method = RequestMethod.GET)
    public String getLset(HttpServletRequest request, @PathVariable UUID uuid) {
        try {
            Context context = ContextUtil.obtainContext(request);

            Item item = itemService.find(context, uuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such Item: " + uuid);
            }
            verifyItemIsDiscoverable(item);
            List<List<LinksetNode>> linksetNodes = createLinksetNodes(request, context, item);
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
                for (ItemSignpostingProcessor processor : itemProcessors) {
                    processor.addLinkSetNodes(context, request, (Item) dso, linksetNodes);
                }
            } else {
                for (BitstreamSignpostingProcessor processor : bitstreamProcessors) {
                    processor.addLinkSetNodes(context, request, (Bitstream) dso, linksetNodes);
                }
            }

            return linksetNodes.stream()
                    .map(node ->
                            new TypedLinkRest(node.getLink(), node.getRelation(), node.getType(), node.getAnchor()))
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<List<LinksetNode>> createLinksetNodes(
            HttpServletRequest request,
            Context context, Item item
    ) throws SQLException {
        ArrayList<List<LinksetNode>> linksets = new ArrayList<>();
        addItemLinksets(request, context, item, linksets);
        addBitstreamLinksets(request, context, item, linksets);
        addMetadataLinksets(request, context, item, linksets);
        return linksets;
    }

    private void addMetadataLinksets(
            HttpServletRequest request,
            Context context,
            Item item,
            ArrayList<List<LinksetNode>> linksets
    ) {
        for (MetadataSignpostingProcessor processor : metadataProcessors) {
            List<LinksetNode> metadataLinkset = new ArrayList<>();
            processor.addLinkSetNodes(context, request, item, metadataLinkset);
            if (!metadataLinkset.isEmpty()) {
                linksets.add(metadataLinkset);
            }
        }
    }

    private void addBitstreamLinksets(
            HttpServletRequest request,
            Context context,
            Item item,
            ArrayList<List<LinksetNode>> linksets
    ) throws SQLException {
        Iterator<Bitstream> bitstreamsIterator = bitstreamService.getItemBitstreams(context, item);
        bitstreamsIterator.forEachRemaining(bitstream -> {
            try {
                boolean isAuthorized = bitstreamMetadataReadPermissionEvaluatorPlugin
                        .metadataReadPermissionOnBitstream(context, bitstream);
                if (isAuthorized) {
                    List<LinksetNode> bitstreamLinkset = new ArrayList<>();
                    for (BitstreamSignpostingProcessor processor : bitstreamProcessors) {
                        processor.addLinkSetNodes(context, request, bitstream, bitstreamLinkset);
                    }
                    if (!bitstreamLinkset.isEmpty()) {
                        linksets.add(bitstreamLinkset);
                    }
                }
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    private void addItemLinksets(
            HttpServletRequest request,
            Context context,
            Item item,
            List<List<LinksetNode>> linksets
    ) {
        List<LinksetNode> linksetNodes = new ArrayList<>();
        if (item.getType() == Constants.ITEM) {
            for (ItemSignpostingProcessor sp : itemProcessors) {
                sp.addLinkSetNodes(context, request, item, linksetNodes);
            }
        }
        linksets.add(linksetNodes);
    }

    private static void verifyItemIsDiscoverable(Item item) {
        if (!item.isDiscoverable()) {
            String message = format("Item with uuid [%s] is not Discoverable", item.getID().toString());
            throw new AccessDeniedException(message);
        }
    }
}
