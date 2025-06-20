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

/**
 * the json object from witch @see org.dspace.app.ldn.LDNMessageEntity are created.
 * <a href="https://notify.coar-repositories.org/patterns/">see official coar doc</a>
 */
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

    /**
     * 
     */
    public Notification() {
        super();
    }

    /**
     * @return String[]
     */
    public String[] getC() {
        return c;
    }

    /**
     * @param c
     */
    public void setC(String[] c) {
        this.c = c;
    }

    /**
     * @return Actor
     */
    public Actor getActor() {
        return actor;
    }

    /**
     * @param actor
     */
    public void setActor(Actor actor) {
        this.actor = actor;
    }

    /**
     * @return Context
     */
    public Context getContext() {
        return context;
    }

    /**
     * @param context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * @return Object
     */
    public Object getObject() {
        return object;
    }

    /**
     * @param object
     */
    public void setObject(Object object) {
        this.object = object;
    }

    /**
     * @return Service
     */
    public Service getOrigin() {
        return origin;
    }

    /**
     * @param origin
     */
    public void setOrigin(Service origin) {
        this.origin = origin;
    }

    /**
     * @return Service
     */
    public Service getTarget() {
        return target;
    }

    /**
     * @param target
     */
    public void setTarget(Service target) {
        this.target = target;
    }

    /**
     * @return String
     */
    public String getInReplyTo() {
        return inReplyTo;
    }

    /**
     * @param inReplyTo
     */
    public void setInReplyTo(String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

}