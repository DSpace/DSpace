/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import it.cilea.osd.jdyna.model.AnagraficaSupport;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

public interface IExportableDynamicObject<TP extends PropertiesDefinition, P extends Property<TP>, AS extends AnagraficaSupport<P, TP>>
{

    String getNamePublicIDAttribute();

    String getValuePublicIDAttribute();

    String getNameIDAttribute();

    String getValueIDAttribute();

    String getNameBusinessIDAttribute();

    String getValueBusinessIDAttribute();

    String getNameTypeIDAttribute();

    String getValueTypeIDAttribute();

    AS getAnagraficaSupport();

    String getNameSingleRowElement();

}
