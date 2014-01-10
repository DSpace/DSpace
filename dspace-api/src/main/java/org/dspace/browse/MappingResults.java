/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.util.List;

interface MappingResults
{
    List<Integer> getAddedDistinctIds();
    List<Integer> getRetainedDistinctIds();
    List<Integer> getRemovedDistinctIds();
}
