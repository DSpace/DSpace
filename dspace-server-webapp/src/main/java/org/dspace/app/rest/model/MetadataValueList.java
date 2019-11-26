/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.AbstractList;
import java.util.List;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.MetadataConverter;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.MetadataValue;

/**
 * Type-safe wrapper for a list of {@link MetadataValue}s for use with {@link MetadataConverter},
 * so it can be invoked properly via calls to {@link ConverterService#toRest(Object, Projection)}.
 */
public class MetadataValueList extends AbstractList<MetadataValue> {

    private final List<MetadataValue> list;

    public MetadataValueList(List<MetadataValue> list) {
        this.list = list;
    }

    @Override
    public MetadataValue get(int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }
}
