/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;

public interface FieldResolver {
    int getFieldID(Context context, String field) throws InvalidMetadataFieldException, SQLException;
}
