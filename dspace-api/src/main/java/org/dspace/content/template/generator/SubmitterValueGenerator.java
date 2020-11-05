/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link TemplateValueGenerator} implementation that fills target metadata value with value taken from
 * target item submitter metadata.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class SubmitterValueGenerator implements TemplateValueGenerator {

    @Autowired
    private EPersonService ePersonService;

    @Override
    public String generator(final Context context, final Item targetItem, final Item templateItem,
                            final String extraParams) {

        EPerson eperson = targetItem.getSubmitter();
        if (StringUtils.equalsIgnoreCase(extraParams, "email")) {
            return eperson.getEmail();
        }
        if (StringUtils.equalsIgnoreCase(extraParams, "phone")) {
            return ePersonService.getMetadata(eperson, "phone");
        }
        if (StringUtils.equalsIgnoreCase(extraParams, "fullname")) {
            return eperson.getFullName();
        }
        return ePersonService.getMetadata(eperson, extraParams);

    }
}
