/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;

/**
 * UUID generator that allows DSpaceObjects to provide a pre-determined UUID.
 * If no predefined UUID is set, a random UUID is generated.
 *
 * @author April Herron
 */
public class PredefinedUUIDGenerator implements BeforeExecutionGenerator {

    /**
     * Constructor required by {@link org.hibernate.annotations.IdGeneratorType}.
     *
     * @param annotation the {@link PredefinedUUID} annotation instance
     * @param member the annotated member (field or method)
     * @param context the generator creation context
     */
    public PredefinedUUIDGenerator(PredefinedUUID annotation, Member member,
                                   GeneratorCreationContext context) {
        // no configuration needed
    }

    @Override
    public Object generate(
        SharedSessionContractImplementor session, Object owner,
        Object currentValue, EventType eventType
    ) {
        if (owner instanceof DSpaceObject spaceObject) {
            UUID uuid = spaceObject.getPredefinedUUID();
            if (uuid != null) {
                return uuid;
            }
        }
        return UUID.randomUUID();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }
}
