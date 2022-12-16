/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning.utils;

import static org.dspace.versioning.utils.RelationshipVersioningUtils.LatestVersionStatusChangelog.LEFT_SIDE_CHANGED;
import static org.dspace.versioning.utils.RelationshipVersioningUtils.LatestVersionStatusChangelog.NO_CHANGES;
import static org.dspace.versioning.utils.RelationshipVersioningUtils.LatestVersionStatusChangelog.RIGHT_SIDE_CHANGED;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Relationship;
import org.dspace.content.Relationship.LatestVersionStatus;

/**
 * Class with utility methods to manipulate relationships that are linked to versioned items.
 * Specifically focussed on the "latest version status" of relationships,
 * which controls which related items are relevant (visible) to any given item.
 */
public class RelationshipVersioningUtils {

    private static final Logger log = LogManager.getLogger(RelationshipVersioningUtils.class);

    /**
     * Given a latest version status, check if the other side is "latest".
     * If we look from the left, this implies BOTH and RIGHT_ONLY return true.
     * If we look from the right, this implies BOTH and LEFT_ONLY return true.
     * @param isLeft whether we should look from the left or right side.
     * @param latestVersionStatus the latest version status.
     * @return true if the other side has "latest" status, false otherwise.
     */
    public boolean otherSideIsLatest(boolean isLeft, LatestVersionStatus latestVersionStatus) {
        if (latestVersionStatus == LatestVersionStatus.BOTH) {
            return true;
        }

        return latestVersionStatus == (isLeft ? LatestVersionStatus.RIGHT_ONLY : LatestVersionStatus.LEFT_ONLY);
    }

    public enum LatestVersionStatusChangelog {
        NO_CHANGES,
        LEFT_SIDE_CHANGED,
        RIGHT_SIDE_CHANGED
    }

    /**
     * Update {@link Relationship#latestVersionStatus} of the given relationship.
     * If isLatest = true, this method will never throw IllegalStateException.
     * If isLatest = false, you should make sure that the selected side of given relationship
     * currently has "latest" status, otherwise IllegalStateException will be thrown.
     * @param relationship the relationship.
     * @param updateLeftSide whether the status of the left item or the right item should be updated.
     * @param isLatest to what the status should be set.
     * @throws IllegalStateException if the operation would result in both the left side and the right side
     *                               being set to non-latest.
     */
    public LatestVersionStatusChangelog updateLatestVersionStatus(
        Relationship relationship, boolean updateLeftSide, boolean isLatest
    ) throws IllegalStateException {
        LatestVersionStatus lvs = relationship.getLatestVersionStatus();

        boolean leftSideIsLatest = lvs == LatestVersionStatus.BOTH || lvs == LatestVersionStatus.LEFT_ONLY;
        boolean rightSideIsLatest = lvs == LatestVersionStatus.BOTH || lvs == LatestVersionStatus.RIGHT_ONLY;

        if (updateLeftSide) {
            if (leftSideIsLatest == isLatest) {
                return NO_CHANGES; // no change needed
            }
            leftSideIsLatest = isLatest;
        } else {
            if (rightSideIsLatest == isLatest) {
                return NO_CHANGES; // no change needed
            }
            rightSideIsLatest = isLatest;
        }

        LatestVersionStatus newVersionStatus;
        if (leftSideIsLatest && rightSideIsLatest) {
            newVersionStatus = LatestVersionStatus.BOTH;
        } else if (leftSideIsLatest) {
            newVersionStatus = LatestVersionStatus.LEFT_ONLY;
        } else if (rightSideIsLatest) {
            newVersionStatus = LatestVersionStatus.RIGHT_ONLY;
        } else {
            String msg = String.format(
                "Illegal state: cannot set %s item to latest = false, because relationship with id %s, " +
                "rightward name %s between left item with uuid %s, handle %s and right item with uuid %s, handle %s " +
                "has latest version status set to %s",
                updateLeftSide ? "left" : "right", relationship.getID(),
                relationship.getRelationshipType().getRightwardType(),
                relationship.getLeftItem().getID(), relationship.getLeftItem().getHandle(),
                relationship.getRightItem().getID(), relationship.getRightItem().getHandle(), lvs
            );
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        log.info(
            "set latest version status from {} to {} for relationship with id {}, rightward name {} " +
            "between left item with uuid {}, handle {} and right item with uuid {}, handle {}",
            lvs, newVersionStatus, relationship.getID(), relationship.getRelationshipType().getRightwardType(),
            relationship.getLeftItem().getID(), relationship.getLeftItem().getHandle(),
            relationship.getRightItem().getID(), relationship.getRightItem().getHandle()
        );
        relationship.setLatestVersionStatus(newVersionStatus);

        return updateLeftSide ? LEFT_SIDE_CHANGED : RIGHT_SIDE_CHANGED;
    }

}
