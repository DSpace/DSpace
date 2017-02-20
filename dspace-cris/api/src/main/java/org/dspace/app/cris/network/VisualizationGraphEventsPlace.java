/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class VisualizationGraphEventsPlace extends AVisualizationGraphModeTwo
    {

        public final static String CONNECTION_NAME = "eventsplace";

        private static final String FIELD_QUERY = "crisrp.partecipatedin.networkevents";
        
        @Override
        public String getType()
        {
            return CONNECTION_NAME;
        }

        @Override
        public String getLineWidthToOverride()
        {
            return "2.5";
        }

        @Override
        public String getConnectionName()
        {
            return CONNECTION_NAME;
        }

        @Override
        protected String getFacetFieldQuery()
        {
            return FIELD_QUERY;
        }

        @Override
        protected String buildExtraCustom(String extra)
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getFacet(String value)
        {
            return FA_VALUE;
        }
    
        protected List<String> transform(String values)
        {
            // e.g.
            String tt = StringUtils.capitalise(values);
            List<String> result = new LinkedList<String>();
            result.add(tt);
            return result;
        }
}
