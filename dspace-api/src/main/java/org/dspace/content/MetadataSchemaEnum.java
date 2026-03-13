/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 * This is an enumeration that holds track of a few special MetadataSchema types.
 * It is important to note that this list is not exhaustive for the MetadataSchema
 * types and different MetadataSchema can easily be made.
 * These MetadataSchema objects are simply required.
 */
public enum MetadataSchemaEnum {
    DC("dc"),
    EPERSON("eperson"),
    RELATION("relation"),
    PERSON("person");

    /**
     * The String representation of the MetadataSchemaEnum
     */
    private final String name;

    /**
     * Default constructor with the name parameter.
     * @param name  The name parameter
     */
    MetadataSchemaEnum(String name) {
        this.name = name;
    }

    /**
     * Generic getter for the String representation of the enumerated object.
     * @return  The name of the enumerated object
     */
    public String getName() {
        return name;
    }
}
