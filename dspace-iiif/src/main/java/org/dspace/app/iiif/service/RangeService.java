/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.iiif.model.generator.CanvasGenerator;
import org.dspace.app.iiif.model.generator.RangeGenerator;
import org.dspace.app.iiif.service.utils.IIIFUtils;
import org.dspace.core.I18nUtil;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This service provides methods for creating a {@code Range}. There should be a single instance of this service
 * per request. The {@code @RequestScope} provides a single instance created and available during complete lifecycle
 * of the HTTP request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@RequestScope
@Component
public class RangeService extends AbstractResourceService {

    @Autowired
    CanvasService canvasService;

    private Map<String, RangeGenerator> tocRanges = new LinkedHashMap<String, RangeGenerator>();
    private RangeGenerator lastRange;
    private RangeGenerator root;


    public RangeService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Returns the current range to which canvases should be added. The "last"
     * range is updated every time a new toc entry is processed.
     * @return
     */
    public RangeGenerator getLastRange() {
        return this.lastRange;
    }

    /**
     * Get the root range generator. This will contain table of contents entries.
     * @return
     */
    public RangeGenerator getRootRange() {
        return root;
    }

    /**
     * Sets the root range generator to which sub-ranges will be added.
     * @param manifestId id of the manifest to which ranges will be added.
     */
    public void setInitialRange(String manifestId) {
        root = getRootGenerator(manifestId);
    }


    /**
     * Creates generator for the root table of contents range.
     * @param manifestId manifest id
     * @return root range generator
     */
    private RangeGenerator getRootGenerator(String manifestId) {
        RangeGenerator root = new RangeGenerator(this);
        // This hint is required.
        root.addViewingHint("top");
        root.setLabel(I18nUtil.getMessage("iiif.toc.root-label"));
        root.setIdentifier(manifestId + "/range/r0");
        return root;
    }

    /**
     * Gets the current ranges.
     * @return map of toc ranges.
     */
    public Map<String, RangeGenerator> getTocRanges() {
        return this.tocRanges;
    }

    /**
     * When the bitstream has a toc metadata field, this creates a new range and adds the first canvas.
     * If the toc metadata includes a separator, sub-ranges are created.
     * @param tocs ranges from toc metadata
     * @param canvasGenerator generator for the current canvas
     * @return
     */
    public void setTocRange(List<String> tocs , CanvasGenerator canvasGenerator) {

        for (String toc : tocs) {
            // Set the current range. The first time called, this will be the root range.
            RangeGenerator currRange = root;
            String[] parts = toc.split(IIIFUtils.TOC_SEPARATOR_REGEX);
            String key = "";
            // Process range and sub-ranges.
            for (int pIdx = 0; pIdx < parts.length; pIdx++) {
                if (pIdx > 0) {
                    key += IIIFUtils.TOC_SEPARATOR;
                }
                key += parts[pIdx];
                if (tocRanges.get(key) != null) {
                    // This handles the case of a bitstream that crosses two ranges.
                    currRange = tocRanges.get(key);
                } else {
                    // Create the new range
                    RangeGenerator range = new RangeGenerator(this);
                    range.setLabel(parts[pIdx]);
                    // Add it to the root range
                    currRange.addSubRange(range);

                    // Current range is now the new sub-range
                    currRange = range;
                    // Add sub-range to the list.
                    tocRanges.put(key, range);
                }
            }
            // Add a canvas to the current range.
            currRange
                .addCanvas(canvasService.getRangeCanvasReference(canvasGenerator.getIdentifier()));

            // Finally, update the range that will be used in the next iteration.
            lastRange = currRange;
        }
    }

    /**
     * Ranges expect the sub-range object to have only an identifier.
     *
     * @param range the sub-range to reference
     * @return RangeGenerator able to create the reference
     */
    public RangeGenerator getRangeReference(RangeGenerator range) {
        return new RangeGenerator(this).setIdentifier(range.getIdentifier());
    }
}
