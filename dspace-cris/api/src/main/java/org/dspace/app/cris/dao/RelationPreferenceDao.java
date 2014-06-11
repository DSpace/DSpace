/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import it.cilea.osd.common.dao.GenericDao;

import java.util.List;

import org.dspace.app.cris.model.RelationPreference;

public interface RelationPreferenceDao extends GenericDao<RelationPreference, Integer>
{
    RelationPreference uniqueByUUIDItemID(String UUID, int itemID,
            String relationType);

    RelationPreference uniqueByUUIDs(String sourceUUID, String targetUUID,
            String relationType);
    
    List<RelationPreference> findByTargetUUID(String targetUUID);
    
    List<RelationPreference> findByTargetItemID(int itemID);

    List<RelationPreference> findBySourceUUIDAndRelationType(String sourceUUID, String relationType);

    List<RelationPreference> findBySourceUUIDAndRelationTypeAndStatus(
            String uuid, String relationType, String selected);
}
