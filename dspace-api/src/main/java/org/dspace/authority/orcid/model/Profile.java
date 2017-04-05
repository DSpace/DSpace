/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.authority.orcid.model;

import java.util.ArrayList;
import java.util.List;

public class Profile extends Bio
{
    protected List<String> affiliations;

    public Profile() {
        super();

        this.affiliations = new ArrayList<String>();
    }

    public List<String> getAffiliations() {
        return this.affiliations;
    }

    public void addAffiliation(String affiliation) {
        this.affiliations.add(affiliation);
    }

    @Override
    public String toString() {
        return "Profile{" + super.toString() +
               ", affiliations='" + affiliations + "'" +
               "}";
    }
}
