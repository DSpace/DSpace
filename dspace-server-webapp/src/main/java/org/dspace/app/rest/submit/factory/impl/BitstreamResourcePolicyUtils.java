package org.dspace.app.rest.submit.factory.impl;

import org.dspace.app.rest.model.UploadBitstreamAccessConditionDTO;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.submit.model.UploadConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;

public class BitstreamResourcePolicyUtils {

    private static final Logger log = LoggerFactory.getLogger(BitstreamResourcePolicyUtils.class);

    public static void findApplyResourcePolicy(Context context, Iterator<UploadConfiguration> uploadConfigs,
                                         Bitstream b, UploadBitstreamAccessConditionDTO newAccessCondition)
            throws SQLException, AuthorizeException {
        String name = newAccessCondition.getName();
        String description = newAccessCondition.getDescription();

        Date startDate = newAccessCondition.getStartDate();
        Date endDate = newAccessCondition.getEndDate();

        findApplyResourcePolicy(context, uploadConfigs, b, name, description, startDate, endDate);
    }

    public static void findApplyResourcePolicy(Context context, Iterator<UploadConfiguration> uploadConfigs,
                                               Bitstream b, String name, String description,
                                               Date startDate, Date endDate)
            throws SQLException, AuthorizeException {
        while (uploadConfigs
                .hasNext()) {
            UploadConfiguration uploadConfiguration = uploadConfigs.next();
            for (AccessConditionOption aco : uploadConfiguration.getOptions()) {
                if (aco.getName().equalsIgnoreCase(name)) {
                    aco.createResourcePolicy(context, b, name, description, startDate, endDate);
                    return;
                }
            }
        }
        log.warn("no AccessCondition found or applied for bitstream " + b.getID() +
                " with AccessCondition " + name);
    }
}
