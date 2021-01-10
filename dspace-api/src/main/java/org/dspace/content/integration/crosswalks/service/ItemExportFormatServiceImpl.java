/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        return sdc instanceof ItemExportCrosswalk ? create(id, (ItemExportCrosswalk) sdc) : null;

    }

    @Override
    public List<ItemExportFormat> getAll(Context context) {

        return this.streamDissiminatorCrosswalkMapper.getAllItemExportCrosswalks().entrySet().stream()
            .map(entry -> create(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

    }

    @Override
    public List<ItemExportFormat> byEntityTypeAndMolteplicity(Context context, String entityTypeId,
            CrosswalkMode molteplicity) {

        Map<String, ItemExportCrosswalk> map = this.streamDissiminatorCrosswalkMapper.getAllItemExportCrosswalks()
                .entrySet().stream()
                // filter molteplicity
                .filter(entry -> {
                    if (entry.getValue().getCrosswalkMode().equals(CrosswalkMode.SINGLE_AND_MULTIPLE)) {
                        return true;
                    }
                    return entry.getValue().getCrosswalkMode().equals(molteplicity);
                })
                // filter entityType
                .filter(entry -> {
                    if (entityTypeId == null) {
                        return true;
                    }
                    return entry.getValue().getEntityType().isPresent()
                            && entry.getValue().getEntityType().get().equals(entityTypeId);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<ItemExportFormat> formats = map.entrySet().stream()
            .map(entry -> create(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

        return formats;

    }

    private ItemExportFormat create(String id, ItemExportCrosswalk sdc) {
        ItemExportFormat itemExportFormatRest = new ItemExportFormat();
        itemExportFormatRest.setId(id);
        itemExportFormatRest.setMolteplicity(sdc.getCrosswalkMode().name());
        itemExportFormatRest.setEntityType(sdc.getEntityType().get());
        itemExportFormatRest.setMimeType(sdc.getMIMEType());
        return itemExportFormatRest;
    }

}
