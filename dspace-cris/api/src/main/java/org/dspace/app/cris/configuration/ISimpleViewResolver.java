/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.configuration;

import org.dspace.app.cris.model.dto.SimpleViewEntityDTO;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public interface ISimpleViewResolver
{

    public void fillDTO(Context context, SimpleViewEntityDTO dto, DSpaceObject dso);

}
