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
 * Interface to be extended for the configuration related to access item modes.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
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
