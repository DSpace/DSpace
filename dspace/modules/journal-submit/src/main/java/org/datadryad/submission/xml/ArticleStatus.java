/*
 * EthOntos - a tool for comparative methods using ontologies
 * Copyright 2004-2005 Peter E. Midford
 * 
 * Created on Oct 19, 2011
 * Last updated on Oct 19, 2011
 * 
 */
package org.datadryad.submission.xml;

import nu.xom.Element;

public class ArticleStatus extends Element {

    public ArticleStatus(String aStatus){
        super("ArticleStatus");
        appendChild(aStatus);
    }
}
