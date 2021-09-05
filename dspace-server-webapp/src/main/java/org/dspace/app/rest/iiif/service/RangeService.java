/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.iiif.model.generator.RangeGenerator;
import org.dspace.app.rest.iiif.model.info.Info;
import org.dspace.app.rest.iiif.model.info.Range;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class RangeService extends AbstractResourceService {

    @Autowired
    CanvasService canvasService;

    public RangeService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Adds Ranges to manifest structures element.
     * Ranges are defined in the info.json file.
     * @param info
     * @param identifier
     */
    public List<RangeGenerator> getRanges(Info info, String identifier) {
        List<RangeGenerator> ranges = new ArrayList<>();
        List<Range> rangesFromConfig = utils.getRangesFromInfoObject(info);
        if (rangesFromConfig != null) {
            for (int pos = 0; pos < rangesFromConfig.size(); pos++) {
                ranges.add(getRange(identifier, rangesFromConfig.get(pos), pos));
            }
        }
        return ranges;
    }

    /**
     * Sets properties on the Range.
     * @param identifier DSpace item id
     * @param range range from info.json configuration
     * @param pos list position of the range
     */
    private RangeGenerator getRange(String identifier, Range range, int pos) {
        String id = IIIF_ENDPOINT + identifier + "/r" + pos;
        String label = range.getLabel();
        RangeGenerator rangeGenerator = new RangeGenerator();
        rangeGenerator.setIdentifier(id);
        rangeGenerator.setLabel(label);
        String startCanvas = utils.getCanvasId(range.getStart());
        rangeGenerator.addCanvas(canvasService.getRangeCanvasReference(identifier, startCanvas));
        return rangeGenerator;
    }
}
