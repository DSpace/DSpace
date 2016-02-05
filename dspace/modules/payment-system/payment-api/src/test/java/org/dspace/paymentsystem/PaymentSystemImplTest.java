/*
 */
package org.dspace.paymentsystem;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class PaymentSystemImplTest {
    private static final Long GB = 1L * 1024L * 1024L * 1024L; // Giga
    private static final Long TEN_GB = 10L * GB;

    public PaymentSystemImplTest() {
    }

    @Test
    public void testCalculateNoSurcharge() {
        System.out.println("Testing surcharge calculation for 9GB package");
        long allowedSize = TEN_GB;
        long totalDataFileSize = 9L * GB;
        double fileSizeFeeAfter = 10.0;
        double initialSurcharge = 15.0;
        long surchargeUnitSize = GB;
        double expResult = 0.0;
        double result = PaymentSystemImpl.calculateFileSizeSurcharge(allowedSize, totalDataFileSize, fileSizeFeeAfter, initialSurcharge, surchargeUnitSize);
        assertEquals("Calculated surcharge does not match", expResult, result, 0.0);
    }

    @Test
    public void testCalculateSmallSurcharge() {
        System.out.println("Testing surcharge calculation for just over 10GB package");
        long allowedSize = TEN_GB;
        long totalDataFileSize = TEN_GB + 1L; // 10GB and one byte
        double fileSizeFeeAfter = 10.0;
        double initialSurcharge = 15.0;
        long surchargeUnitSize = GB;
        double expResult = 15.0;
        double result = PaymentSystemImpl.calculateFileSizeSurcharge(allowedSize, totalDataFileSize, fileSizeFeeAfter, initialSurcharge, surchargeUnitSize);
        assertEquals("Calculated does not match", expResult, result, 0.0);
    }

    @Test
    public void testCalculateLargeSurcharge() {
        System.out.println("Testing surcharge calculation for 25.5 GB package");
        // Total 25.5 GB
        // First 10GB is free, leaving 15.5GB to charge.
        // 1.0  GB @ 15.0
        // 14.5 GB @ 10.0 (round up to 15)
        // total should be 15.0 + (15.0 * 10.0) = 165
        long allowedSize = TEN_GB;
        long totalDataFileSize = 25L * GB + (GB / 2L);
        double fileSizeFeeAfter = 10.0;
        double initialSurcharge = 15.0;
        long surchargeUnitSize = GB;
        double expResult = 165.0;
        double result = PaymentSystemImpl.calculateFileSizeSurcharge(allowedSize, totalDataFileSize, fileSizeFeeAfter, initialSurcharge, surchargeUnitSize);
        assertEquals("Calculated does not match", expResult, result, 0.0);
    }

    @Test
    public void testCalculateTotalDiscounted() {
        System.out.println("Testing shopping cart total with discount");
        boolean discount = true;
        double fileSizeSurcharge = 15.0;
        double basicFee = 80.0;
        double expResult = 15.0;
        double result = PaymentSystemImpl.calculateTotal(discount, fileSizeSurcharge, basicFee);
        assertEquals("Calculated does not match", expResult, result, 0.0);
    }

    @Test
    public void testCalculateTotalNotDiscounted() {
        System.out.println("Testing shopping cart total without discount");
        boolean discount = false;
        double fileSizeSurcharge = 0.0;
        double basicFee = 80.0;
        double expResult = 90.0;
        double result = PaymentSystemImpl.calculateTotal(discount, fileSizeSurcharge, basicFee);
        assertEquals("Calculated does not match", expResult, result, 0.0);
    }
}
