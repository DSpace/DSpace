/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.helpers;

public class SyntacticSugar {
    public static <T> T given (T elem) {
        return elem;
    }
    public static <T> T the (T elem) {
        return elem;
    }
    public static <T> T and (T elem) {
        return elem;
    }
}
