package org.dspace.browse;

import java.util.List;

interface MappingResults
{
    List<Integer> getAddedDistinctIds();
    List<Integer> getRetainedDistinctIds();
    List<Integer> getRemovedDistinctIds();
}
