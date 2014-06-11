/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.integration.statistics;

import java.util.Map;

public interface IStatComponentService<T extends IStatsGenericComponent>
{
    Map<String, T> getComponents();

    T getSelectedObjectComponent();
    
    Map getCommonsParams();
    
    public boolean isShowSelectedObject();
    
    public boolean isShowExtraTab();
}
