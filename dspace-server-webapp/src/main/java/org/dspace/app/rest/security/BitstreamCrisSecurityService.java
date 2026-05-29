/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.security.AccessItemMode;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Service to evaluate custom CRIS-based security policies for bitstream access.
 * 
 * <p>This service provides an additional layer of authorization on top of the standard
 * DSpace ACL system. It allows configuration of custom policies that grant download
 * permissions based on user roles and relationships to items (e.g., submitter, author, editor)
 * with optional restrictions on which bundles the policies apply to.</p>
 * 
 * <h2>How It Works</h2>
 * <p>When a user attempts to download a bitstream:</p>
 * <ol>
 *   <li>The service checks if the bitstream belongs to an Item with a configured entity type</li>
 *   <li>For each configured access mode for that entity type:
 *     <ul>
 *       <li>Check if the user satisfies the security criteria (SUBMITTER, CUSTOM, etc.)</li>
 *       <li>Check if the bitstream is in an allowed bundle (if bundle restrictions are configured)</li>
 *     </ul>
 *   </li>
 *   <li>If both conditions are met, access is granted</li>
 * </ol>
 * 
 * <h2>Configuration</h2>
 * <p>Configured via {@code dspace/config/spring/api/bitstream-access-modes.xml}:</p>
 * <ul>
 *   <li>{@code bitstreamAccessModesMap} - Maps entity types to list of access modes</li>
 *   <li>{@code allowedBundlesForBitstreamAccess} - Optional bundle restrictions per access mode</li>
 * </ul>
 * 
 * <h2>Bundle Restriction Behavior</h2>
 * <ul>
 *   <li><strong>No entry in map</strong> - Access granted for all bundles</li>
 *   <li><strong>Empty list</strong> - Access denied for all bundles</li>
 *   <li><strong>List with bundle names</strong> - Access granted only for specified bundles</li>
 * </ul>
 * 
 * @author Stefano Maffei (stefano.maffei at 4science.it)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @see CrisSecurityService
 * @see EditItemMode
 */
@Component
public class BitstreamCrisSecurityService {

    /**
     * Service for evaluating CRIS security policies
     */
    @Autowired
    private CrisSecurityService crisSecurityService;

    /**
     * Service for Item operations
     */
    @Autowired
    private ItemService itemService;

    /**
     * Service for Bitstream operations
     */
    @Autowired
    private BitstreamService bitstreamService;

    /**
     * Map of entity types to their configured access modes.
     * Injected from {@code bitstream-access-modes.xml}.
     * Key: Entity type (e.g., "Publication")
     * Value: List of EditItemMode defining who can access bitstreams
     */
    @Autowired
    @Qualifier("bitstreamAccessModesMap")
    private Map<String, List<EditItemMode>> bitstreamAccessModesMap;

    /**
     * Map of access modes to allowed bundle names.
     * Injected from {@code bitstream-access-modes.xml}.
     * Key: EditItemMode instance
     * Value: List of bundle names (e.g., ["ORIGINAL", "LICENSE"])
     *        - null/missing = all bundles allowed
     *        - empty list = no bundles allowed
     */
    @Autowired
    @Qualifier("allowedBundlesForBitstreamAccess")
    private Map<EditItemMode, List<String>> allowedBundles;

    /**
     * Determines if a user is allowed to access a bitstream based on CRIS security policies.
     * 
     * <p>This method evaluates custom security rules configured for the parent item's entity type.
     * It checks if the user satisfies any of the configured access modes (e.g., SUBMITTER, CUSTOM)
     * and if the bitstream is in an allowed bundle.</p>
     * 
     * <p>The method performs the following checks:</p>
     * <ol>
     *   <li>Verify the bitstream belongs to an Item (not a Community/Collection)</li>
     *   <li>Get the Item's entity type (e.g., "Publication")</li>
     *   <li>For each configured access mode for that entity type:
     *     <ul>
     *       <li>Check if user has access according to CRIS security rules</li>
     *       <li>Check if bitstream is in an allowed bundle (if bundle restrictions exist)</li>
     *     </ul>
     *   </li>
     * </ol>
     * 
     * @param context the DSpace context
     * @param ePerson the user requesting access (can be null for anonymous)
     * @param bit the bitstream being accessed
     * @return true if access is allowed by CRIS security policies, false otherwise
     * @throws SQLException if database error occurs
     */
    public boolean isBitstreamAccessAllowedByCrisSecurity(Context context, EPerson ePerson, Bitstream bit)
            throws SQLException {
        DSpaceObject pdso = bitstreamService.getParentObject(context, bit);
        if (pdso != null && pdso instanceof Item) {
            Item item = (Item) pdso;
            String entityType = itemService.getEntityType(item);
            if (StringUtils.isNotEmpty(entityType)) {
                for (AccessItemMode accessMode : bitstreamAccessModesMap.getOrDefault(entityType,
                        Collections.emptyList())) {
                    if (accessMode != null &&
                        crisSecurityService.hasAccess(context, item, ePerson, accessMode) &&
                        isBundleNotRestricted(accessMode, bit)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if a bitstream is in an allowed bundle according to the access mode configuration.
     * 
     * <p>Bundle restriction behavior:</p>
     * <ul>
     *   <li><strong>null (no entry in map)</strong> - All bundles are allowed</li>
     *   <li><strong>Empty list</strong> - No bundles are allowed</li>
     *   <li><strong>List with values</strong> - Only specified bundles are allowed</li>
     * </ul>
     * 
     * @param accessItemMode the access mode being evaluated
     * @param bit the bitstream to check
     * @return true if the bitstream is in an allowed bundle, false otherwise
     * @throws SQLException if database error occurs
     */
    private boolean isBundleNotRestricted(AccessItemMode accessItemMode, Bitstream bit) throws SQLException {
        if (allowedBundles.get(accessItemMode) == null) {
            // no restriction was specified - allow all bundles
            return true;
        }
        List<Bundle> bitstreamBundles = bit.getBundles();
        return bitstreamBundles.stream().anyMatch(bundle -> {
            return allowedBundles.get(accessItemMode).contains(bundle.getName());
        });
    }
}
