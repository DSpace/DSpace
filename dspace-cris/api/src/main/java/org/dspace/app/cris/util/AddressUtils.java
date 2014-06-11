/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

public class AddressUtils
{

    public static long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }
    
    public static boolean checkIsInRange(String from, String to, String ip) throws UnknownHostException {
        
        long ipLo = ipToLong(InetAddress.getByName(from));
        long ipHi = ipToLong(InetAddress.getByName(to));
        long ipToTest = ipToLong(InetAddress.getByName(ip));

        return ipToTest >= ipLo && ipToTest <= ipHi;
        
    }

    public static boolean checkIsInPattern(String from, String ip) {
        return Pattern.matches(from, ip);
    }
}
