/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import java.io.Serializable;
import javax.persistence.*;

/**
 * Database entity representation of the checksum_results table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name="checksum_results")
public class ChecksumResult
        implements Serializable
{
    @Id
    @Column(name="result_code")
    @Enumerated(EnumType.STRING)
    private ChecksumResultCode resultCode;

    @Column(name = "result_description")
    private String resultDescription;

    /**
     * Protected constructor, new object creation impossible
     */
    protected ChecksumResult()
    {

    }
    public ChecksumResultCode getResultCode() {
        return resultCode;
    }

    public String getResultDescription() {
        return resultDescription;
    }
}
