/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.metadata.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.impl.MockDataProvider;
import org.dspace.external.provider.metadata.MetadataSuggestionProvider;

public class MockMetadataSuggestionProvider extends MetadataSuggestionProvider<MockDataProvider> {

    @Override
    public boolean supports(InProgressSubmission inProgressSubmission, String query,
                            Bitstream bitstream, boolean useMetadata) {
        return true;
    }

    @Override
    public List<ExternalDataObject> bitstreamQuery(Bitstream bitstream) {
        List<ExternalDataObject> list = new LinkedList<>();
        list.add(getExternalDataProvider().getExternalDataObject("one").get());
        list.add(getExternalDataProvider().getExternalDataObject("two").get());
        return list;
    }

    @Override
    public List<ExternalDataObject> metadataQuery(Item item, int start, int limit) {
        List<ExternalDataObject> list = new LinkedList<>();
        list.add(getExternalDataProvider().getExternalDataObject("one").get());
        return list;
    }

    @Override
    public List<ExternalDataObject> query(String query, int start, int limit) {
        List<ExternalDataObject> list = new LinkedList<>();
        if (StringUtils.equalsIgnoreCase(query, "one")) {
            list.add(getExternalDataProvider().getExternalDataObject("one").get());
            list.add(getExternalDataProvider().getExternalDataObject("onetwo").get());
        } else {
            list.add(getExternalDataProvider().getExternalDataObject(query).get());
        }
        return list.stream().skip(start).limit(limit).collect(Collectors.toList());
    }
}
