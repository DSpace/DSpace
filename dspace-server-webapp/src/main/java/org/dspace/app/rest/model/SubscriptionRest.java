/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.RestResourceController;

@LinksRest(links = {
    @LinkRest(name = SubscriptionRest.DSPACE_OBJECT, method = "getDSpaceObject"),
    @LinkRest(name = SubscriptionRest.EPERSON, method = "getEPerson")
})
public class SubscriptionRest extends BaseObjectRest<Integer> {

    private static final long serialVersionUID = 1L;

    public static final String NAME = "subscription";
    public static final String NAME_PLURAL = "subscriptions";
    public static final String CATEGORY = "core";
    public static final String DSPACE_OBJECT = "resource";
    public static final String EPERSON = "eperson";

    private Integer id;
    private String subscriptionType;
    private List<SubscriptionParameterRest> subscriptionParameterList = new ArrayList<>();

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class<RestResourceController> getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    public void setSubscriptionType(String type) {
        this.subscriptionType = type;
    }

    public List<SubscriptionParameterRest> getSubscriptionParameterList() {
        return subscriptionParameterList;
    }

    public void setSubscriptionParameterList(List<SubscriptionParameterRest> subscriptionParameterList) {
        this.subscriptionParameterList = subscriptionParameterList;
    }

    public String getSubscriptionType() {
        return this.subscriptionType;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

}
