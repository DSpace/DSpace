/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.hibernate.proxy.HibernateProxyHelper;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Database entity representation of the bundle2bitstream table
 *
 * @author kevinvandevelde at atmire.com
 */
@Entity
@Table(name="bundle2bitstream")
public class BundleBitstream implements Serializable {

    @Id
    @ManyToOne
    @JoinColumn(name="bundle_id", nullable = false, referencedColumnName = "uuid")
    private Bundle bundle;

    @Id
    @ManyToOne
    @JoinColumn(name="bitstream_id", nullable = false, referencedColumnName = "uuid")
    private Bitstream bitstream;


    @Column(name="bitstream_order")
    private int bitstreamOrder = -1;

    protected BundleBitstream() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
        {
            return false;
        }
        Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
        if (getClass() != objClass)
        {
            return false;
        }
        final BundleBitstream other = (BundleBitstream) obj;
        if(!other.getBundle().equals(bundle))
        {
            return false;
        }
        if(!other.getBitstream().equals(bitstream))
        {
            return false;
        }
        return true;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Bitstream getBitstream() {
        return bitstream;
    }

    public int getBitstreamOrder() {
        return bitstreamOrder;
    }

    void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    void setBitstream(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    void setBitstreamOrder(int bitstreamOrder) {
        this.bitstreamOrder = bitstreamOrder;
    }
}
