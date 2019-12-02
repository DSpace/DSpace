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

import com.google.common.base.Function;
import com.lyncode.builder.ListBuilder;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterList;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.ParameterValue;
import com.lyncode.xoai.dataprovider.xml.xoaiconfig.parameters.SimpleType;

import org.apache.commons.lang3.StringUtils;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.filter.results.SolrFilterResult;

/**
 * Filter for limit the type of entities returned by the oai.
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class DSpaceEntityTypeFilter extends DSpaceFilter {

    private List<String> values;
    private String relationShipType;

    private String getRelationShipType() {
        if (relationShipType == null) {
            ParameterValue parameterValue = getConfiguration().get("relationShipType");
            relationShipType = ((SimpleType) parameterValue).asString();
        }
        return relationShipType;
    }

    private List<String> getValues() {
        if (values == null) {
            ParameterValue parameterValue = getConfiguration().get("value");
            if (parameterValue == null) {
                parameterValue = getConfiguration().get("values");
            }

            if (parameterValue instanceof SimpleType) {
                values = new ArrayList<String>();
                values.add(((SimpleType) parameterValue).asString());
            } else if (parameterValue instanceof ParameterList) {
                values = new ListBuilder<ParameterValue>().add(parameterValue.asParameterList().getValues())
                        .build(new Function<ParameterValue, String>() {
                            @Override
                            public String apply(ParameterValue elem) {
                                return elem.asSimpleType().asString();
                            }
                        });
            } else {
                values = new ArrayList<String>();
            }
        }
        return values;
    }

    public boolean isShown(DSpaceItem item) {
        List<String> relationshipType = item.getMetadata(getRelationShipType());
        if (relationshipType != null && !relationshipType.isEmpty()) {
            getValues().contains(relationshipType.get(0));
        }
        return false;
    }

    @Override
    public SolrFilterResult buildSolrQuery() {
        // In Solr, we store withdrawn items as "deleted".
        // See org.dspace.xoai.app.XOAI, index(Item) method.
        return new SolrFilterResult("item.type:(" + StringUtils.join(getValues(), " OR ") + ")");
    }
}
