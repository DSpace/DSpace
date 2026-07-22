/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.enumeration;

/**
 * Enum that contains the rules for the validation of the sub types of the
 * fields rendering for the Cris Layout Tool.
 * 
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public enum RenderingSubTypeValidationRule {

    /**
     * Sub types are allowed, but not mandatory, for the given rendering type.
     */
    ALLOWED,

    /**
     * Sub types are not allowed for the given rendering type.
     */
    NOT_ALLOWED,

    /**
     * A sub type is mandatory for the given rendering type.
     */
    MANDATORY;
}
