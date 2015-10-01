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
 * This filter allows one to retrieve (from the data source) those items
 * which contains at least one metadata field value defined, it allows
 * one to define multiple metadata fields to check against.
 * <p/>
 * One line summary: At least one metadata field defined
 *
 * @author Ariel J. Lira <arieljlira@gmail.com>
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class DSpaceMetadataExistsFilter extends DSpaceFilter {
    private static final Logger log = LogManager
            .getLogger(DSpaceMetadataExistsFilter.class);

    private List<String> fields;

    private List<String> getFields() {
        if (this.fields == null) {
            ParameterValue fields = getConfiguration().get("fields");
            if (fields == null) fields = getConfiguration().get("field");

            if (fields instanceof SimpleType) {
                this.fields = new ArrayList<String>();
                this.fields.add(((SimpleType) fields).asString());
            } else {
                this.fields = new ArrayList<String>();
                for (ParameterValue val : fields.asParameterList().getValues())
                    this.fields.add(val.asSimpleType().asString());
            }

        }
        return fields;
    }

    @Override
    public boolean isShown(DSpaceItem item) {
        for (String field : this.getFields()) {
            //do we have a match? if yes, our job is done
            if (item.getMetadata(field).size() > 0)
                return true;
        }
        return false;
    }

    @Override
    public SolrFilterResult buildSolrQuery() {
        StringBuilder cond = new StringBuilder("(");
        List<String> fields = this.getFields();
        for (int i = 0; i < fields.size(); i++) {
            cond.append("metadata.").append(fields.get(i)).append(":[* TO *]");
            if (i < fields.size() - 1)
                cond.append(" OR ");
        }
        cond.append(")");

        return new SolrFilterResult(cond.toString());
    }

}
