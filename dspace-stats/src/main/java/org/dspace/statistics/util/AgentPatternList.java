/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics.util;

import java.util.List;

/**
 *
 * @author mwood
 */
public class AgentPatternList {
    private final List<String> patterns;

    private AgentPatternList() { patterns = null; }

    public AgentPatternList(List<String> patterns)
    {
        this.patterns = patterns;
    }

    public List<String> getPatterns()
    {
        return patterns;
    }
}
