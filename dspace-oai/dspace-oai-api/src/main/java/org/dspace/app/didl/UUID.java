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
package org.dspace.app.didl;

import java.io.Serializable;
/**
 * This class implements UUID version 4. The values for the various fields are
 * crypto random values set by the factory class UUIDFactory
 * 
 * Development of this code was part of the aDORe repository project by the
 * Research Library of the Los Alamos National Laboratory.
 * 
 * This code is based on the implementation of UUID version 4 (the one that
 * uses random/pseudo-random numbers by Ashraf Amrou of the Old Dominion University
 * (Aug 14, 2003)
 **/
public final class UUID implements Serializable
{
    private long hi;
    private long lo;

    /**
     * Construct a Version 4 UUID object from another UUID object
     * 
     * @param uuid
     *          the UUID to use as a base for the new UUID 
     **/
    public UUID(UUID uuid)
    {
        this.hi = uuid.hi;
        this.lo = uuid.lo;
    }
    
    /**
     * Construct a Version 4 UUID object form the two given long values.
     * These values are (pseudo)random numbers (best if crypto quality)
     * 
     * @param _hi
     *      first long value
     *      
     * @param _lo
     *      second long value
     *      
     **/
    public UUID(long _hi, long _lo)
    {
        this.hi = _hi;
        this.lo = _lo;
        // IETF variant (10)b
        lo &= 0x3FFFFFFFFFFFFFFFL; lo |= 0x8000000000000000L;
        // set multicast bit (so that it there is no chance it will clash
        // with other UUIDs generated based on real IEEE 802 addresses)
        lo |= 0x0000800000000000L;
        // version 4 (100)b: the one based on random/pseudo-random numbers
        hi &= 0xFFFFFFFFFFFF0FFFL; hi |= 0x0000000000004000L;
    }

    /**
     * Compare UUID objects 
     * 
     * @param obj
     *      the object to compare this UUID against
     * 
     * @return true or false
     **/
    public boolean equals(Object obj)
    {
        if(this == obj) // comparing to myself
            return true;
        if(obj instanceof UUID)
            return equals((UUID)obj);
        return false;
    }

    /**
     * Compare UUIDs 
     * 
     * @param uuid
     *      the UUID to compare this UUID against
     * 
     * @return true or false
     **/
    public boolean equals(UUID uuid)
    {
        return (hi == uuid.hi && lo == uuid.lo);
    }

    
    /**
     * Generate a hash for the UUID 
     * 
     * @return hash code for the UUID
     * 
     **/
    public int hashCode()
    {
        return new Long(hi ^ lo).hashCode();
    }
 
    
    /**
     * Obtain a string representation of the UUID object
     * 
     * @return the string representation of this UUID
     * 
     **/
    public String toString()
    {
        return (/**"urn:uuid:" + **/
                hexDigits(hi >> 32, 4)  // time_low: 4 hexOctet (8 hex digits)
                + "-" + 
                hexDigits(hi >> 16, 2)  // time_mid: 2 hexOctet (4 hex digits)
                + "-" +
                hexDigits(hi, 2)        // time_high_and_version: 2 hexOctet (4 hex digits)
                + "-" +
                hexDigits(lo >> 48, 2)  // clock_seq_and_reserved: 1 hexOctet (2 hex digits) & clock_seq_low: 1 hexOctet (2 hex digits)
                + "-" +
                hexDigits(lo, 6));      // node: 6 hexOctet (12 hex digits)
    }

    /**
     * Obtain the Hex value of a given number of least significant octets
     * from a long value as a String
     * 
     * @param lVal
     *          the long value to retrieve octets from
     * 
     * @param nHexOctets
     *          number of hex octets to return
     * 
     * @return hex value of least significant octets as a string 
     * 
     **/
    private static String hexDigits(long lVal, int nHexOctets) {
        long tmp = 1L << (nHexOctets * 2 * 4); // e.g., if nHexOctets is 2, tmp = (1 0000 0000 0000 0000)b & tmp - 1 = (1111 1111 1111 1111)b
        long result = lVal & (tmp - 1); // get ride of the uneeded most significant bits
        result = tmp | result; // make sure the digit at position (nDigits + 1) equals 1 (to preserve leading zeroes)
        return Long.toHexString(result).substring(1); // getride ot the digit at position nDigits + 1
    }
}