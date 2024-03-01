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
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.springframework.beans.factory.InitializingBean;

/**
 * An implementation for Submission Config service
 *
 * @author paulo.graca at fccn.pt
 */
public class SubmissionConfigServiceImpl implements SubmissionConfigService, InitializingBean {

    protected SubmissionConfigReader submissionConfigReader;

    public SubmissionConfigServiceImpl () throws SubmissionConfigReaderException {
        submissionConfigReader = new SubmissionConfigReader();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        submissionConfigReader.reload();
    }

    @Override
    public void reload() throws SubmissionConfigReaderException {
        submissionConfigReader.reload();
    }

    @Override
    public String getDefaultSubmissionConfigName() {
        return submissionConfigReader.getDefaultSubmissionConfigName();
    }

    @Override
    public List<SubmissionConfig> getAllSubmissionConfigs(Integer limit, Integer offset) {
        return submissionConfigReader.getAllSubmissionConfigs(limit, offset);
    }

    @Override
    public int countSubmissionConfigs() {
        return submissionConfigReader.countSubmissionConfigs();
    }

    @Override
    public SubmissionConfig getSubmissionConfigByCollection(Collection collection) {
        return submissionConfigReader.getSubmissionConfigByCollection(collection);
    }

    @Override
    public SubmissionConfig getSubmissionConfigByName(String submitName) {
        return submissionConfigReader.getSubmissionConfigByName(submitName);
    }

    @Override
    public SubmissionStepConfig getStepConfig(String stepID) throws SubmissionConfigReaderException {
        return submissionConfigReader.getStepConfig(stepID);
    }

    @Override
    public List<Collection> getCollectionsBySubmissionConfig(Context context, String submitName)
            throws IllegalStateException, SQLException {
        return submissionConfigReader.getCollectionsBySubmissionConfig(context, submitName);
    }

}
