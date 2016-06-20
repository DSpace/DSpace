/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util.service;

import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Static utility class to manage configuration for exposure (hiding) of
 * certain Item metadata fields.
 *
 * This class answers the question, "is the user allowed to see this
 * metadata field?"  Any external interface (UI, OAI-PMH, etc) that
 * disseminates metadata should consult it before disseminating the value
 * of a metadata field.
 *
 * Since the MetadataExposure.isHidden() method gets called in a lot of inner
 * loops, it is important to implement it efficiently, in both time and
 * memory utilization.  It computes an answer without consuming ANY memory
 * (e.g. it does not build any temporary Strings) and in close to constant
 * time by use of hash tables.  Although most sites will only hide a few
 * fields, we can't predict what the usage will be so it's better to make it
 * scalable.
 *
 * Algorithm is as follows:
 *  1. If a Context is provided and it has a user who is Administrator,
 *     always grant access (return false).
 *  2. Return true if field is on the hidden list, false otherwise.
 *
 * The internal maps are populated from DSpace Configuration at the first
 * call, in case the properties are not available in the static context.
 *
 * Configuration Properties:
 *  ## hide a single metadata field
 *  #metadata.hide.SCHEMA.ELEMENT[.QUALIFIER] = true
 *  # example: dc.type
 *  metadata.hide.dc.type = true
 *  # example: dc.description.provenance
 *  metadata.hide.dc.description.provenance = true
 *
 * @author Larry Stone
 * @version $Revision: 3734 $
 */
public interface MetadataExposureService {

    /**
     * Returns whether the given metadata field should be exposed (visible). The metadata field is in the DSpace's DC notation: schema.element.qualifier
     *
     * @param context DSpace context
     * @param schema metadata field schema (namespace), e.g. "dc"
     * @param element metadata field element
     * @param qualifier metadata field qualifier
     *
     * @return true (hidden) or false (exposed)
     * @throws SQLException if database error
     */
    public boolean isHidden(Context context, String schema, String element, String qualifier)
            throws SQLException;
}
