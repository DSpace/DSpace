/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;
import org.dspace.supervision.SupervisionOrder;

/**
 * The REST Resource of {@link SupervisionOrder}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class SupervisionOrderRest extends BaseObjectRest<Integer> {

    public static final String NAME = "supervisionorder";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private Integer id;

    @JsonIgnore
    private ItemRest item;

    @JsonIgnore
    private GroupRest group;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public ItemRest getItem() {
        return item;
    }

    public void setItem(ItemRest item) {
        this.item = item;
    }

    public GroupRest getGroup() {
        return group;
    }

    public void setGroup(GroupRest group) {
        this.group = group;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }
}
