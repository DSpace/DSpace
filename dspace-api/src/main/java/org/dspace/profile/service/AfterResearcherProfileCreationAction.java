/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.profile.service;

import java.sql.SQLException;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.profile.ResearcherProfile;

/**
 * Interface to mark classes that allow to perform additional logic on created
 * researcher profile.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface AfterResearcherProfileCreationAction {

    /**
     * Perform some actions on the given researcher profile and returns the updated
     * profile.
     *
     * @param  context           the DSpace context
     * @param  researcherProfile the created researcher profile
     * @param  owner             the EPerson that is owner of the given profile
     * @throws SQLException      if a SQL error occurs
     */
    void perform(Context context, ResearcherProfile researcherProfile, EPerson owner) throws SQLException;
}
