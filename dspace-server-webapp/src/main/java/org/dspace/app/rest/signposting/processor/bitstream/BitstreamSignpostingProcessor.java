/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor.bitstream;

import org.dspace.app.rest.signposting.processor.AbstractSignPostingProcessor;
import org.dspace.app.rest.signposting.processor.SignPostingProcessor;
import org.dspace.content.Bitstream;
import org.dspace.util.FrontendUrlService;

/**
 * An abstract class represents {@link SignPostingProcessor } for a bitstream.
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public abstract class BitstreamSignpostingProcessor extends AbstractSignPostingProcessor
        implements SignPostingProcessor<Bitstream> {

    protected final FrontendUrlService frontendUrlService;

    public BitstreamSignpostingProcessor(FrontendUrlService frontendUrlService) {
        this.frontendUrlService = frontendUrlService;
    }

    public String buildAnchor(Bitstream bitstream) {
        return frontendUrlService.generateUrl(bitstream);
    }
}
