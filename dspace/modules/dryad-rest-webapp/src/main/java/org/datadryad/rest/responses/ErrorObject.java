/*
 */
package org.datadryad.rest.responses;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ErrorObject {
    public String status;
    public String title; // should not change from occurrence to occurrence
    public String detail;
    public String path;

}
