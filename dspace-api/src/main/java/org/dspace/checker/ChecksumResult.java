/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import javax.persistence.*;

/**
 * Database entity representation of the checksum_results table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name="checksum_results")
public final class ChecksumResult
{
    @Id
    @Column(name="result_code")
    @Enumerated(EnumType.STRING)
    private ChecksumResultCode resultCode;

    @Column(name = "result_description")
    private String resultDescription;

    public ChecksumResultCode getResultCode() {
        return resultCode;
    }

    public String getResultDescription() {
        return resultDescription;
    }
}
