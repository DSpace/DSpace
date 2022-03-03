/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;

public class LDNBusinessDelegate {

    private static final Logger log = Logger.getLogger(LDNBusinessDelegate.class);

    private String foo;

    @PostConstruct
    public void init() {
        log.info("\n\n" + foo + "\n\n");
    }

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

}
