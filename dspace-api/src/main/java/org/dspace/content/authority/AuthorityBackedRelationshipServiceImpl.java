/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.dao.ItemDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default implementation of {@link AuthorityBackedRelationshipService}. It stamps
 * the relationship sides on the metadata value itself; Hibernate then writes the
 * {@code relationship} secondary-table row as part of the value's own insert.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class AuthorityBackedRelationshipServiceImpl implements AuthorityBackedRelationshipService {

    private static final Logger log = LogManager.getLogger(AuthorityBackedRelationshipServiceImpl.class);

    /**
     * Length of the canonical string representation of a UUID (8-4-4-4-12). Used to reject
     * authorities that cannot be a UUID before any parsing happens.
     */
    private static final int UUID_STRING_LENGTH = 36;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    @Autowired(required = true)
    protected ItemDAO itemDAO;

    @Override
    public boolean reconcileRelationshipForAuthority(Context context, DSpaceObject owner, MetadataValue metadataValue)
        throws SQLException, AuthorizeException {

        if (context == null || owner == null || metadataValue == null || owner.getType() != Constants.ITEM) {
            // The left side must reference an item, so only items can carry an authority-backed relationship.
            return false;
        }

        UUID ownerItem = owner.getID();
        UUID currentLeft = metadataValue.getLeftItem();
        UUID currentRight = metadataValue.getRightItem();
        // Read the RAW authority column, not the derived getter: for an already-elided internal
        // reference the column is null while the getter would reconstitute the UUID from right_id.
        // Reconcile must see the physical null so it can recognise "already in sync" below.
        String authority = metadataValue.getRawAuthority();

        // Already-in-sync shortcut for a resolved internal reference whose authority column
        // was elided. When this value was stamped, left_id/right_id were set to
        // (owner -> target) and elideAuthorityColumn() nulled the authority column, so the
        // UUID now lives only in right_id. That combination — authority column null, left_id
        // pointing at this owner, right_id set — is a fully reconciled value, so return early.
        // This check MUST run before the resolve/clear logic below: with a null authority,
        // resolveTargetItem() returns null and the clear branch would then null left_id/right_id,
        // destroying a still-valid relationship row on every reconcile.
        if (authority == null && currentRight != null && ownerItem.equals(currentLeft)) {
            return false;
        }

        // Fast path: the authority column is still present and already equals right_id, with
        // left_id pointing at this owner — the value names the right item, so nothing to do.
        // The metadata_value_id foreign key guarantees a stamped right_id still exists, so this
        // common case needs no database access. This runs only while the raw authority is non-null;
        // a null authority is handled by the already-in-sync shortcut above or the clear branch below.
        if (currentRight != null && ownerItem.equals(currentLeft)
            && currentRight.toString().equals(authority == null ? null : authority.trim())) {
            return false;
        }

        UUID targetItem = resolveTargetItem(context, authority);

        if (targetItem == null) {
            // The authority does not name an existing item: drop any stale row.
            if (currentLeft == null && currentRight == null) {
                return false;
            }
            metadataValue.setLeftItem(null);
            metadataValue.setRightItem(null);
            log.debug("Cleared authority-backed relationship of metadata value {} (item {}): "
                    + "authority '{}' does not name an existing item",
                metadataValue.getID(), ownerItem, authority);
            return true;
        }

        if (ownerItem.equals(currentLeft) && targetItem.equals(currentRight)) {
            // Idempotent: the value already names this exact pair.
            return false;
        }

        if (!authorizeService.authorizeActionBoolean(context, owner, Constants.WRITE)) {
            throw new AuthorizeException("You do not have write rights on this relationship's item");
        }

        metadataValue.setLeftItem(ownerItem);
        metadataValue.setRightItem(targetItem);
        // Single-store invariant: once the row is stamped the target UUID lives in right_id, so
        // elide the redundant copy from the authority column. Endpoints are set first (above), so
        // getAuthority() can always reconstitute the UUID from right_id afterwards.
        metadataValue.elideAuthorityColumn();

        log.debug("Stamped metadata value {} (item {}) as authority-backed towards item {}",
            metadataValue.getID(), ownerItem, targetItem);

        return true;
    }

    @Override
    public boolean isValidTarget(Context context, String authority) throws SQLException {
        return resolveTargetItem(context, authority) != null;
    }

    @Override
    public boolean markRelationshipForResolvedAuthority(Context context, Item ownerItem,
        MetadataValue ownerMetadataValue, Item relatedItem) throws SQLException, AuthorizeException {

        if (ownerItem == null || ownerMetadataValue == null || relatedItem == null) {
            return false;
        }

        UUID ownerItemId = ownerItem.getID();
        UUID relatedItemId = relatedItem.getID();

        if (ownerItemId.equals(ownerMetadataValue.getLeftItem())
            && relatedItemId.equals(ownerMetadataValue.getRightItem())) {
            // Idempotent: the value already names this exact pair.
            return false;
        }

        if (!authorizeService.authorizeActionBoolean(context, ownerItem, Constants.WRITE) &&
            !authorizeService.authorizeActionBoolean(context, relatedItem, Constants.WRITE)) {
            throw new AuthorizeException("You do not have write rights on this relationship's items");
        }

        ownerMetadataValue.setLeftItem(ownerItemId);
        ownerMetadataValue.setRightItem(relatedItemId);
        // Single-store invariant: the target UUID now lives in right_id, so elide the redundant
        // copy from the authority column. Endpoints are set first, so getAuthority() can always
        // reconstitute the UUID from right_id afterwards.
        ownerMetadataValue.elideAuthorityColumn();

        log.debug("Stamped metadata value {} (item {}) as authority-backed towards item {}",
            ownerMetadataValue.getID(), ownerItemId, relatedItemId);

        return true;
    }

    /**
     * Resolve the given authority to the UUID of the item it names, or {@code null} when it
     * does not name an existing item. Checks run cheapest first: the prefix and shape checks
     * allocate nothing, the UUID parse is null-safe and never throws, and the item existence
     * check is the only database access.
     *
     * @param context   the DSpace context
     * @param authority the authority key to resolve
     * @return the resolved item UUID, or {@code null} if the authority names no existing item
     * @throws SQLException if a database error occurs
     */
    private UUID resolveTargetItem(Context context, String authority) throws SQLException {
        if (authority == null) {
            return null;
        }
        String candidate = authority.trim();
        if (candidate.isEmpty()) {
            return null;
        }
        if (candidate.startsWith(AuthorityValueService.GENERATE)
            || candidate.startsWith(AuthorityValueService.REFERENCE)
            || candidate.startsWith(Constants.VIRTUAL_AUTHORITY_PREFIX)) {
            return null;
        }
        if (candidate.length() != UUID_STRING_LENGTH) {
            return null;
        }
        for (int i = 0; i < UUID_STRING_LENGTH; i++) {
            char character = candidate.charAt(i);
            boolean hexDigit = (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
            if (!hexDigit && character != '-') {
                return null;
            }
        }
        UUID uuid = UUIDUtils.fromString(candidate);
        if (uuid == null) {
            return null;
        }
        return itemDAO.existsById(context, uuid) ? uuid : null;
    }

}
