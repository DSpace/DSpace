/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa.v2;

import java.io.Serializable;

/**
 * Model class for the Embargo of SHERPAv2 API (JSON)
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk@4science.com)
 */
public class SHERPAEmbargo implements Serializable {

    private static final long serialVersionUID = 6140668058547523656L;

    private int amount;
    private String units;

    public SHERPAEmbargo(int amount, String units) {
        this.amount = amount;
        this.units = units;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

}