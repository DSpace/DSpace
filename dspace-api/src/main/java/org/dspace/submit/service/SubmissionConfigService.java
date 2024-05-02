/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.core.Context;

/**
 * Item Submission Configuration Service
 * enables interaction with a submission config like
 * getting a config by a collection name or handle
 * as also retrieving submission configuration steps
 *
 * @author paulo.graca at fccn.pt
 */
public interface SubmissionConfigService {

    public void reload() throws SubmissionConfigReaderException;

    public String getDefaultSubmissionConfigName();

    public List<SubmissionConfig> getAllSubmissionConfigs(Integer limit, Integer offset);

    public int countSubmissionConfigs();

    public SubmissionConfig getSubmissionConfigByCollection(Collection collection);

    public SubmissionConfig getSubmissionConfigByName(String submitName);

    public SubmissionStepConfig getStepConfig(String stepID)
            throws SubmissionConfigReaderException;

    public List<Collection> getCollectionsBySubmissionConfig(Context context, String submitName)
            throws IllegalStateException, SQLException, SubmissionConfigReaderException;

}
