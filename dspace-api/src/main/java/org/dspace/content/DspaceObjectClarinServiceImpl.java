/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.content.service.DspaceObjectClarinService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Additional service implementation for the DspaceObject in Clarin-DSpace.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class DspaceObjectClarinServiceImpl<T extends DSpaceObject> implements DspaceObjectClarinService<T> {
    private WorkspaceItemService workspaceItemService;
    @Override
    public Community getPrincipalCommunity(Context context, DSpaceObject dso) throws SQLException {
        int type = dso.getType();
        // dso is community
        if (type == Constants.COMMUNITY) {
            return (Community) dso;
        }

        Collection collection = this.getCollectionOfDSO(context, dso, type);
        // collection doesn't have the community
        if (Objects.isNull(collection)) {
            return null;
        }

        List<Community> communities = collection.getCommunities();
        // collection doesn't have the community
        if (CollectionUtils.isEmpty(communities)) {
            return null;
        }

        // principal community is in the first index
        return communities.get(0);
    }

    /**
     * Return the collection where belongs current DSpaceObject
     * @param context DSpaceObject contenxt
     * @param dso DSpaceObject Collection or Item
     * @param type number representation of DSpaceObject type
     * @return Collection of the dso
     * @throws SQLException database error
     */
    private Collection getCollectionOfDSO(Context context, DSpaceObject dso, int type) throws SQLException {
        // the dso is Collection
        if (type == Constants.COLLECTION) {
            return (Collection) dso;
        }

        // the dso is not the Item it doesn't have Collection
        if (type != Constants.ITEM) {
            return null;
        }

        Collection collection;
        collection = ((Item) dso).getOwningCollection();
        if (Objects.nonNull(collection)) {
            return collection;
        }

        // the dso doesn't have owning collection try to find the collection from the wi
        WorkspaceItem wi = workspaceItemService.findByItem(context, (Item)dso);
        if (Objects.isNull(wi)) {
            return null;
        }
        return wi.getCollection();
    }
}
