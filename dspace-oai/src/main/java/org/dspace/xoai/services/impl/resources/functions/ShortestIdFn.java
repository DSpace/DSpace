/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.resources.functions;

import org.dspace.utils.LangUtil;

/**
 * Serves as proxy for call from XSL engine. Calls LicenseUtil
 * @author Marian Berger (marian.berger at dataquest.sk)
 */
public class ShortestIdFn extends StringXSLFunction {
    @Override
    protected String getFnName() {
        return "shortestIdFn";
    }

    @Override
    protected String getStringResult(String param) {
        return LangUtil.getShortestId(param);
    }
}
