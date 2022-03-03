/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder(value = {
    "@context",
    "id",
    "type",
    "actor",
    "context",
    "object",
    "origin",
    "target",
    "inReplyTo"
})
public class Notification extends Base {

    @JsonProperty("@context")
    private String[] c = new String[] {
        "https://purl.org/coar/notify",
        "https://www.w3.org/ns/activitystreams"
    };

    @JsonProperty("actor")
    private Actor actor;

    @JsonProperty("context")
    private Context context;

    @JsonProperty("object")
    private Object object;

    @JsonProperty("origin")
    private Service origin;

    @JsonProperty("target")
    private Service target;

    @JsonProperty("inReplyTo")
    private String inReplyTo;

    public Notification() {
        super();
    }

    public String[] getC() {
        return c;
    }

    public void setC(String[] c) {
        this.c = c;
    }

    public Actor getActor() {
        return actor;
    }

    public void setActor(Actor actor) {
        this.actor = actor;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Service getOrigin() {
        return origin;
    }

    public void setOrigin(Service origin) {
        this.origin = origin;
    }

    public Service getTarget() {
        return target;
    }

    public void setTarget(Service target) {
        this.target = target;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

}
