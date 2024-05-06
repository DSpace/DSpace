/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.coarnotify;

import java.util.List;
import java.util.Map;

/**
 * Simple bean to manage different COAR Notify configuration
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyConfigurationService {

    /**
     * Mapping the submission step process identifier with the configuration
     * (see configuration at coar-notify.xml)
     */
    private Map<String, List<NotifyPattern>> patterns;

    public Map<String, List<NotifyPattern>> getPatterns() {
        return patterns;
    }

    public void setPatterns(Map<String, List<NotifyPattern>> patterns) {
        this.patterns = patterns;
    }

}
