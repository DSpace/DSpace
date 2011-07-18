/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
        {
            return true;
        }
        if(obj instanceof UUID)
        {
            UUID uuid = (UUID)obj;
            return (hi == uuid.hi && lo == uuid.lo);
        }
        return false;
    }
    
    /**
     * Generate a hash for the UUID 
     * 
     * @return hash code for the UUID
     * 
     **/
    public int hashCode()
    {
        return Long.valueOf(hi ^ lo).hashCode();
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