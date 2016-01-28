/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.api.database;

import com.lyncode.xoai.dataprovider.filter.ScopedFilter;

import java.util.List;

public interface DatabaseQueryResolver {
    DatabaseQuery buildQuery (List<ScopedFilter> filters, int offset, int length) throws DatabaseQueryException;
}
