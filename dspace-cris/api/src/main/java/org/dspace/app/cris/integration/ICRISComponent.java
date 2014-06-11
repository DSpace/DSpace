/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration;

import it.cilea.osd.jdyna.components.IBeanSubComponent;
import it.cilea.osd.jdyna.components.IComponent;

import org.dspace.app.cris.configuration.RelationConfiguration;
import org.dspace.app.cris.integration.statistics.IStatsDualComponent;

public interface ICRISComponent<IBC extends IBeanSubComponent> extends IComponent<IBC>, IStatsDualComponent
{

    public RelationConfiguration getRelationConfiguration();
    public Class getTarget();
}
