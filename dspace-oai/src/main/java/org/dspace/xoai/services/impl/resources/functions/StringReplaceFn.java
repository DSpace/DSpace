/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.services.impl.resources.functions;

/**
 * Serves as proxy for call from XSL engine. Replaces http:// with https://
 */
public class StringReplaceFn extends StringXSLFunction {

    @Override
    protected String getFnName() {
        return "stringReplace";
    }

    @Override
    protected String getStringResult(String param) {
        return param.replaceFirst("http://", "https://");
    }
}
