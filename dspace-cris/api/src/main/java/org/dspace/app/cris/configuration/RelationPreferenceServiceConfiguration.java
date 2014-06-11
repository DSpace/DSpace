/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.configuration;

import java.util.List;

public class RelationPreferenceServiceConfiguration
{
    private List<RelationPreferenceConfiguration> list;

    public List<RelationPreferenceConfiguration> getList()
    {
        return list;
    }

    public void setList(List<RelationPreferenceConfiguration> list)
    {
        this.list = list;
    }

    public synchronized RelationPreferenceConfiguration getRelationPreferenceConfiguration(
            String name)
    {
        for (RelationPreferenceConfiguration conf : list)
        {
            if (conf.getRelationConfiguration() != null)
            {
                if (name.equals(conf.getRelationConfiguration()
                        .getRelationName()))
                {
                    return conf;
                }
            }
        }
        return null;
    }
}
