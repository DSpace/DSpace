/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

/**
 * Component that receives BitstreamInfo results from a checker.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 */
public interface ChecksumResultsCollector
{
    /**
     * Collects results.
     * 
     * @param info
     *            BitstreamInfo representing the check results.
     */
    void collect(BitstreamInfo info);
}
