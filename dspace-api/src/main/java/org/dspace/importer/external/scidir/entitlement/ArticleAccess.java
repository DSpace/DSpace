/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scidir.entitlement;

import org.apache.commons.lang.StringUtils;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 09/11/15
 * Time: 10:54
 */
public class ArticleAccess {

    private String audience;
    private String startDate;

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    @Override
    public String toString() {
        if (StringUtils.isNotBlank(startDate)) {
            return "audience " + getAudience() + " start date " + getStartDate();
        } else {
            return "audience " + getAudience();
        }
    }
}
