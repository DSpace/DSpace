/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private final Map<String, Map<String, Map<String, Set<String>>>> map
            = new HashMap<>();

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

        String[] start;

        String[] end;

        String[] range = ip.split("-");

        if (range.length >= 2) {

            start = range[0].trim().split("/")[0].split("\\.");
            end = range[1].trim().split("/")[0].split("\\.");

            if (start.length != 4 || end.length != 4) {
                throw new IPFormatException(ip + " - Ranges need to be full IPv4 Addresses");
            }

            if (!(start[0].equals(end[0]) && start[1].equals(end[1]) && start[2].equals(end[2]))) {
                throw new IPFormatException(ip + " - Ranges can only be across the last subnet x.y.z.0 - x.y.z.254");
            }

        } else {
            //need to ignore CIDR notation for the moment.
            //ip = ip.split("\\/")[0];

            String[] subnets = ip.split("\\.");

            if (subnets.length < 3) {
                throw new IPFormatException(ip + " - require at least three subnet places (255.255.255.0");
            }

            start = subnets;
            end = subnets;
        }

        if (start.length >= 3) {
            Map<String, Map<String, Set<String>>> first = map.get(start[0]);

            if (first == null) {
                first = new HashMap<>();
                map.put(start[0], first);
            }

            Map<String, Set<String>> second = first.get(start[1]);

            if (second == null) {
                second = new HashMap<>();
                first.put(start[1], second);
            }

            Set<String> third = second.get(start[2]);

            if (third == null) {
                third = new HashSet<>();
                second.put(start[2], third);
            }

            //now populate fourth place (* or value 0-254);

            if (start.length == 3) {
                third.add("*");
            }

            if (third.contains("*")) {
                return;
            }

            if (start.length >= 4) {
                int s = Integer.valueOf(start[3]);
                int e = Integer.valueOf(end[3]);
                for (int i = s; i <= e; i++) {
                    third.add(String.valueOf(i));
                }
            }
        }
    }

    /**
     * Check whether a given address is contained in this netblock.
     *
     * @param ip the address to be tested
     * @return true if {@code ip} is within this table's limits.  Returns false
     *         if {@link ip} looks like an IPv6 address.
     * @throws IPFormatException Exception Class to deal with IPFormat errors.
     */
    public boolean contains(String ip) throws IPFormatException {

        String[] subnets = ip.split("\\.");

        // Does it look like IPv6?
        if (subnets.length > 4 || ip.contains("::")) {
            log.warn("Address {} assumed not to match.  IPv6 is not implemented.", ip);
            return false;
        }

        // Does it look like a subnet?
        if (subnets.length < 4) {
            throw new IPFormatException("needs to be a single IP address");
        }

        Map<String, Map<String, Set<String>>> first = map.get(subnets[0]);

        if (first == null) {
            return false;
        }

        Map<String, Set<String>> second = first.get(subnets[1]);

        if (second == null) {
            return false;
        }

        Set<String> third = second.get(subnets[2]);

        if (third == null) {
            return false;
        }

        return third.contains(subnets[3]) || third.contains("*");

    }

    /**
     * Convert to a Set.
     *
     * @return this table's content as a Set
     */
    public Set<String> toSet() {
        HashSet<String> set = new HashSet<>();

        for (Map.Entry<String, Map<String, Map<String, Set<String>>>> first : map.entrySet()) {
            String firstString = first.getKey();
            Map<String, Map<String, Set<String>>> secondMap = first.getValue();

            for (Map.Entry<String, Map<String, Set<String>>> second : secondMap.entrySet()) {
                String secondString = second.getKey();
                Map<String, Set<String>> thirdMap = second.getValue();

                for (Map.Entry<String, Set<String>> third : thirdMap.entrySet()) {
                    String thirdString = third.getKey();
                    Set<String> fourthSet = third.getValue();

                    if (fourthSet.contains("*")) {
                        set.add(firstString + "." + secondString + "." + thirdString);
                    } else {
                        for (String fourth : fourthSet) {
                            set.add(firstString + "." + secondString + "." + thirdString + "." + fourth);
                        }
                    }

                }
            }
        }

        return set;
    }

    /**
     * Return whether IPTable is empty (having no entries)
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Exception Class to deal with IPFormat errors.
     */
    public static class IPFormatException extends Exception {
        public IPFormatException(String s) {
            super(s);
        }
    }


}
