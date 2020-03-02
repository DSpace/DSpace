/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.projection.Projection;

/**
 * A directly addressable REST resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public abstract class RestAddressableModel implements RestModel {

    private int embedLevel = 0;

    private Projection projection = Projection.DEFAULT;

    @JsonIgnore
    public abstract String getCategory();

    @JsonIgnore
    public abstract Class getController();

    @JsonIgnore
    public int getEmbedLevel() {
        return embedLevel;
    }

    public void setEmbedLevel(int embedLevel) {
        this.embedLevel = embedLevel;
    }

    @JsonIgnore
    public Projection getProjection() {
        return projection;
    }

    public void setProjection(Projection projection) {
        this.projection = projection;
    }
}
