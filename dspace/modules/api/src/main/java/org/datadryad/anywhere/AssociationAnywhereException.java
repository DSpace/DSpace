package org.datadryad.anywhere;

/**
 * User: lantian @ atmire . com
 * Date: 8/1/14
 * Time: 10:04 AM
 */
public class AssociationAnywhereException extends Exception {


    public AssociationAnywhereException(String s) {
        super(s);
    }

    public AssociationAnywhereException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public AssociationAnywhereException(Throwable throwable) {
        super(throwable);
    }
}
