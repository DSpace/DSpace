/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics.embeddable.model;

import org.dspace.app.metrics.CrisMetrics;

public class EmbeddableCrisMetrics extends CrisMetrics {
    private String embeddableId;

    public String getEmbeddableId() {
        return embeddableId;
    }

    public void setEmbeddableId(String embeddableId) {
        this.embeddableId = embeddableId;
    }
}