/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.PreviewContentRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.PreviewContent;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the PreviewContent in the DSpace API data model and the
 * REST data model
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@Component
public class PreviewContentConverter  implements DSpaceConverter<PreviewContent, PreviewContentRest>  {

    @Override
    public PreviewContentRest convert(PreviewContent modelObject, Projection projection) {
        PreviewContentRest previewContentRest = new PreviewContentRest();
        previewContentRest.setProjection(projection);
        previewContentRest.setId(modelObject.getID());
        previewContentRest.setContent(modelObject.getContent());
        previewContentRest.setName(modelObject.getName());
        previewContentRest.setSize(modelObject.getSize());
        previewContentRest.setDirectory(modelObject.isDirectory());
        return previewContentRest;
    }

    @Override
    public Class<PreviewContent> getModelClass() {
        return PreviewContent.class;
    }
}
