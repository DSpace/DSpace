/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor;

import org.dspace.content.Bitstream;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * BitstreamSignPostingProcessor interface represents SignPostingProcessor for a bitstream.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public interface BitstreamSignPostingProcessor extends SignPostingProcessor<Bitstream> {

    default String buildAnchor(Bitstream bitstream) {
        ConfigurationService configurationService =
                DSpaceServicesFactory.getInstance().getConfigurationService();
        String url = configurationService.getProperty("dspace.ui.url");
        return url + "/bitstreams/" + bitstream.getID() + "/download";
    }
}
