/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CrisLayoutBoxConfigurationRest;
import org.dspace.core.Context;
import org.dspace.layout.CrisLayoutBox;

/**
 * This is the interface that must be implemented by the configurator of box. At
 * least one configurator for each defined box type is expected
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public interface CrisLayoutBoxConfigurator {

    public boolean support(CrisLayoutBox box);

    public CrisLayoutBoxConfigurationRest getConfiguration(CrisLayoutBox box);

    public void configure(Context context, CrisLayoutBox box, CrisLayoutBoxConfigurationRest rest);

}
