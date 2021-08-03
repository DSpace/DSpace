/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link TemplateValueGenerator} that fills metadata value with group id.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class GroupValueGenerator implements TemplateValueGenerator {

    private static final Logger log = LoggerFactory.getLogger(GroupValueGenerator.class);

    @Autowired
    private ItemService itemService;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private CommunityService communityService;
    @Autowired
    private GroupService groupService;


    @Override
    public List<MetadataValueVO> generator(Context context, Item targetItem, Item templateItem, String extraParams) {

        String[] params = StringUtils.split(extraParams, "\\.");
        String prefix = params[0];
        String suffix = "";
        if (params.length > 1) {
            suffix = params[1];
        }
        String value = prefix;
        try {
            if (StringUtils.startsWith(prefix, "community")) {
                String metadata = prefix.substring("community[".length(),
                                                   prefix.length() - 1);

                final Collection owningCollection = templateItem.getOwningCollection();
                final Community parentObject = (Community) collectionService.getParentObject(context, owningCollection);

                value = communityService.getMetadata(parentObject, metadata);

            } else if (StringUtils.startsWith(prefix, "collection")) {
                String metadata = prefix.substring("collection[".length(),
                                                   prefix.length() - 1);
                final Collection owningCollection = templateItem.getOwningCollection();
                value = collectionService.getMetadata(owningCollection, metadata);
            } else if (StringUtils.startsWith(prefix, "item")) {
                value = itemService.getMetadata(targetItem, prefix.replace("_", "."));
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }

        if (StringUtils.isNotBlank(suffix)) {
            value = value + "-" + suffix;
        }

        Group group = null;
        try {
            group = groupService.findByName(context, value);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return Arrays.asList(group != null ?
                new MetadataValueVO(value, group.getID().toString()) : new MetadataValueVO(""));
    }
}
