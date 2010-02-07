/**
 * $Id: $
 * $URL: $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A Spare v4 IPTable implementation that uses nested HashMaps
 * TO optimize IP Address matching over ranges of IP Addresses.
 *
 * @author: mdiggory at atmire.com
 */
public class IPTable {

    /* A lookup tree for IP Addresses and SubnetRanges */
    private HashMap<String, HashMap<String, HashMap<String, HashSet<String>>>> map =
            new HashMap<String, HashMap<String, HashMap<String, HashSet<String>>>>();

    /**
     * Can be full v4 IP, subnet or range string
     *
     * @param ip
     */
    public void add(String ip) throws IPFormatException {

        String[] start;

        String[] end;

        String[] range = ip.split("-");

        if (range.length >= 2) {

            start = range[0].trim().split("/")[0].split("\\.");
            end = range[1].trim().split("/")[0].split("\\.");

            if (start.length != 4 || end.length != 4)
                throw new IPFormatException(ip + " - Ranges need to be full IPv4 Addresses");

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


            HashMap<String, HashMap<String, HashSet<String>>> first = map.get(start[0]);

            if (first == null) {
                first = new HashMap<String, HashMap<String, HashSet<String>>>();
                map.put(start[0], first);
            }

            HashMap<String, HashSet<String>> second = first.get(start[1]);


            if (second == null) {
                second = new HashMap<String, HashSet<String>>();
                first.put(start[1], second);
            }

            HashSet<String> third = second.get(start[2]);

            if (third == null) {
                third = new HashSet<String>();
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

    public boolean contains(String ip) throws IPFormatException {

        String[] subnets = ip.split("\\.");

        if (subnets.length != 4)
            throw new IPFormatException("needs to be single IP Address");

        HashMap<String, HashMap<String, HashSet<String>>> first = map.get(subnets[0]);

        if (first == null) return false;

        HashMap<String, HashSet<String>> second = first.get(subnets[1]);

        if (second == null) return false;

        HashSet<String> third = second.get(subnets[2]);

        if (third == null) return false;

        return third.contains(subnets[3]) || third.contains("*");

    }

    /**
     * @return
     */
    public Set<String> toSet() {
        HashSet<String> set = new HashSet<String>();

        for (Map.Entry<String, HashMap<String, HashMap<String, HashSet<String>>>> first : map.entrySet()) {
            String firstString = first.getKey();
            HashMap<String, HashMap<String, HashSet<String>>> secondMap = first.getValue();

            for (Map.Entry<String, HashMap<String, HashSet<String>>> second : secondMap.entrySet()) {
                String secondString = second.getKey();
                HashMap<String, HashSet<String>> thirdMap = second.getValue();

                for (Map.Entry<String, HashSet<String>> third : thirdMap.entrySet()) {
                    String thirdString = third.getKey();
                    HashSet<String> fourthSet = third.getValue();

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
     * Exception Class to deal with IPFormat errors.
     */
    public class IPFormatException extends Exception {
        public IPFormatException(String s) {
            super(s);
        }
    }


}


