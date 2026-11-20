/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.ItemExportCrosswalk;
import org.dspace.content.integration.crosswalks.StreamDisseminationCrosswalkMapper;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the ItemExportFormat object.
 * This class is responsible for all business logic calls for the ItemExportFormat object and is autowired by spring.
 * This class should never be accessed directly.
 * 
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ItemExportFormatServiceImpl implements ItemExportFormatService {

    @Autowired
    public StreamDisseminationCrosswalkMapper streamDissiminatorCrosswalkMapper;

    @Override
    public ItemExportFormat get(Context context, String id) {

        StreamDisseminationCrosswalk sdc = this.streamDissiminatorCrosswalkMapper.getByType((String)id);
        return sdc instanceof ItemExportCrosswalk ? buildItemExportFormat(id, (ItemExportCrosswalk) sdc) : null;

    }

    @Override
    public List<ItemExportFormat> getAll(Context context) {

        return this.streamDissiminatorCrosswalkMapper.getAllItemExportCrosswalks().entrySet().stream()
            .filter(entry -> entry.getValue().isAuthorized(context))
            .map(entry -> buildItemExportFormat(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

    }

    @Override
    public List<ItemExportFormat> byEntityTypeAndMolteplicity(Context context, String entityType,
            CrosswalkMode molteplicity) {

        return this.streamDissiminatorCrosswalkMapper.getAllItemExportCrosswalks()
            .entrySet().stream()
            .filter(entry -> hasSameMolteplicity(entry.getValue(), molteplicity))
            .filter(entry -> hasSameEntityType(entry.getValue(), entityType))
            .filter(entry -> entry.getValue().isAuthorized(context))
            .map(entry -> buildItemExportFormat(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

    }

    private boolean hasSameMolteplicity(ItemExportCrosswalk exportCrosswalk, CrosswalkMode molteplicity) {
        CrosswalkMode crosswalkMode = exportCrosswalk.getCrosswalkMode();
        if (crosswalkMode == CrosswalkMode.SINGLE_AND_MULTIPLE) {
            return true;
        }

        return crosswalkMode == molteplicity;
    }

    private boolean hasSameEntityType(ItemExportCrosswalk exportCrosswalk, String entityType) {
        Optional<String> crosswalkEntityType = exportCrosswalk.getEntityType();
        if (!crosswalkEntityType.isPresent() || StringUtils.isBlank(entityType)) {
            return true;
        }
        return crosswalkEntityType.get().equals(entityType);
    }

    private ItemExportFormat buildItemExportFormat(String id, ItemExportCrosswalk sdc) {
        ItemExportFormat itemExportFormatRest = new ItemExportFormat();
        itemExportFormatRest.setId(id);
        itemExportFormatRest.setMolteplicity(sdc.getCrosswalkMode().name());
        sdc.getEntityType().ifPresent(itemExportFormatRest::setEntityType);
        itemExportFormatRest.setMimeType(sdc.getMIMEType());
        return itemExportFormatRest;
    }

}
