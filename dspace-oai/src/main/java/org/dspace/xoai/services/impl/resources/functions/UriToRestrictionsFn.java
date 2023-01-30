/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.services.impl.resources.functions;

import javax.xml.parsers.ParserConfigurationException;

import org.dspace.utils.LicenseUtil;

/**
 * Serves as proxy for call from XSL engine. Calls LicenseUtil
 * @author Marian Berger (marian.berger at dataquest.sk)
 */
public class UriToRestrictionsFn extends NodeListXslFunction {
    @Override
    protected String getFnName() {
        return "uriToRestrictions";
    }

    @Override
    protected org.w3c.dom.NodeList getNodeList(String param) {
        try {
            return LicenseUtil.uriToRestrictions(param);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

}
