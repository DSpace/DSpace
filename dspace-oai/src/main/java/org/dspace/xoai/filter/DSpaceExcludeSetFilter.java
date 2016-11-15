/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.SolrFilterResult;

import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterValue;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.SimpleType;


/**
 * Excludes records from the set specified in the <code>excludeSet</code>
 * parameter.
 *
 * @author Àlex Magaz Graça
 */
public class DSpaceExcludeSetFilter extends DSpaceFilter {
    private static Logger log = LogManager.getLogger(DSpaceExcludeSetFilter.class);

    private List<String> excludeSets;

    private List<String> getExcludeSets() {
        if (this.excludeSets == null) {
            ParameterValue excludeSets = getConfiguration().get("excludeSet");
            if (excludeSets == null) excludeSets = getConfiguration().get("excludeSet");

            if (excludeSets instanceof SimpleType) {
                this.excludeSets = new ArrayList<String>();
                this.excludeSets.add(((SimpleType) excludeSets).asString());
            } else {
                this.excludeSets = new ArrayList<String>();
                for (ParameterValue val : excludeSets.asParameterList().getValues())
                    this.excludeSets.add(val.asSimpleType().asString());
            }
        }
        return excludeSets;
    }

    @Override
    public boolean isShown(DSpaceItem item) {
        for (String excludeSet: this.getExcludeSets()) {
            if ( item.getSets().contains(excludeSet) )
                return false;
        }
        return true;
    }

    @Override
    public SolrFilterResult buildSolrQuery() {
        StringBuilder cond = new StringBuilder("NOT (");
        List<String> excludeSets = this.getExcludeSets();
        for (int i = 0; i < excludeSets.size(); i++) {
            // TODO: distinguish communities/collections
            if (excludeSets.get(i).substring(0, 4) == "col_") {
                cond.append("item.collections:");
            } else if (excludeSets.get(i).substring(0, 4) == "com_") {
                cond.append("item.communities:");
            } else {
                throw new UnsupportedOperationException("DSpaceExcludeSetFilter: Unexpected set prefix " + excludeSets.get(i).substring(0, 4));
            }
            cond.append(excludeSets.get(i));
            if (i < excludeSets.size() - 1)
                cond.append(" OR ");
        }
        cond.append(")");

        return new SolrFilterResult(cond.toString());
    }
}
