/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.service.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.rest.security.BitstreamMetadataReadPermissionEvaluatorPlugin;
import org.dspace.app.rest.signposting.model.LinksetNode;
import org.dspace.app.rest.signposting.processor.bitstream.BitstreamSignpostingProcessor;
import org.dspace.app.rest.signposting.processor.item.ItemSignpostingProcessor;
import org.dspace.app.rest.signposting.processor.metadata.MetadataSignpostingProcessor;
import org.dspace.app.rest.signposting.service.LinksetService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link LinksetService}.
 */
@Service
public class LinksetServiceImpl implements LinksetService {

    private static final Logger log = Logger.getLogger(LinksetServiceImpl.class);

    @Autowired
    protected ItemService itemService;

    @Autowired
    private BitstreamMetadataReadPermissionEvaluatorPlugin bitstreamMetadataReadPermissionEvaluatorPlugin;

    private final List<BitstreamSignpostingProcessor> bitstreamProcessors = new DSpace().getServiceManager()
            .getServicesByType(BitstreamSignpostingProcessor.class);

    private final List<ItemSignpostingProcessor> itemProcessors = new DSpace().getServiceManager()
            .getServicesByType(ItemSignpostingProcessor.class);

    private final List<MetadataSignpostingProcessor> metadataProcessors = new DSpace().getServiceManager()
            .getServicesByType(MetadataSignpostingProcessor.class);

    @Override
    public List<List<LinksetNode>> createLinksetNodesForMultipleLinksets(
            HttpServletRequest request,
            Context context,
            Item item
    ) {
        ArrayList<List<LinksetNode>> linksets = new ArrayList<>();
        addItemLinksets(request, context, item, linksets);
        addBitstreamLinksets(request, context, item, linksets);
        addMetadataLinksets(request, context, item, linksets);
        return linksets;
    }

    @Override
    public List<LinksetNode> createLinksetNodesForSingleLinkset(
            HttpServletRequest request,
            Context context,
            DSpaceObject object
    ) {
        List<LinksetNode> linksetNodes = new ArrayList<>();
        if (object.getType() == Constants.ITEM) {
            for (ItemSignpostingProcessor processor : itemProcessors) {
                processor.addLinkSetNodes(context, request, (Item) object, linksetNodes);
            }
        } else if (object.getType() == Constants.BITSTREAM) {
            for (BitstreamSignpostingProcessor processor : bitstreamProcessors) {
                processor.addLinkSetNodes(context, request, (Bitstream) object, linksetNodes);
            }
        }
        return linksetNodes;
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

    private void addBitstreamLinksets(
            HttpServletRequest request,
            Context context,
            Item item,
            ArrayList<List<LinksetNode>> linksets
    ) {
        Iterator<Bitstream> bitstreamsIterator = getItemBitstreams(context, item);
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

    private Iterator<Bitstream> getItemBitstreams(Context context, Item item) {
        try {
            List<Bundle> bundles = itemService.getBundles(item, Constants.DEFAULT_BUNDLE_NAME);
            return bundles.stream().flatMap(bundle -> bundle.getBitstreams().stream()).iterator();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
