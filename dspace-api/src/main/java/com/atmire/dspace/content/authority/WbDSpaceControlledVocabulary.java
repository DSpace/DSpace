/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package com.atmire.dspace.content.authority;

import org.dspace.content.authority.DSpaceControlledVocabulary;
import org.w3c.dom.Node;

public class WbDSpaceControlledVocabulary extends DSpaceControlledVocabulary {

    @Override
    protected void init() {
        super.init();
        hierarchyDelimiter = " " + hierarchyDelimiter + " ";
    }

    protected String buildString(Node node) {
        if (node.getNodeType() == Node.DOCUMENT_NODE || (
            node.getParentNode() != null &&
            node.getParentNode().getNodeType() == Node.DOCUMENT_NODE)) {
            return ("");
        } else {
            return super.buildString(node);
        }
    }
}
