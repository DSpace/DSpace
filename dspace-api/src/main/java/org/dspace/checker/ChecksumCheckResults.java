/*
 * Copyright (c) 2004-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
