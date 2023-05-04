/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.relation;

import java.text.MessageFormat;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.signposting.model.Linkset;
import org.dspace.app.rest.signposting.model.Lset;
import org.dspace.app.rest.signposting.model.Relation;
import org.dspace.app.rest.signposting.processor.BitstreamSignPostingProcessor;
import org.dspace.app.rest.signposting.processor.ItemSignPostingProcessor;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of {@link ItemSignPostingProcessor} for the license relation.
 */
public class BitstreamLicenseProcessor extends ASignPostingProcessor
        implements BitstreamSignPostingProcessor {

    private static Logger log = Logger.getLogger(BitstreamLicenseProcessor.class);

    @Autowired
    private BitstreamService bitstreamService;

    public BitstreamLicenseProcessor() {
        setRelation("license");
    }

    @Override
    public void buildRelation(Context context, HttpServletRequest request,
                              Bitstream bitstream, List<Linkset> linksets,
                              Linkset primaryLinkset) {
        try {
            String license = bitstreamService.getMetadata(bitstream, getMetadataField());
            if (StringUtils.isNotBlank(license)) {
                if (StringUtils.isNotBlank(getPattern())) {
                    license = MessageFormat.format(getPattern(), license);
                }
                primaryLinkset.getLicense().add(new Relation(license, null));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void buildLset(Context context, HttpServletRequest request,
                          Bitstream bitstream, List<Lset> lsets) {
        try {
            String license = bitstreamService.getMetadata(bitstream, getMetadataField());
            if (StringUtils.isNotBlank(license)) {
                if (StringUtils.isNotBlank(getPattern())) {
                    license = MessageFormat.format(getPattern(), license);
                }
                lsets.add(new Lset(license, getRelation(), buildAnchor(bitstream)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
