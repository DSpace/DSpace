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


