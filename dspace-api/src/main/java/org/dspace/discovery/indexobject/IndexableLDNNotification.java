/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import org.dspace.app.ldn.LDNMessageEntity;
import org.dspace.discovery.IndexableObject;

/**
 * {@link LDNMessageEntity} implementation for the {@link IndexableObject}
 *
 * @author Stefano Maffei at 4science.com
 */
public class IndexableLDNNotification extends AbstractIndexableObject<LDNMessageEntity, String> {

    private LDNMessageEntity ldnMessage;
    public static final String TYPE = LDNMessageEntity.class.getSimpleName();

    public IndexableLDNNotification(LDNMessageEntity ldnMessage) {
        super();
        this.ldnMessage = ldnMessage;
    }

    @Override
    public String getType() {
        return getTypeText();
    }

    @Override
    public String getID() {
        return ldnMessage.getID();
    }

    @Override
    public LDNMessageEntity getIndexedObject() {
        return ldnMessage;
    }

    @Override
    public void setIndexedObject(LDNMessageEntity object) {
        this.ldnMessage = object;
    }

    @Override
    public String getTypeText() {
        return TYPE;
    }

}
