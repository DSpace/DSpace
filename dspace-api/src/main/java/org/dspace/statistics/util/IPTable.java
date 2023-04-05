/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A Spare v4 IPTable implementation that uses nested HashMaps
 * to optimize IP address matching over ranges of IP addresses.
 *
 * @author mdiggory at atmire.com
 */
public class IPTable {
    private static final Logger log = LogManager.getLogger(IPTable.class);

    /* A lookup tree for IP addresses and SubnetRanges */
    private final Set<IPRange> ipRanges = new HashSet<>();

    /**
     * Internal class representing an IP range
     */
    static class IPRange {

        /* Lowest address in the range */
        private final long ipLo;

        /* Highest address in the range */
        private final long ipHi;

        IPRange(long ipLo, long ipHi) {
            this.ipLo = ipLo;
            this.ipHi = ipHi;
        }

        /**
         * Get the lowest address in the range
         * @return  the lowest address as a long integer
         */
        public long getIpLo() {
            return ipLo;
        }

        /**
         * Get the highest address in the range
         * @return  the highest address as a long integer
         */
        public long getIpHi() {
            return ipHi;
        }
    }

    /**
     * Can be full v4 IP, subnet or range string.
     * <ul>
     *   <li>A full address is a complete dotted-quad:  {@code "1.2.3.4".}
     *   <li>A subnet is a dotted-triplet:  {@code "1.2.3"}.  It means an entire
     *       Class C subnet:  "1.2.3.0-1.2.3.255".
     *   <li>A range is two dotted-quad addresses separated by hyphen:
     *       {@code "1.2.3.4-1.2.3.14"}.  Only the final octet may be different.
     * </ul>
     *
     * Any attempt at CIDR notation is ignored.
     *
     * @param ip IP address(es)
     * @throws IPFormatException Exception Class to deal with IPFormat errors.
     */
    public void add(String ip) throws IPFormatException {

        String start;

        String end;

        String[] range = ip.split("-");

        if (range.length == 2) {

            start = range[0].trim();
            end = range[1].trim();

            try {
                long ipLo = ipToLong(InetAddress.getByName(start));
                long ipHi = ipToLong(InetAddress.getByName(end));
                ipRanges.add(new IPRange(ipLo, ipHi));
                return;
            } catch (UnknownHostException e) {
                throw new IPFormatException(ip + " - Range format should be similar to 1.2.3.0-1.2.3.255");
            }

        } else {
            // Convert implicit ranges to netmask format
            //  192       -> 192.0.0.0/8
            //  192.168   -> 192.168.0.0/16
            //  192.168.1 -> 192.168.1.0/24
            int periods = StringUtils.countMatches(ip, '.');
            if (periods < 3) {
                ip = StringUtils.join(ip, StringUtils.repeat(".0", 4 - periods - 1), "/", (periods + 1) * 8);
            }

            if (ip.contains("/")) {
                String[] parts = ip.split("/");
                try {
                    long ipLong = ipToLong(InetAddress.getByName(parts[0]));
                    long mask = (long) Math.pow(2, 32 - Integer.parseInt(parts[1]));
                    long ipLo = (ipLong / mask) * mask;
                    long ipHi = (( (ipLong / mask) + 1) * mask) - 1;
                    ipRanges.add(new IPRange(ipLo, ipHi));
                    return;
                } catch (Exception e) {
                    throw new IPFormatException(ip + " - Range format should be similar to 172.16.0.0/12");
                }
            } else {
                try {
                    long ipLo = ipToLong(InetAddress.getByName(ip));
                    ipRanges.add(new IPRange(ipLo, ipLo));
                    return;
                } catch (UnknownHostException e) {
                    throw new IPFormatException(ip + " - IP address format should be similar to 1.2.3.14");
                }
            }
        }
    }

    /**
     * Convert an IP address to a long integer
     * @param ip    the IP address
     * @return
     */
    public static long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }

    /**
     * Convert a long integer into an IP address string
     * @param ip    the IP address as a long integer
     * @return
     */
    public static String longToIp(long ip) {
        long part = ip;
        String[] parts = new String[4];
        for (int i = 0; i < 4; i++) {
            long octet = part & 0xff;
            parts[3 - i] = String.valueOf(octet);
            part = part / 256;
        }

        return parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
    }

    /**
     * Check whether a given address is contained in this netblock.
     *
     * @param ip the address to be tested
     * @return true if {@code ip} is within this table's limits.  Returns false
     *         if {@code ip} looks like an IPv6 address.
     * @throws IPFormatException Exception Class to deal with IPFormat errors.
     */
    public boolean contains(String ip) throws IPFormatException {

        try {
            long ipToTest = ipToLong(InetAddress.getByName(ip));
            return ipRanges.stream()
                    .anyMatch(ipRange -> (ipToTest >= ipRange.getIpLo() && ipToTest <= ipRange.getIpHi()));
        } catch (UnknownHostException e) {
            throw new IPFormatException("ip not valid");
        }
    }

    /**
     * Convert to a Set. This set contains all IPs in the range
     *
     * @return this table's content as a Set
     */
    public Set<String> toSet() {
        HashSet<String> set = new HashSet<>();

        Iterator<IPRange> ipRangeIterator = ipRanges.iterator();
        while (ipRangeIterator.hasNext()) {
            IPRange ipRange = ipRangeIterator.next();
            long ipLo = ipRange.getIpLo();
            long ipHi = ipRange.getIpHi();
            for (long ip = ipLo; ip <= ipHi; ip++) {
                set.add(longToIp(ip));
            }
        }

        return set;
    }

    /**
     * Return whether IPTable is empty (having no entries)
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return ipRanges.isEmpty();
    }

    /**
     * Exception Class to deal with IPFormat errors.
     */
    public static class IPFormatException extends Exception {
        public IPFormatException(String s) {
            super(s);
        }
    }

    /**
     * Represent this IP table as a string
     * @return  a string containing all IP ranges in this IP table
     */
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<IPRange> ipRangeIterator = ipRanges.iterator();
        while (ipRangeIterator.hasNext()) {
            IPRange ipRange = ipRangeIterator.next();
            stringBuilder.append(longToIp(ipRange.getIpLo()))
                    .append("-")
                    .append(longToIp(ipRange.getIpHi()));
            if (ipRangeIterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }
        return stringBuilder.toString();
    }
}
