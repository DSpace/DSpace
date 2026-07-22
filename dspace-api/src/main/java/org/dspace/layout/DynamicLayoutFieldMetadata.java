/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
@Entity(name = "DynamicLayoutFieldMetadata")
@DiscriminatorValue(DynamicLayoutFieldMetadata.METADATA_FIELD_TYPE)
public class DynamicLayoutFieldMetadata extends DynamicLayoutField {

    public static final String METADATA_FIELD_TYPE = "METADATA";

}
