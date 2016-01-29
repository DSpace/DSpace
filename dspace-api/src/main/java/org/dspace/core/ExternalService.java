package org.dspace.core;

import org.dspace.content.DSpaceObject;

public interface ExternalService
{
    DSpaceObject getObject(String externalId);
}
