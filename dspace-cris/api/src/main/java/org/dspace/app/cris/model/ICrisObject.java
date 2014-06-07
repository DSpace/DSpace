/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import it.cilea.osd.common.core.HasTimeStampInfo;
import it.cilea.osd.common.model.Identifiable;
import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

public interface ICrisObject<P extends Property<TP>, TP extends PropertiesDefinition>
        extends HasTimeStampInfo, UUIDSupport, Identifiable,
        AnagraficaSupport<P, TP>
{
    
    public int getType();    
    public int getID();     
    public String getPublicPath();
    public SourceReference getSourceReference();
}
