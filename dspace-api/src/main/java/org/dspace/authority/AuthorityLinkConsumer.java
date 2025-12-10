/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.itemupdate.MetadataUtilities;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

/**
 *
 * @author Stefano Maffei(stefano.maffei at 4science.com)
 *
 */
public class AuthorityLinkConsumer implements Consumer {

    public static final String CONSUMER_NAME = "authoritylink";

    private ItemService itemService;

    @Override
    @SuppressWarnings("unchecked")
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @Override
    public void finish(Context context) throws Exception {

    }

    @Override
    public void consume(Context context, Event event) throws Exception {

        Item item = (Item) event.getSubject(context);
        if (item == null || !item.isArchived()) {
            return;
        }

        context.turnOffAuthorisationSystem();
        try {
            consumeItem(context, item);
        } finally {
            context.restoreAuthSystemState();
        }

    }

    private void consumeItem(Context context, Item item) throws Exception {
        List<String> linkMetadata = getLinkMetadata(context, item);
        List<MetadataValue> metadataValues = linkMetadata.stream()
            .flatMap((String metadata) -> {
                String[] splittedMetadata;
                try {
                    splittedMetadata = MetadataUtilities.parseCompoundForm(metadata);
                    return itemService.getMetadata(item, splittedMetadata[0], splittedMetadata[1],
                        splittedMetadata.length > 2 ? splittedMetadata[2] : null, Item.ANY).stream();
                } catch (Exception e) {
                    return Stream.of();
                }

            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        metadataValues
            .stream()
            .filter(metadataVal -> StringUtils.isBlank(metadataVal.getAuthority()))
            .forEach(metadataVal -> {
                metadataVal.setAuthority(metadataVal.getValue());
                metadataVal.setConfidence(Choices.CF_ACCEPTED);
            });
        metadataValues
            .stream()
            .filter(metadataVal -> StringUtils.isBlank(metadataVal.getValue()))
            .forEach(metadataVal -> {
                metadataVal.setValue(metadataVal.getAuthority());
                metadataVal.setConfidence(Choices.CF_ACCEPTED);
            });

    }

    private List<String> getLinkMetadata(Context context, Item item) throws DCInputsReaderException {
        List<DCInput> inputs = getAllInputsByCollection(item.getOwningCollection());
        return inputs.stream()
                     .filter(input -> input != null && input.getInputType() != null)
                     .filter(input -> "link".equalsIgnoreCase(input.getInputType()))
                     .map(DCInput::getFieldName)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }

    private List<DCInput> getAllInputsByCollection(Collection collection) throws DCInputsReaderException {
        DCInputsReader inputsReader = new DCInputsReader();
        return inputsReader.getInputsByCollection(collection).stream()
            .flatMap(dcInputSet -> Arrays.stream(dcInputSet.getFields()))
            .flatMap(dcInputs -> Arrays.stream(dcInputs))
            .collect(Collectors.toList());
    }

    @Override
    public void end(Context context) throws Exception {
    }

}
