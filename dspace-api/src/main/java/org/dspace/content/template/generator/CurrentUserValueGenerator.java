/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import java.util.Arrays;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Implementation of {@link TemplateValueGenerator} that returns a metadata
 * value with the name of the current user.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CurrentUserValueGenerator implements TemplateValueGenerator {

    @Override
    public List<MetadataValueVO> generator(Context context, Item targetItem, Item templateItem, String extraParams) {
        EPerson currentUser = context.getCurrentUser();
        return Arrays.asList(
            currentUser == null ?
                new MetadataValueVO("") :
                new MetadataValueVO(currentUser.getName(), currentUser.getID().toString())
        );
    }

}
