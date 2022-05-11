/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.SubmissionDefinitionRest;
import org.dspace.app.rest.model.SubmissionSectionRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.submit.DataProcessingStep;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the SubmissionConfig in the DSpace API data
 * model and the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class SubmissionDefinitionConverter implements DSpaceConverter<SubmissionConfig, SubmissionDefinitionRest> {

    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(SubmissionDefinitionConverter.class);

    @Autowired
    private SubmissionSectionConverter panelConverter;

    @Autowired
    private RequestService requestService;

    // Must be loaded @Lazy, as ConverterService autowires all DSpaceConverter components
    @Lazy
    @Autowired
    private ConverterService converter;

    @Override
    public SubmissionDefinitionRest convert(SubmissionConfig obj, Projection projection) {
        SubmissionDefinitionRest sd = new SubmissionDefinitionRest();
        sd.setProjection(projection);
        sd.setName(obj.getSubmissionName());
        sd.setDefaultConf(obj.isDefaultConf());
        List<SubmissionSectionRest> panels = new LinkedList<SubmissionSectionRest>();
        for (int idx = 0; idx < obj.getNumberOfSteps(); idx++) {
            SubmissionStepConfig step = obj.getStep(idx);
            try {
                // only the step that process data must be included in the panels list
                if (DataProcessingStep.class.isAssignableFrom(Class.forName(step.getProcessingClassName()))) {
                    SubmissionSectionRest sp = converter.toRest(step, projection);
                    panels.add(sp);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "The submission configration is invalid the processing class for the step " + step.getId()
                                + " is not found",
                        e);
            }
        }

        HttpServletRequest request = requestService.getCurrentRequest().getHttpServletRequest();
        Context context = null;
        try {
            context = ContextUtil.obtainContext(request);
            List<Collection> collections = panelConverter.getSubmissionConfigReader()
                                                         .getCollectionsBySubmissionConfig(context,
                                                                                           obj.getSubmissionName());
            DSpaceConverter<Collection, CollectionRest> cc = converter.getConverter(Collection.class);
            List<CollectionRest> collectionsRest = collections.stream().map((collection) ->
                    cc.convert(collection, projection)).collect(Collectors.toList());
            sd.setCollections(collectionsRest);
        } catch (SQLException | IllegalStateException | SubmissionConfigReaderException e) {
            log.error(e.getMessage(), e);
        }
        sd.setPanels(panels);
        return sd;
    }

    @Override
    public Class<SubmissionConfig> getModelClass() {
        return SubmissionConfig.class;
    }
}
