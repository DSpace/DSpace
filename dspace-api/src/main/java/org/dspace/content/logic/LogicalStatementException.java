/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.logic;

/**
 * Exception for errors encountered while evaluating logical statements
 * defined as spring beans.
 *
 * @author Kim Shepherd
 */
public class LogicalStatementException extends RuntimeException {

    public LogicalStatementException() {
        super();
    }

    public LogicalStatementException(String s, Throwable t) {
        super(s, t);
    }

    public LogicalStatementException(String s) {
        super(s);
    }

    public LogicalStatementException(Throwable t) {
        super(t);
    }

}
