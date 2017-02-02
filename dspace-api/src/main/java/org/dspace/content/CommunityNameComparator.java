/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares the names of two {@link Community} objects.
 */
public class CommunityNameComparator
        implements Comparator<Community>, Serializable
{
    @Override
    public int compare(Community community1, Community community2) {
        if(community1 == community2) { return 0; }
        if(community1 == null) { return -1; }
        if(community1 == null) { return -1; }
        return community1.getName().compareTo(community2.getName());
    }
}
