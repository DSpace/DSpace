/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.swordapp.server;

public interface SwordConfiguration
{
    boolean returnDepositReceipt();

    boolean returnStackTraceInError();

    boolean returnErrorBody();

    String generator();

    String generatorVersion();

    String administratorEmail();

	String getAuthType();

	boolean storeAndCheckBinary();

	String getTempDirectory();

	int getMaxUploadSize();
}
