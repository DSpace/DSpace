/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.Set;

public class UATable {

    /* A lookup tree for IP Addresses and SubnetRanges */
//    private HashSet<Pattern> set = new HashSet<Pattern>();
    private HashSet<String> set = new HashSet<String>();


    /**
     * Can be full v4 IP, subnet or range string
     *
     * @param ip
     */
    public void add(String ua) {
	try {
//		Pattern pattern = Pattern.compile(ua);
//		set.add(pattern);
		set.add(ua);
	} finally {
	}
    }

    public boolean contains(String ua) {
//	for (Pattern pattern : set) {
//		if (pattern.matcher(ua).matches())
//			return true;
//	}
//	return false;
	return set.contains(ua);
    }

}


