package org.dspace.content.logic;

/**
 * Exception for errors encountered while evaluating logical statements
 * defined as spring beans.
 *
 * @author Kim Shepherd
 * @version $Revision$
 */
public class LogicalStatementException extends Exception {

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
