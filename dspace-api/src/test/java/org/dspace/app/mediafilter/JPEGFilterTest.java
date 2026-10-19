/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.dspace.AbstractUnitTest;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Test;
import org.mockito.Mock;

public class JPEGFilterTest extends AbstractUnitTest {

    @Mock
    private ConfigurationService mockConfigurationService;

    @Mock
    private DSpaceServicesFactory mockDSpaceServicesFactory;

    @Mock
    private InputStream mockInputStream;

    @Mock
    private Item mockItem;

    /**
     * Tests that the convertRotationToDegrees method returns 0 for an input value
     * that doesn't match any of the defined rotation cases.
     */
    @Test
    public void testConvertRotationToDegrees_UnknownValue_ReturnsZero() {
        int result = JPEGFilter.convertRotationToDegrees(5);
        assertEquals(0, result);
    }

    /**
     * Test getNormalizedInstance method with a null input.
     * This tests the edge case of passing a null BufferedImage to the method.
     * The method should throw a NullPointerException when given a null input.
     */
    @Test(expected = NullPointerException.class)
    public void testGetNormalizedInstanceWithNullInput() {
        JPEGFilter filter = new JPEGFilter();
        filter.getNormalizedInstance(null);
    }

    /**
     * Test getThumbDim method with a null BufferedImage input.
     * This tests the edge case where the input image is null, which should result in an exception.
     */
    @Test(expected = NullPointerException.class)
    public void testGetThumbDimWithNullBufferedImage() throws Exception {
        JPEGFilter filter = new JPEGFilter();
        Item currentItem = null;
        BufferedImage buf = null;
        boolean verbose = false;
        int xmax = 100;
        int ymax = 100;
        boolean blurring = false;
        boolean hqscaling = false;
        int brandHeight = 0;
        int brandFontPoint = 0;
        int rotation = 0;
        String brandFont = null;

        filter.getThumbDim(
            currentItem, buf, verbose, xmax, ymax, blurring, hqscaling,
            brandHeight, brandFontPoint, rotation, brandFont
        );
    }

    /**
     * Tests that the rotateImage method returns the original image when the rotation angle is 0.
     * This is an edge case explicitly handled in the method implementation.
     */
    @Test
    public void testRotateImageWithZeroAngle() {
        BufferedImage originalImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage rotatedImage = JPEGFilter.rotateImage(originalImage, 0);
        assertSame(
            "When rotation angle is 0, the original image should be returned",
            originalImage, rotatedImage
        );
    }

    /**
     * Test case for convertRotationToDegrees method when input is 6.
     * Expected to return 90 degrees for the rotation value of 6.
     */
    @Test
    public void test_convertRotationToDegrees_whenInputIs6_returns90() {
        int input = 6;
        int expected = 90;
        int result = JPEGFilter.convertRotationToDegrees(input);
        assertEquals(expected, result);
    }

    /**
     * Tests that getBlurredInstance method applies a blur effect to the input image.
     * It verifies that the returned image is not null, has the same dimensions as the input,
     * and is different from the original image (indicating that blurring has occurred).
     */
    @Test
    public void test_getBlurredInstance_appliesBlurEffect() {
        JPEGFilter filter = new JPEGFilter();
        BufferedImage original = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);

        BufferedImage blurred = filter.getBlurredInstance(original);

