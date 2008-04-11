package org.purl.sword.test;

import org.purl.sword.server.SWORDServer;
import org.purl.sword.server.DummyServer;

public class DummyServerTest {

	/**
	 * A main method to test the dummy SWORD server.
	 */
	public static void main(String[] args) {
		//Instantiate the dummy server
		SWORDServer ss = new DummyServer();
		
		// Test the normal service document
		System.out.println("Testing doServiceDocument():");
		System.out.println("============================");
		//System.out.println(ss.doServiceDocument(null));
		
		// Test the normal service document authenticated as 'sdl'
		System.out.println("Testing doServiceDocument(sdl):");
		System.out.println("============================");
		//System.out.println(ss.doServiceDocument("sdl"));

		// Test the 'on behalf of' service document
		System.out.println("Testing doServiceDocument(onBehalfOf):");
		System.out.println("======================================");
		//System.out.println(ss.doServiceDocument(null, "Stuart Lewis"));
		
		// Test the 'on behalf of' service document authenticated as 'sdl'
		System.out.println("Testing doServiceDocument(sdl, onBehalfOf):");
		System.out.println("======================================");
		//System.out.println(ss.doServiceDocument("sdl", "Stuart Lewis"));
	}

}
