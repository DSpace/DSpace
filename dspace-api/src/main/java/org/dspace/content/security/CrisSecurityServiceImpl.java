/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.exception.SQLRuntimeException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link CrisSecurityService} that evaluates {@link AccessItemMode} security policies.
 * 
 * <p><strong>Overview:</strong></p>
 * <p>This service evaluates complex, configurable access control policies for items and bitstreams
 * based on user roles, group memberships, and metadata-based relationships. It works alongside
 * DSpace's standard ACL system to provide additional, flexible security policies.</p>
 * 
 * <p><strong>Access Evaluation Logic:</strong></p>
 * <p>The service evaluates access in the following order:</p>
 * <ol>
 *   <li><strong>Security Type Evaluation</strong> - Checks each {@link CrisSecurity} policy
 *       defined in the {@link AccessItemMode}
 *   </li>
 *   <li><strong>ANY Match Policy</strong> - Access is granted if ANY security policy passes
 *       (OR logic, not AND)</li>
 *   <li><strong>Optional Filter</strong> - If the access mode defines an additional filter,
 *       it must also pass (AND logic with security check)</li>
 * </ol>
 * 
 * <p><strong>CUSTOM Security - Metadata-Based Access:</strong></p>
 * <p>The CUSTOM security type enables fine-grained access based on metadata values.
 * Access is granted if the user matches any of these metadata-based criteria:</p>
 * <ul>
 *   <li><strong>Group Metadata Fields</strong> - Item metadata contains group UUID/name that
 *       user is a member of. Example: "project.team.groups" field contains "Researchers" group,
 *       and user is in "Researchers" group</li>
 *   <li><strong>User Metadata Fields</strong> - Item metadata authority matches user's UUID.
 *       Example: "dc.contributor.author" has authority matching user's EPerson UUID</li>
 *   <li><strong>Item Metadata Fields</strong> - Item metadata points to related items that
 *       user owns. Example: "relation.isAuthorOfPublication" points to publications where
 *       user is the owner</li>
 * </ul>
 * 
 * <p><strong>Example Access Evaluation Flow:</strong></p>
 * <pre>
 * Given an EditItemMode with securities: [OWNER, CUSTOM]
 * And CUSTOM configured with userMetadataFields: ["dc.contributor.author"]
 * 
 * For a person profile item with:
 *   - Owner: Alice (UUID: 123)
 *   - dc.contributor.author: Bob (UUID: 456, stored as authority)
 * 
 * Access evaluation:
 *   - Alice → OWNER check passes → GRANTED
 *   - Bob → OWNER check fails, CUSTOM check passes (authority match) → GRANTED
 *   - Charlie → Both checks fail → DENIED
 * </pre>
 * 
 * <p><strong>Group Resolution:</strong></p>
 * <p>Groups can be specified by name or UUID. The service resolves them in this order:</p>
 * <ol>
 *   <li>Try parsing as UUID and lookup by UUID</li>
 *   <li>If not a valid UUID, lookup by group name</li>
 *   <li>Handle special groups (Anonymous, Administrator) from context</li>
 * </ol>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @see CrisSecurityService
 * @see AccessItemMode
 * @see CrisSecurity
 * @see org.dspace.content.edit.EditItemMode
 *
 */
public class CrisSecurityServiceImpl implements CrisSecurityService {

