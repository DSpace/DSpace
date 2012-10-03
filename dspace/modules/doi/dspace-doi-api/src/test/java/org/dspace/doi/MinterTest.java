package org.dspace.doi;

import org.junit.Test;

public class MinterTest {

	private static Minter myMinter;

    /*	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.setProperty("doi.db.path", "src/test/resources/doi.db");
		myMinter = new Minter();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		myMinter.close();
	}

    */
	@Test
	public void testRegister() {
//		try {
//			DOI doi = new DOI("doi:10.5061/dryad.1005", new URL(
//					"http://datadryad.org/handle/10255/dryad.1005"));
//			
//			// First, remove it so we can add it and register it
//			if (!myMinter.remove(doi)) {
//				fail("Failed to remove registered DOI: " + doi.toString());
//			}
//			
//			try {
//				if (myMinter.register(doi) == null) {
//					fail("Failed to register DOI: " + doi.toString());
//				}
//			}
//			catch (IOException details) {
//				fail(details.getMessage());
//			}
//		}
//		catch (MalformedURLException details) {
//			fail(details.getMessage());
//		}
	}

	@Test
	public void testMintDOI() {
/*		String url1 = "http://datadryad.org/handle/10255/dryad.1005";
		String url2 = "http://hdl.handle.net/10255/dryad.1005";
		String doi = "doi:10.5061/dryad.1005";

		// Test getting a DOI from the database (already existing)
		String result1 = myMinter.calculateDOI(url1).toString();

		if (!doi.equals(result1)) {
			fail("Failed to return minted DOI: " + doi + "; returned: "
					+ result1);
		}

		// Now, remove the pre-existing DOI so we can test adding it
		try {
			myMinter.remove(new DOI(doi, new URL(url1)));
			result1 = myMinter.calculateDOI(url1).toString();

			if (!doi.equals(result1)) {
				fail("Failed to mint DOI: " + doi + "; returned: " + result1);
			}
		}
		catch (MalformedURLException details) {
			fail("Unit test doesn't use a valid URL");
		}

		// Now, do this with a hdl URL to test the handle parsing
		try {
			myMinter.remove(new DOI(doi, new URL(url2)));
			result1 = myMinter.calculateDOI(url2).toString();

			if (!doi.equals(result1)) {
				fail("Failed to mint DOI: " + doi + "; returned: " + result1);
			}
		}
		catch (MalformedURLException details) {
			fail("Unit test doesn't use a valid URL");
		}*/
	}

	@Test
	public void testGetKnownDOI() {
	    /*		// Make sure our DOI is in the system
		myMinter.calculateDOI("http://datadryad.org/handle/10255/dryad.1637");
		
		// Now, check that we can get it via getKnownDOI
		DOI doi = myMinter.getKnownDOI("doi:10.5061/dryad.1637");
		String url = doi.getTargetURL().toExternalForm();

		if (!doi.toString().equals("doi:10.5061/dryad.1637")) {
			fail("Failed to retrieve requested DOI");
		}

		if (!url.equals("http://datadryad.org/handle/10255/dryad.1637")) {
			fail("Failed to get the correct URL from the requested DOI: " + url);
		}
	    */
	}

	@Test
	public void testDump() {
	    /*
		try {		   
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			myMinter.dump(outStream);
			String db = new String(outStream.toByteArray(), "UTF-8").trim();
			
			if (!db.startsWith("doi:10.5061/dryad.100")) {
				fail("db didn't start with expected DOI: doi:10.5061/dryad.100");
			}

			if (!db.endsWith("http://datadryad.org/handle/10255/dryad.1003")) {
				fail("db didn't end with expected DOI: http://datadryad.org/handle/10255/dryad.1003");
			}
		}
		catch (Exception details) {
			fail(details.getMessage());
		}
	    */
	}

	@Test
	public void testRemoveDOI() {
//		try {
//			myMinter.remove(null);
//			fail("Failed to catch a NullPointerException when removing a null DOI");
//		}
//		catch (NullPointerException details) {
//			// it should throw an exception, so ignore for testing purposes
//		}
	}

	@Test
	public void testCount() {
		// FIXME: we need the database DOIs to be able to change
		// think of another way to test... this isn't clean enough
//		try {
//			int count = myMinter.count();
//
//			if (1102 != count) {
//				fail("Expected 1102 records but found " + count);
//			}
//		}
//		catch (IOException details) {
//			fail(details.getMessage());
//		}
	}

}
