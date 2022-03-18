/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class ObjectTest {

  @Test
  public void testObject() {
    Object object = new Object();

    object.setIetfCiteAs("Test");
    assertEquals("Test", object.getIetfCiteAs());

    object.setTitle("Test");
    assertEquals("Test", object.getTitle());

    Url url = new Url();
    url.setId("4af4d9d5-c5c4-464a-b310-f0124c191928");
    object.setUrl(url);
    assertEquals(url, object.getUrl());
  }

}
