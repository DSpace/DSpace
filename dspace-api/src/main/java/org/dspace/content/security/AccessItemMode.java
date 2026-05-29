/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security;

import java.util.List;

import org.dspace.content.logic.Filter;

/**
 * Interface defining configurable access control modes for items in DSpace.
 * 
 * <p><strong>Purpose:</strong></p>
 * <p>AccessItemMode provides a flexible, declarative way to define fine-grained access control
 * policies for items and bitstreams that work alongside DSpace's standard ACL system. It enables
 * role-based access control through configuration rather than code.</p>
 * 
 * <p><strong>Where AccessItemMode is Used:</strong></p>
 * <ol>
 *   <li><strong>Edit Item Access Control</strong> - Determines who can edit published items in
 *       different modes (e.g., "FULL" admin mode vs "OWNER" self-service mode)</li>
 *   <li><strong>Bitstream Download Control</strong> - Grants download access to embargoed/restricted
 *       files for submitters, authors, or editors beyond standard READ policies</li>
 *   <li><strong>Custom Security Evaluation</strong> - Enables metadata-based access control where
 *       users listed in specific metadata fields (e.g., dc.contributor.author) gain access</li>
 * </ol>
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @see org.dspace.content.edit.EditItemMode
 * @see CrisSecurity
 * @see org.dspace.content.security.service.CrisSecurityService
 * @see org.dspace.app.rest.security.BitstreamCrisSecurityService
 *
 */
public interface AccessItemMode {

    /**
     * Returns the configured securities.
     *
     * @return the configured securities
     */
    public List<CrisSecurity> getSecurities();

    /**
     * Returns the configured group metadata fields for the CUSTOM security.
     *
     * @return the metadata fields list
     */
    public List<String> getGroupMetadataFields();

    /**
     * Returns the configured user metadata fields for the CUSTOM security.
     *
     * @return the metadata fields list
     */
    public List<String> getUserMetadataFields();

    /**
     * Returns the configured item metadata fields for the CUSTOM security.
     *
     * @return the metadata fields list
     */
    public List<String> getItemMetadataFields();

    /**
     * Returns the configured groups name/uuid for the GROUP security.
     * @return the group list
     */
    public List<String> getGroups();

    public Filter getAdditionalFilter();
}
