/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

/**
 * Model a LOGIN statistics for a single EPerson.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SuppressWarnings({ "serial", "rawtypes" })
public class LoginStatisticsRest extends BaseObjectRest<String> {

    public static final String NAME = "login";
    public static final String CATEGORY = RestModel.STATISTICS;
    public static final String NAME_PLURAL = "logins";

    private String name;

    private String email;

    private long count;

    /** @return the name of the EPerson */
    public String getName() {
        return name;
    }

    /** @param name the EPerson name to set */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the email of the EPerson */
    public String getEmail() {
        return email;
    }

    /** @param email the EPerson email to set */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return the number of logins recorded for the EPerson */
    public long getCount() {
        return count;
    }

    /** @param count the number of logins to set */
    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return NAME_PLURAL;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

}
