package org.dspace.app.cris.configuration;

import org.dspace.app.cris.model.dto.SimpleViewEntityDTO;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public interface ISimpleViewResolver
{

    public void fillDTO(Context context, SimpleViewEntityDTO dto, DSpaceObject dso);

}
