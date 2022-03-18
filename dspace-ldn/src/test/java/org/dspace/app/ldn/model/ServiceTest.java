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
public class ServiceTest {

  @Test
  public void testService() {
    Service service = new Service();

    service.setInbox("Test");
    assertEquals("Test", service. getInbox());
  }

}
