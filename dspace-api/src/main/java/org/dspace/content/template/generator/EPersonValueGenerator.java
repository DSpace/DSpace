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
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Generator for dynamic template item metadata with 'EPERSON' prefix.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class EPersonValueGenerator implements TemplateValueGenerator {

    private static final Logger log = LoggerFactory.getLogger(EPersonValueGenerator.class);

    @Autowired
    private EPersonService ePersonService;
    @Autowired
    private ItemService itemService;


    @Override
    public List<MetadataValueVO> generator(Context context, Item targetItem, Item templateItem, String extraParams) {
        String[] params = StringUtils.split(extraParams, "\\.");
        String prefix = params[0];
        String suffix = "";
        if (params.length > 1) {
            suffix = params[1];
        }
        String value = prefix;
        if (StringUtils.startsWith(prefix, "submitter")) {
            String metadata = prefix.substring("submitter[".length(),
                                               prefix.length() - 1);

            value = ePersonService.getMetadata(targetItem.getSubmitter(), metadata);

        } else if (StringUtils.startsWith(prefix, "item")) {
            value = itemService.getMetadata(targetItem, prefix.replace("_", "."));
        }

        if (StringUtils.isNotBlank(suffix)) {
            value = value + "-" + suffix;
        }

        EPerson ePerson = null;
        try {
            ePerson = ePersonService.findByEmail(context, value);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return Arrays.asList(ePerson != null ?
                new MetadataValueVO(value, ePerson.getID().toString()) : new MetadataValueVO(""));
    }
}
