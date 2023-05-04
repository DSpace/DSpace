/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.controller;

import static org.dspace.app.rest.utils.RegexUtils.REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.LinksetRest;
import org.dspace.app.rest.signposting.model.Lset;
import org.dspace.app.rest.signposting.model.Relation;
import org.dspace.app.rest.signposting.model.TypedLinkRest;
import org.dspace.app.rest.signposting.processor.BitstreamSignPostingProcessor;
import org.dspace.app.rest.signposting.processor.ItemSignPostingProcessor;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private HandleService handleService;
    @Autowired
    private ConfigurationService configurationService;
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

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/linksets" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID + "/json",
            method = RequestMethod.GET, produces = "application/linkset+json")
    public LinksetRest getJson(HttpServletRequest request, @PathVariable UUID uuid) {
        try {
            Context context = ContextUtil.obtainContext(request);

            DSpaceObject dso = null;
            dso = itemService.find(context, uuid);
            if (dso == null) {
                throw new ResourceNotFoundException("No such Item: " + uuid);
            }

            List<Linkset> linksets = new ArrayList<>();
            Linkset primaryLinkset = new Linkset();
            linksets.add(primaryLinkset);

            if (dso.getType() == Constants.ITEM) {
                primaryLinkset.setAnchor(handleService.resolveToURL(
                        context, dso.getHandle()));
                List<ItemSignPostingProcessor> ispp = new DSpace().getServiceManager()
                        .getServicesByType(ItemSignPostingProcessor.class);
                for (ItemSignPostingProcessor sp : ispp) {
                    sp.buildRelation(context, request, (Item) dso, linksets, primaryLinkset);
                }
            }

            LinksetRest linksetRest = null;
            for (Linkset linkset : linksets) {
                if (linksetRest == null) {
                    linksetRest = converter.toRest(linkset, utils.obtainProjection());
                } else {
                    linksetRest.getLinkset().add(linkset);
                }
            }
            return linksetRest;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/linksets" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID,
            method = RequestMethod.GET, produces = "application/linkset")
    public LinksetRest getLset(HttpServletRequest request, @PathVariable UUID uuid) {
        try {
            Context context = ContextUtil.obtainContext(request);

            DSpaceObject dso = null;
            dso = itemService.find(context, uuid);
            if (dso == null) {
                throw new ResourceNotFoundException("No such Item: " + uuid);
            }

            List<Lset> lsets = new ArrayList<>();
            if (dso.getType() == Constants.ITEM) {
                List<ItemSignPostingProcessor> ispp = new DSpace().getServiceManager()
                        .getServicesByType(ItemSignPostingProcessor.class);
                for (ItemSignPostingProcessor sp : ispp) {
                    sp.buildLset(context, request, (Item) dso, lsets);
                }
            }

            LinksetRest linksetRest = null;
            for (Lset lset : lsets) {
                if (linksetRest == null) {
                    linksetRest = converter.toRest(lset, utils.obtainProjection());
                } else {
                    linksetRest.getLset().add(lset);
                }
            }
            return linksetRest;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/links" + REGEX_REQUESTMAPPING_IDENTIFIER_AS_UUID, method = RequestMethod.GET)
    public List<TypedLinkRest> getHeader(HttpServletRequest request, @PathVariable UUID uuid) {
        try {
            Context context = ContextUtil.obtainContext(request);

            DSpaceObject dso = null;
            dso = bitstreamService.find(context, uuid);
            if (dso == null) {
                dso = itemService.find(context, uuid);
                if (dso == null) {
                    throw new ResourceNotFoundException("No such resource: " + uuid);
                }
            }

            List<Linkset> linksets = new ArrayList<>();
            Linkset primaryLinkset = new Linkset();
            linksets.add(primaryLinkset);

            if (dso.getType() == Constants.ITEM) {
                primaryLinkset.setAnchor(handleService.resolveToURL(
                        context, dso.getHandle()));
                List<ItemSignPostingProcessor> ispp = new DSpace().getServiceManager()
                        .getServicesByType(ItemSignPostingProcessor.class);
                for (ItemSignPostingProcessor sp : ispp) {
                    sp.buildRelation(context, request, (Item) dso, linksets, primaryLinkset);
                }
            } else {
                List<BitstreamSignPostingProcessor> bspp = new DSpace().getServiceManager()
                        .getServicesByType(BitstreamSignPostingProcessor.class);
                for (BitstreamSignPostingProcessor sp : bspp) {
                    sp.buildRelation(context, request, (Bitstream) dso, linksets, primaryLinkset);
                }
                String url = configurationService.getProperty("dspace.ui.url");
                primaryLinkset.setAnchor(url + "/bitstreams/" + dso.getID() + "/download");
            }

            return linksets.stream()
                    .flatMap(linkset -> mapTypedLinks(linkset).stream())
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<TypedLinkRest> mapTypedLinks(Linkset linkset) {
        return Stream.of(
                mapTypedLinks(TypedLinkRest.Relation.LANDING_PAGE, linkset.getLandingPage()),
                mapTypedLinks(TypedLinkRest.Relation.ITEM, linkset.getItem()),
                mapTypedLinks(TypedLinkRest.Relation.CITE_AS, linkset.getCiteAs()),
                mapTypedLinks(TypedLinkRest.Relation.AUTHOR, linkset.getAuthor()),
                mapTypedLinks(TypedLinkRest.Relation.TYPE, linkset.getType()),
                mapTypedLinks(TypedLinkRest.Relation.LICENSE, linkset.getLicense()),
                mapTypedLinks(TypedLinkRest.Relation.COLLECTION, linkset.getCollection()),
                mapTypedLinks(TypedLinkRest.Relation.LINKSET, linkset.getLinkset())
        ).flatMap(List::stream).collect(Collectors.toList());
    }

    private static List<TypedLinkRest> mapTypedLinks(TypedLinkRest.Relation relationType, List<Relation> relations) {
        return relations.stream()
                .map(relation -> new TypedLinkRest(relation.getHref(), relationType, relation.getType()))
                .collect(Collectors.toList());
    }
}
