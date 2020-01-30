/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import org.dspace.content.Collection;
import org.dspace.core.Constants;

/**
 * Collection implementation for the IndexableObject
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class IndexableCollection extends IndexableDSpaceObject<Collection> {

    public static final String TYPE = Collection.class.getSimpleName();

    public IndexableCollection(Collection dso) {
        super(dso);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getTypeText() {
        return Constants.typeText[Constants.COLLECTION];
    }
}