/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

/**
 * Enumeration of ChecksumCheckResults containing constants for checksum
 * comparison result that must correspond to values in checksum_result table.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * 
 * 
 * @todo Refactor these as properties of ChecksumChecker?
 */
public class ChecksumCheckResults
{
    /**
     * Bitstream not found result.
     */
    public static final String BITSTREAM_NOT_FOUND = "BITSTREAM_NOT_FOUND";

    /**
     * BitstreamInfo not found result.
     */
    public static final String BITSTREAM_INFO_NOT_FOUND = "BITSTREAM_INFO_NOT_FOUND";

    /**
     * Bitstream not to be processed result.
     */
    public static final String BITSTREAM_NOT_PROCESSED = "BITSTREAM_NOT_PROCESSED";

    /**
     * Bitstream marked as deleted result.
     */
    public static final String BITSTREAM_MARKED_DELETED = "BITSTREAM_MARKED_DELETED";

    /**
     * Bitstream tallies with recorded checksum result.
     */
    public static final String CHECKSUM_MATCH = "CHECKSUM_MATCH";

    /**
     * Bitstream digest does not tally with recorded checksum result.
     */
    public static final String CHECKSUM_NO_MATCH = "CHECKSUM_NO_MATCH";

    /**
     * Previous checksum result not found.
     */
    public static final String CHECKSUM_PREV_NOT_FOUND = "CHECKSUM_PREV_NOT_FOUND";

    /**
     * No match between requested algorithm and previously used algorithm.
     */
    public static final String CHECKSUM_ALGORITHM_INVALID = "CHECKSUM_ALGORITHM_INVALID";
}
