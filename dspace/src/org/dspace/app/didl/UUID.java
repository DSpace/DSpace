/******************************************************************************
 * DSPACE DIDL MODULE UUID GENERATOR
 * AUTHOR
 *        Los Alamos National Laboratory
 *        Research Library
 *        Digital Library Research & Prototyping Team
 *        Henry Jerez
 *        2004, 2005
 *CONTACT
 *   proto@gws.lanl.gov
 *VERSION
 *  Beta 1
 *  date 07/26/2005
 * ACKNOWLEDGMENT
 *    Development of this code is part of the aDORe repository project by the Research Library of the Los Alamos National Laboratory.
* BASED ON:
 * Implementation of UUID version 4 (the one that uses random/pseudo-random
 * numbers) 
 * By: Ashraf Amrou
 * Old Dominion University
 * Aug 14, 2003
 *****************************************************************************/
package org.dspace.app.didl;

import java.io.*;
/**
 * This class implements UUID version 4
 * The values for the various fields are crypto random values set by 
 * the factory class UUIDFactory
 **/
public final class UUID implements java.io.Serializable {
    private long hi;
    private long lo;
    ///////////////////////////////////////////////////
    /**
     * Construct a Version 4 UUID object form another UUID object
     **/
    public UUID(UUID uuid) {
        this.hi = uuid.hi;
        this.lo = uuid.lo;
    }
    ///////////////////////////////////////////////////
    /**
     * Construct a Version 4 UUID object form the two given long values.
     * These values are (pseudo)random numbers (best if crypto quality)
     **/
    public UUID(long _hi, long _lo) {
        this.hi = _hi;
        this.lo = _lo;
        // IETF variant (10)b
        lo &= 0x3FFFFFFFFFFFFFFFL; lo |= 0x8000000000000000L;
        // set multicast bit (so that it there is no chance it will clash with other UUIDs generated based on real IEEE 802 addresses)
        lo |= 0x0000800000000000L;
        // version 4 (100)b: the one based on random/pseudo-random numbers
        hi &= 0xFFFFFFFFFFFF0FFFL; hi |= 0x0000000000004000L;
    }
    ///////////////////////////////////////////////////
    /**
     * Returns true if equal
     **/
    public boolean equals( Object obj ) {
        if(this == obj) // comparing to myself
            return true;
        if(obj instanceof UUID)
            return equals((UUID)obj);
        return false;
    }
    ///////////////////////////////////////////////////
    /**
     * Returns true if equal
     **/
    public boolean equals(UUID uuid) {
        return (hi == uuid.hi && lo == uuid.lo);
    }
    ///////////////////////////////////////////////////
    public int hashCode(){
        return new Long(hi ^ lo).hashCode();
        //return new Long(((((long)(new Long(hi).hashCode())) << 32) + ((long)(new Long(lo).hashCode())))).hashCode();
    }
    ///////////////////////////////////////////////////
    /**
     * Returns the string representation of this UUID
     **/
    public String toString() {
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
    ///////////////////////////////////////////////////
    /**
     * Returns the Hex value of the nHexOctets lest significant octets from the long value lVal as a String
     **/
    private static String hexDigits(long lVal, int nHexOctets) {
        long tmp = 1L << (nHexOctets * 2 * 4); // e.g., if nHexOctets is 2, tmp = (1 0000 0000 0000 0000)b & tmp - 1 = (1111 1111 1111 1111)b
        long result = lVal & (tmp - 1); // get ride of the uneeded most significant bits
        result = tmp | result; // make sure the digit at position (nDigits + 1) equals 1 (to preserve leading zeroes)
        return Long.toHexString(result).substring(1); // getride ot the digit at position nDigits + 1
    }
    ///////////////////////////////////////////////////
}