    private static final Logger log = LogManager.getLogger(CrisSecurityServiceImpl.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private EPersonService ePersonService;

    /**
     * Checks if the specified user has access to the given item based on the provided access mode.
     * This method evaluates all security policies defined in the access mode and returns true if
     * any of the security checks pass. The access mode may include multiple security levels such as
     * ADMIN, OWNER, SUBMITTER, GROUP membership, or custom policies based on metadata fields.
     *
     * @param context the DSpace context
     * @param item the item to check access for
     * @param user the user (EPerson) requesting access, may be null for anonymous users
     * @param accessMode the access mode containing security policies and optional filters to evaluate
     * @return true if the user has access according to any of the security policies in the access mode,
     *         false otherwise
     * @throws SQLException if a database error occurs during access evaluation
     */
    @Override
    public boolean hasAccess(Context context, Item item, EPerson user, AccessItemMode accessMode) throws SQLException {
        return accessMode.getSecurities().stream()
            .anyMatch(security -> hasAccess(context, item, user, accessMode, security));
    }

    /**
     * Evaluates a single security policy from an access mode and applies optional filter.
     * 
     * <p>This method combines security type checking with optional filter logic:</p>
     * <ul>
     *   <li>First evaluates the security check using {@link #checkSecurity}</li>
     *   <li>If an additional filter is defined, applies it (both must pass)</li>
     *   <li>If no filter is defined, returns the security check result</li>
     * </ul>
     * 
     * @param context      the DSpace context
     * @param item         the item being checked
     * @param user         the user requesting access (may be null for anonymous)
     * @param accessMode   the access mode containing optional filter
     * @param crisSecurity the specific security type to evaluate
     * @return true if security check passes and filter (if present) also passes
     */
    private boolean hasAccess(
        Context context, Item item, EPerson user, AccessItemMode accessMode, CrisSecurity crisSecurity
    ) {
        try {
            final boolean checkSecurity = checkSecurity(context, item, user, accessMode, crisSecurity);

            return Optional.ofNullable(accessMode.getAdditionalFilter())
                .map(filter -> checkSecurity && filter.getResult(context, item))
                .orElse(checkSecurity);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }

    }

    /**
     * Evaluates access based on the specific {@link CrisSecurity} type.
     * 
     * <p>This is the core security evaluation method that implements the logic for each
     * security type. It uses a switch statement to delegate to specialized checks.</p>
     * 
     * <p><strong>Security Type Implementations:</strong></p>
     * <ul>
     *   <li><strong>ADMIN</strong> - Checks if user is repository admin using AuthorizeService</li>
     *   <li><strong>ITEM_ADMIN</strong> - Checks if user is admin for this specific item</li>
     *   <li><strong>OWNER</strong> - Checks if user owns the item via EPersonService</li>
     *   <li><strong>SUBMITTER</strong> - Direct equality check against item submitter</li>
     *   <li><strong>SUBMITTER_GROUP</strong> - Checks if user is in collection's submitter group</li>
     *   <li><strong>GROUP</strong> - Checks if user is member of any configured groups</li>
     *   <li><strong>CUSTOM</strong> - Evaluates metadata-based access policies</li>
     *   <li><strong>ALL</strong> - Always returns true (public access)</li>
     *   <li><strong>NONE</strong> - Always returns false (no access)</li>
     * </ul>
     * 
     * @param context      the DSpace context
     * @param item         the item being checked
     * @param user         the user requesting access (may be null)
     * @param accessMode   the access mode containing metadata field configurations
     * @param crisSecurity the security type to evaluate
     * @return true if the user passes the security check
     * @throws SQLException if a database error occurs during evaluation
     */
    private boolean checkSecurity(Context context, Item item, EPerson user, AccessItemMode accessMode,
                              CrisSecurity crisSecurity) throws SQLException {
        String userEmail = user != null ? user.getEmail() : "anonymous";
        UUID itemId = item.getID();

        log.debug("[CRIS-SECURITY] Evaluating security={} for user={}, item={}",
                  crisSecurity, userEmail, itemId);

        boolean granted;

        switch (crisSecurity) {
            case ADMIN:
                granted = authorizeService.isAdmin(context, user);
                break;
            case CUSTOM:
                granted = hasAccessByCustomPolicy(context, item, user, accessMode);
                break;
            case GROUP:
                granted = hasAccessByGroup(context, user, accessMode.getGroups());
                break;
            case ITEM_ADMIN:
                granted = authorizeService.isAdmin(context, user, item);
                break;
            case OWNER:
                granted = isOwner(user, item);
                break;
            case SUBMITTER:
                granted = user != null && user.equals(item.getSubmitter());
                break;
            case SUBMITTER_GROUP:
                granted = isUserInSubmitterGroup(context, item, user);
                break;
            case ALL:
                granted = true;
                break;
            case NONE:
            default:
                granted = false;
        }

        log.debug("[CRIS-SECURITY] {} security={}, user={}, item={}",
                  granted ? "GRANTED" : "DENIED", crisSecurity, userEmail, itemId);

        return granted;
    }

    /**
     * Checks if the user is the owner of the item.
     * 
     * <p>Delegates to {@link EPersonService#isOwnerOfItem} which determines ownership
     * based on configured ownership metadata fields.</p>
     * 
     * @param eperson the user to check
     * @param item    the item to check ownership for
     * @return true if the user owns the item
     */
    private boolean isOwner(EPerson eperson, Item item) {
        return ePersonService.isOwnerOfItem(eperson, item);
    }

    /**
     * Evaluates CUSTOM security by checking group, user, and item metadata fields.
     * 
     * <p>CUSTOM security grants access through metadata-based matching. This method
     * checks three types of metadata fields in order, granting access if ANY match:</p>
     * <ol>
     *   <li><strong>Group metadata</strong> - Item metadata contains group UUID that user belongs to</li>
     *   <li><strong>User metadata</strong> - Item metadata authority matches user's UUID</li>
     *   <li><strong>Item metadata</strong> - Item metadata references other items user owns</li>
     * </ol>
     * 
     * <p><strong>Use Case Example:</strong> A publication item has dc.contributor.author with
     * authority "uuid-of-bob". When Bob requests access, the user metadata check matches Bob's
     * UUID in the authority field and grants access.</p>
     * 
     * @param context    the DSpace context
     * @param item       the item being checked
     * @param user       the user requesting access
     * @param accessMode the access mode containing metadata field configurations
     * @return true if user matches any metadata-based access criteria
     * @throws SQLException if database error occurs during metadata lookup
     */
    private boolean hasAccessByCustomPolicy(Context context, Item item, EPerson user, AccessItemMode accessMode)
        throws SQLException {

        String userEmail = user != null ? user.getEmail() : "anonymous";
        UUID itemId = item.getID();

        log.debug("[CRIS-SECURITY] Evaluating CUSTOM sub-policies for user={}, item={}",
                  userEmail, itemId);

        if (hasAccessByGroupMetadataFields(context, item, user, accessMode.getGroupMetadataFields())) {
            log.debug("[CRIS-SECURITY] CUSTOM matched via group metadata for user={}, item={}",
                      userEmail, itemId);
            return true;
        }

        if (hasAccessByUserMetadataFields(context, item, user, accessMode.getUserMetadataFields())) {
            log.debug("[CRIS-SECURITY] CUSTOM matched via user metadata for user={}, item={}",
                      userEmail, itemId);
            return true;
        }

        if (hasAccessByItemMetadataFields(context, item, user, accessMode.getItemMetadataFields())) {
            log.debug("[CRIS-SECURITY] CUSTOM matched via item metadata (transitive ownership)"
                      + " for user={}, item={}", userEmail, itemId);
            return true;
        }

        log.debug("[CRIS-SECURITY] CUSTOM no match for user={}, item={}", userEmail, itemId);
        return false;
    }

    /**
     * Checks if user has access based on group membership specified in item metadata.
     * 
     * <p><strong>How it works:</strong></p>
     * <ol>
     *   <li>Retrieves all groups the user belongs to</li>
     *   <li>For each configured group metadata field on the item</li>
     *   <li>Checks if any metadata value's authority matches a user's group UUID</li>
     * </ol>
     *
     * @param context            the DSpace context
     * @param item               the item to check
     * @param user               the user requesting access (returns false if null)
     * @param groupMetadataFields list of metadata field names to check (e.g., ["project.team.groups"])
     * @return true if any of user's groups matches metadata authorities
     * @throws SQLException if database error occurs
     */
    private boolean hasAccessByGroupMetadataFields(Context context, Item item, EPerson user,
        List<String> groupMetadataFields) throws SQLException {

        if (user == null || CollectionUtils.isEmpty(groupMetadataFields)) {
            return false;
        }

        List<Group> userGroups = user.getGroups();
        if (CollectionUtils.isEmpty(userGroups)) {
            return false;
        }

        for (Group group : userGroups) {
            for (String groupMetadataField : groupMetadataFields) {
                if (anyMetadataHasAuthorityEqualsTo(item, groupMetadataField, group.getID())) {
                    log.debug("[CRIS-SECURITY] Group metadata match: user={}, item={},"
                              + " field={}, group={} ({})",
                              user.getEmail(), item.getID(), groupMetadataField,
                              group.getName(), group.getID());
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if user has access based on user UUID specified in item metadata authorities.
     * 
     * <p><strong>How it works:</strong></p>
     * <ol>
     *   <li>For each configured user metadata field (e.g., "dc.contributor.author")</li>
     *   <li>Retrieves metadata values from the item</li>
     *   <li>Checks if any metadata authority equals the user's UUID</li>
     * </ol>
     *
     * <p><strong>Important:</strong> Matches against metadata <em>authority</em> field, not value.
     * The authority typically stores the UUID while the value stores the display name.</p>
     * 
     * @param context           the DSpace context
     * @param item              the item to check
     * @param user              the user requesting access (returns false if null)
     * @param userMetadataFields list of metadata fields to check (e.g., ["dc.contributor.author"])
     * @return true if user's UUID matches any metadata authority
     * @throws SQLException if database error occurs
     */
    private boolean hasAccessByUserMetadataFields(Context context, Item item, EPerson user,
        List<String> userMetadataFields) throws SQLException {

        if (user == null || CollectionUtils.isEmpty(userMetadataFields)) {
            return false;
        }

        for (String userMetadataField : userMetadataFields) {
            if (anyMetadataHasAuthorityEqualsTo(item, userMetadataField, user.getID())) {
                log.debug("[CRIS-SECURITY] User metadata match: user={} ({}), item={}, field={}",
                          user.getEmail(), user.getID(), item.getID(), userMetadataField);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if user has access based on ownership of related items referenced in metadata.
     * 
     * <p><strong>How it works:</strong></p>
     * <ol>
     *   <li>Retrieves related items from configured metadata fields</li>
     *   <li>Each metadata authority is treated as an item UUID</li>
     *   <li>Checks if user is the owner of any of those related items</li>
     * </ol>
     *
     * 
     * @param context           the DSpace context
     * @param item              the item to check
     * @param user              the user requesting access (returns false if null)
     * @param itemMetadataFields list of metadata fields containing related item UUIDs
     * @return true if user owns any of the related items
     * @throws SQLException if database error occurs
     */
    private boolean hasAccessByItemMetadataFields(Context context, Item item, EPerson user,
        List<String> itemMetadataFields) throws SQLException {

        if (user == null || CollectionUtils.isEmpty(itemMetadataFields)) {
            return false;
        }

        List<Item> relatedItems = findRelatedItems(context, item, itemMetadataFields);

        for (Item relatedItem : relatedItems) {
            if (isOwner(user, relatedItem)) {
                log.debug("[CRIS-SECURITY] Item metadata match (transitive ownership):"
                          + " user={} ({}), item={}, relatedItem={}",
                          user.getEmail(), user.getID(), item.getID(), relatedItem.getID());
                return true;
            }
        }

        return false;
    }

    /**
     * Finds all related items referenced in the specified metadata fields.
     * 
     * <p>Extracts item UUIDs from metadata authorities and resolves them to Item objects.
     * Used by {@link #hasAccessByItemMetadataFields} to check transitive ownership.</p>
     * 
     * @param context           the DSpace context
     * @param item              the item to extract related items from
     * @param itemMetadataFields list of metadata fields containing item UUIDs in authorities
     * @return list of related items (empty list if no valid UUIDs found)
     * @throws SQLException if database error occurs during item lookup
     */
    private List<Item> findRelatedItems(Context context, Item item, List<String> itemMetadataFields)
        throws SQLException {

        List<Item> relatedItems = new ArrayList<>();

        if (CollectionUtils.isEmpty(itemMetadataFields)) {
            return relatedItems;
        }

        for (String itemMetadataField : itemMetadataFields) {
            List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, itemMetadataField);

            log.debug("[CRIS-SECURITY] Resolving related items: field={}, item={}, candidates={}",
                      itemMetadataField, item.getID(), metadataValues.size());

            for (MetadataValue metadataValue : metadataValues) {
                UUID relatedItemId = UUIDUtils.fromString(metadataValue.getAuthority());
                Item relatedItem = itemService.find(context, relatedItemId);
                if (relatedItem != null) {
                    log.debug("[CRIS-SECURITY] Resolved related item: field={}, authority={},"
                              + " relatedItem={}",
                              itemMetadataField, metadataValue.getAuthority(), relatedItemId);
                    relatedItems.add(relatedItem);
                }
            }
        }

        return relatedItems;

    }

    /**
     * Checks if user is a member of any of the specified groups.
     * 
     * <p><strong>Group Resolution:</strong></p>
     * <p>Groups can be specified as either names or UUIDs. The method:</p>
     * <ol>
     *   <li>Attempts to parse each group string as a UUID</li>
     *   <li>If valid UUID, looks up group by UUID</li>
     *   <li>If not a UUID, looks up group by name</li>
     *   <li>Checks user membership in each resolved group</li>
     * </ol>
     * 
     * @param context the DSpace context
     * @param user    the user to check (returns false if null)
     * @param groups  list of group names or UUIDs to check membership against
     * @return true if user is member of any specified group
     */
    private boolean hasAccessByGroup(Context context, EPerson user, List<String> groups) {
        if (CollectionUtils.isEmpty(groups)) {
            log.debug("[CRIS-SECURITY] Group check skipped: no groups configured");
            return false;
        }

        if (user == null) {
            log.debug("[CRIS-SECURITY] Group check skipped: anonymous user");
            return false;
        }

        log.debug("[CRIS-SECURITY] Checking group membership: user={} ({}), groupCount={}",
                  user.getEmail(), user.getID(), groups.size());

        for (String groupIdentifier : groups) {
            Group group = findGroupByNameOrUUID(context, groupIdentifier);
            if (group == null) {
                log.debug("[CRIS-SECURITY] Group not found: identifier={}", groupIdentifier);
                continue;
            }

            try {
                boolean isMember = groupService.isMember(context, user, group);
                log.debug("[CRIS-SECURITY] Group membership: user={}, group={} ({}), member={}",
                          user.getEmail(), group.getName(), group.getID(), isMember);
                if (isMember) {
                    return true;
                }
            } catch (SQLException e) {
                log.debug("[CRIS-SECURITY] Group membership check failed: user={}, group={},"
                          + " error={}", user.getEmail(), group.getName(), e.getMessage());
            }
        }

        return false;
    }

    /**
     * Resolves a group by name or UUID.
     * 
     * <p><strong>Resolution Strategy:</strong></p>
     * <ol>
     *   <li>Attempts to parse the string as a UUID</li>
     *   <li>If valid UUID, performs group lookup by UUID</li>
     *   <li>If not a UUID (returns null), performs lookup by group name</li>
     * </ol>
     * 
     * @param context the DSpace context
     * @param group   the group identifier (name or UUID string)
     * @return the resolved Group object, or null if not found
     */
    private Group findGroupByNameOrUUID(Context context, String group) {
        try {
            UUID groupUUID = UUIDUtils.fromString(group);
            return groupUUID != null ? groupService.find(context, groupUUID) : groupService.findByName(context, group);
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    /**
     * Checks if user is a member of the owning collection's submitter group.
     *
     * @param context the DSpace context
     * @param item    the item to check
     * @param user    the user to check membership for
     * @return true if user is in the collection's submitter group, false if collection is null
     * @throws SQLException if database error occurs during group membership check
     */
    private boolean isUserInSubmitterGroup(Context context, Item item, EPerson user) throws SQLException {
        Collection collection = item.getOwningCollection();
        if (collection == null) {
            return false;
        }
        return groupService.isMember(context, user, collection.getSubmitters());
    }

    /**
     * Checks if any metadata value for the specified field has an authority matching the given UUID.
     * 
     * <p><strong>Authority Matching:</strong></p>
     * <p>This method matches against the metadata <em>authority</em> field, not the value.
     * Authorities typically store UUIDs of controlled vocabulary entries, EPerson records,
     * or related items.</p>
     * 
     * @param item     the item to check
     * @param metadata the metadata field name (e.g., "dc.contributor.author")
     * @param uuid     the UUID to match against metadata authorities
     * @return true if any metadata value's authority equals the UUID
     */
    private boolean anyMetadataHasAuthorityEqualsTo(Item item, String metadata, UUID uuid) {
        List<MetadataValue> metadataValues = itemService.getMetadataByMetadataString(item, metadata);

        log.debug("[CRIS-SECURITY] Checking authorities: field={}, item={}, values={}, targetUUID={}",
                  metadata, item.getID(), metadataValues.size(), uuid);

        for (MetadataValue value : metadataValues) {
            String authority = value.getAuthority();
            if (uuid.toString().equals(authority)) {
                log.debug("[CRIS-SECURITY] Authority match: field={}, value='{}', authority={}",
                          metadata, value.getValue(), authority);
                return true;
            }
        }

        return false;
    }

}