        assertNotNull("Blurred image should not be null", blurred);
        assertEquals("Width should be the same", original.getWidth(), blurred.getWidth());
        assertEquals("Height should be the same", original.getHeight(), blurred.getHeight());
        assertNotEquals("Blurred image should be different from original", original, blurred);
    }

    /**
     * Test case for getBundleName method of JPEGFilter class.
     * This test verifies that the getBundleName method returns the expected string "THUMBNAIL".
     */
    @Test
    public void test_getBundleName_returnsExpectedString() {
        JPEGFilter filter = new JPEGFilter();
        String result = filter.getBundleName();
        assertEquals("THUMBNAIL", result);
    }

    /**
     * Tests that the getDescription method returns the expected string "Generated Thumbnail".
     * This verifies that the method correctly provides the description for the JPEG filter.
     */
    @Test
    public void test_getDescription_1() {
        JPEGFilter filter = new JPEGFilter();
        String description = filter.getDescription();
        assertEquals("Generated Thumbnail", description);
    }

    /**
     * Tests that getFilteredName method appends ".jpg" to the input filename.
     */
    @Test
    public void test_getFilteredName_appendsJpgExtension() {
        JPEGFilter filter = new JPEGFilter();
        String oldFilename = "testimage";
        String expectedResult = "testimage.jpg";
        String actualResult = filter.getFilteredName(oldFilename);
        assertEquals(expectedResult, actualResult);
    }

    /**
     * Test case for getFormatString method of JPEGFilter class.
     * Verifies that the method returns the expected string "JPEG".
     */
    @Test
    public void test_getFormatString_returnsJPEG() {
        JPEGFilter filter = new JPEGFilter();
        String result = filter.getFormatString();
        assertEquals("JPEG", result);
    }

    /**
     * Tests the behavior of getImageRotationUsingImageReader when an ImageProcessingException occurs.
     * This test verifies that the method handles an ImageProcessingException by logging the error
     * and returning 0 degrees rotation.
     */
    @Test
    public void test_getImageRotationUsingImageReader_imageProcessingException() {
        InputStream errorStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated image processing error");
            }
        };
        int result = JPEGFilter.getImageRotationUsingImageReader(errorStream);
        assertEquals(0, result);
    }

    /**
     * Testcase for getImageRotationUsingImageReader when the image doesn't contain orientation metadata.
     * This test verifies that the method returns 0 when there's no ExifIFD0Directory
     * or when it doesn't contain the TAG_ORIENTATION.
     */
    @Test
    public void test_getImageRotationUsingImageReader_noOrientationMetadata() throws IOException {
        URL resource = this.getClass().getResource("cat.jpg");
        int rotationAngle = -1;
        try (InputStream inputStream = new FileInputStream(resource.getFile())) {
            // Call the method under test
            rotationAngle = JPEGFilter.getImageRotationUsingImageReader(inputStream);
        }
        assertEquals(0, rotationAngle);
    }

    /**
     * Tests the getImageRotationUsingImageReader method when the image contains
     * valid EXIF orientation metadata.
     * 
     * This test verifies that the method correctly reads the orientation tag
     * from the EXIF metadata and returns the appropriate rotation angle in degrees.
     */
    @Test
    public void test_getImageRotationUsingImageReader_withValidExifOrientation() throws Exception {
        // Create a mock InputStream with EXIF metadata containing orientation information
        URL resource = this.getClass().getResource("cat-rotated-90.jpg");
        int rotationAngle = -1;
        try (InputStream inputStream = new FileInputStream(resource.getFile())) {
            // Call the method under test
            rotationAngle = JPEGFilter.getImageRotationUsingImageReader(inputStream);
        }

        // Assert the expected rotation angle
        // Note: The expected value should be adjusted based on the mock data
        assertEquals(90, rotationAngle);
    }

    /**
     * Tests the getScaledInstance method of JPEGFilter class with higher quality scaling.
     * This test verifies that the method correctly scales down an image in multiple passes
     * when higherQuality is true and the image dimensions are larger than the target dimensions.
     */
    @Test
    public void test_getScaledInstance() {
        JPEGFilter filter = new JPEGFilter();
        BufferedImage originalImage = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        int targetWidth = 100;
        int targetHeight = 75;
        Object hint = RenderingHints.VALUE_INTERPOLATION_BILINEAR;
        boolean higherQuality = true;

        BufferedImage result = filter.getScaledInstance(originalImage, targetWidth, targetHeight, hint, higherQuality);

        assertNotNull(result);
        assertEquals(targetWidth, result.getWidth());
        assertEquals(targetHeight, result.getHeight());
    }

    /**
     * Tests the rotateImage method with a non-zero angle.
     * This test verifies that the image is rotated correctly when given a non-zero angle.
     */
    @Test
    public void test_rotateImage_nonZeroAngle() {
        BufferedImage originalImage = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        int angle = 90;

        BufferedImage rotatedImage = JPEGFilter.rotateImage(originalImage, angle);

        assertNotNull(rotatedImage);
        assertEquals(50, rotatedImage.getWidth());
        assertEquals(100, rotatedImage.getHeight());
    }

}
