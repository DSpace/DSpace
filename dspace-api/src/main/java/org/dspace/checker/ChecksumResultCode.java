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
 * @author Kevin Van de Velde
 * 
 * 
 */
public enum ChecksumResultCode {
    BITSTREAM_NOT_FOUND,
    BITSTREAM_INFO_NOT_FOUND,
    BITSTREAM_NOT_PROCESSED,
    BITSTREAM_MARKED_DELETED,
    CHECKSUM_MATCH,
    CHECKSUM_NO_MATCH,
    CHECKSUM_PREV_NOT_FOUND,
    CHECKSUM_ALGORITHM_INVALID
}
