/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.MetadataFieldDescriptor;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class SpacesInputCleaner implements InputCleaner {

    protected List<MetadataFieldDescriptor> metadataFields = new LinkedList<>();

    public SpacesInputCleaner(List<MetadataFieldDescriptor> metadataFields) {
        this.metadataFields = metadataFields;
    }

    @Override
    public String cleanup(String input) {
        return StringUtils.strip(input.replaceAll("\\s{2,}", " "));
    }

    @Override
    public List<MetadataFieldDescriptor> getMetadataFields() {
        return Collections.unmodifiableList(metadataFields);
    }
}
