package test;

import org.junit.Test;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
   This class is just a skeleton to determine whether JUnit is working properly.
**/
public class TestTest extends TestCase {

	@Test
	public void testAssertion() {
	    Assert.assertTrue("This is supposed to be true.", true);
	}
	
	public static void main(String... args) {
		new TestTest().testAssertion();
	}
}
