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
import org.dspace.content.Bitstream;
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
    private RangeGenerator currentRange;
    private RangeGenerator root;


    public RangeService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
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
    public void setRootRange(String manifestId) {
        root = new RangeGenerator(this);
        root.addViewingHint("top");
        root.setLabel(I18nUtil.getMessage("iiif.toc.root-label"));
        root.setIdentifier(manifestId + "/range/r0");
    }

    /**
     * Gets the current ranges.
     * @return map of toc ranges.
     */
    public Map<String, RangeGenerator> getTocRanges() {
        return this.tocRanges;
    }

    /**
     * Updates the current range and adds sub-ranges.
     * @param bitstream bitstream DSO
     * @param bundleToCPrefix range prefix from bundle metadata
     * @param canvas the current canvas generator
     */
    public void updateRanges(Bitstream bitstream, String bundleToCPrefix, CanvasGenerator canvas) {
        List<String> tocs = utils.getIIIFToCs(bitstream, bundleToCPrefix);
        if (tocs.size() > 0) {
            // Add a new Range.
            addTocRange(tocs, canvas);
        } else {
            // Add canvases to the current Range.
            if (tocRanges.size() > 0) {
                String canvasIdentifier = canvas.getIdentifier();
                CanvasGenerator simpleCanvas = canvasService.getRangeCanvasReference(canvasIdentifier);
                currentRange.addCanvas(simpleCanvas);
            }
        }
    }

    /**
     * Adds sub-ranges to the root Range. If the toc metadata includes a separator,
     * hierarchical sub-ranges are created.
     * @param tocs ranges from toc metadata
     * @param canvasGenerator generator for the current canvas
     * @return
     */
    private void addTocRange(List<String> tocs , CanvasGenerator canvasGenerator) {

        for (String toc : tocs) {
            // Make tempRange a reference to root.
            RangeGenerator tempRange = root;
            String[] parts = toc.split(IIIFUtils.TOC_SEPARATOR_REGEX);
            String key = "";
            // Process sub-ranges.
            for (int pIdx = 0; pIdx < parts.length; pIdx++) {
                if (pIdx > 0) {
                    key += IIIFUtils.TOC_SEPARATOR;
                }
                key += parts[pIdx];
                if (tocRanges.get(key) != null) {
                    // Handles the case of a bitstream that crosses two ranges.
                    tempRange = tocRanges.get(key);
                } else {
                    RangeGenerator range = new RangeGenerator(this);
                    range.setLabel(parts[pIdx]);
                    // Add sub-range to the root Range
                    tempRange.addSubRange(range);
                    // Add new sub-range to the map.
                    tocRanges.put(key, range);
                    // Make tempRange a reference to the new sub-range.
                    tempRange = range;
                }
            }
            // Add a simple canvas reference to the Range.
            tempRange
                .addCanvas(canvasService.getRangeCanvasReference(canvasGenerator.getIdentifier()));

            // Update the current Range.
            currentRange = tempRange;
        }
    }

    /**
     * Ranges expect the sub-range to have only an identifier.
     *
     * @param range the sub-range to reference
     * @return RangeGenerator able to create the reference
     */
    public RangeGenerator getRangeReference(RangeGenerator range) {
        return new RangeGenerator(this).setIdentifier(range.getIdentifier());
    }
}
