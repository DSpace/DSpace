/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Java Bean to expose the section license during in progress submission.
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 */
public class DataDetectDuplicate implements SectionData {
    @JsonUnwrapped
    private Map<UUID, DuplicateMatch> matches = null;

    public Map<UUID, DuplicateMatch> getMatches() {
        return matches;
    }

    public void setMatches(Map<UUID, DuplicateMatch> matches) {
        this.matches = matches;
    }

}