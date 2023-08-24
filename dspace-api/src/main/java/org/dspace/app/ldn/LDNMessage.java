/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.dspace.content.DSpaceObject;
import org.dspace.core.ReloadableEntity;

/**
 * Class representing ldnMessages stored in the DSpace system.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Entity
@Table(name = "ldn_messages")
public class LDNMessage implements ReloadableEntity<String> {

    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "object", referencedColumnName = "uuid")
    private DSpaceObject object;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "type")
    private String type;

    @ManyToOne
    @JoinColumn(name = "origin", referencedColumnName = "id")
    private NotifyServiceEntity origin;

    @ManyToOne
    @JoinColumn(name = "target", referencedColumnName = "id")
    private NotifyServiceEntity target;

    @ManyToOne
    @JoinColumn(name = "inReplyTo", referencedColumnName = "id")
    private LDNMessage inReplyTo;

    @ManyToOne
    @JoinColumn(name = "context", referencedColumnName = "uuid")
    private DSpaceObject context;

    protected LDNMessage() {

    }

    protected LDNMessage(String id) {
        this.id = id;
    }

    @Override
    public String getID() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DSpaceObject getObject() {
        return object;
    }

    public void setObject(DSpaceObject object) {
        this.object = object;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NotifyServiceEntity getOrigin() {
        return origin;
    }

    public void setOrigin(NotifyServiceEntity origin) {
        this.origin = origin;
    }

    public NotifyServiceEntity getTarget() {
        return target;
    }

    public void setTarget(NotifyServiceEntity target) {
        this.target = target;
    }

    public LDNMessage getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(LDNMessage inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    public DSpaceObject getContext() {
        return context;
    }

    public void setContext(DSpaceObject context) {
        this.context = context;
    }
}
