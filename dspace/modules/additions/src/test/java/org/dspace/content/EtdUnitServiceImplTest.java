/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EtdUnitService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * EtdUnit tests for the EtdUnitServiceImpl class
 */
public class EtdUnitServiceImplTest extends AbstractUnitTest {
  private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(EtdUnitServiceImplTest.class);

  private EtdUnit etdunit1;
  private EtdUnit etdunit2;
  private EtdUnit etdunit3;
  private Collection collection1;
  private Collection collection2;

  protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
  protected EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();

  /**
   * This method will be run before every test as per @Before. It will
   * initialize resources required for the tests.
   *
   * Other methods can be annotated with @Before here or in subclasses
   * but no execution order is guaranteed
   */
  @Before
  @Override
  public void init() {
    super.init();
    try {
      etdunit1 = EtdUnitTestUtils.createEtdUnit(context, "EtdUnit One", true);
      etdunit2 = EtdUnitTestUtils.createEtdUnit(context, "EtdUnit Two", false);
      etdunit3 = EtdUnitTestUtils.createEtdUnit(context, "EtdUnit Three", false);
      collection1 = CollectionBuilder.createCollection(context, null).build();
      collection2 = CollectionBuilder.createCollection(context, null).build();
    } catch (SQLException ex) {
      log.error("SQL Error in init", ex);
      fail("SQL Error in init: " + ex.getMessage());
    } catch (AuthorizeException ex) {
      log.error("Authorization Error in init", ex);
      fail("Authorization Error in init: " + ex.getMessage());
    }
  }

  @After
  @Override
  public void destroy() {
    try {
      EtdUnitTestUtils.deleteEtdUnit(context, etdunit3);
      EtdUnitTestUtils.deleteEtdUnit(context, etdunit2);
      EtdUnitTestUtils.deleteEtdUnit(context, etdunit1);
      super.destroy();
    } catch (SQLException ex) {
      log.error("SQL Error in init", ex);
      fail("SQL Error in init: " + ex.getMessage());
    } catch (AuthorizeException ex) {
      log.error("Authorization Error in init", ex);
      fail("Authorization Error in init: " + ex.getMessage());
    } catch (IOException ex) {
      log.error("IO Error in init", ex);
      fail("IO Error in init: " + ex.getMessage());
    }
  }

  @Test
  public void testCreateEtdUnit() throws SQLException, AuthorizeException, IOException {
    EtdUnit etdunit = null;
    try {
      context.turnOffAuthorisationSystem();
      etdunit = etdunitService.create(context);
      assertThat("testCreateEtdUnit", etdunit, notNullValue());
    } finally {
      if (etdunit != null) {
        etdunitService.delete(context, etdunit);
      }
      context.restoreAuthSystemState();
    }
  }

  @Test(expected = AuthorizeException.class)
  public void createEtdUnitUnAuthorized() throws SQLException, AuthorizeException {
    context.setCurrentUser(null);
    etdunitService.create(context);
  }

  @Test
  public void setEtdUnitName() throws SQLException, AuthorizeException {
    etdunit1.setName("new name");
    try {
      context.turnOffAuthorisationSystem();
      etdunitService.update(context, etdunit1);
    } finally {
      context.restoreAuthSystemState();
    }
    assertThat("setEtdUnitName 1", etdunit1.getName(), notNullValue());
    assertThat("setEtdUnitName 2", etdunit1.getName(), equalTo("new name"));
  }

  @Test
  public void findByName() throws SQLException {
    EtdUnit etdunit = etdunitService.findByName(context, "EtdUnit One");
    assertThat("findByName 1", etdunit, notNullValue());
    assertThat("findByName 2", etdunit.getName(), notNullValue());
    assertThat("findByName 2", etdunit.getName(), equalTo("EtdUnit One"));
  }

  @Test
  public void findAll() throws SQLException {
    List<EtdUnit> etdunits = etdunitService.findAll(context, -1, -1);
    assertThat("findAll 1", etdunits, notNullValue());
    assertTrue("findAll 2", !etdunits.isEmpty());
  }

  @Test
  public void findAllNameSort() throws SQLException {
    // Retrieve etdunits sorted by name
    List<EtdUnit> etdunits = etdunitService.findAll(context, -1, -1);

    assertThat("findAllNameSort 1", etdunits, notNullValue());

    // Add all etdunit names to two arraylists (arraylists are unsorted)
    // NOTE: we use lists here because we don't want duplicate names removed
    List<String> names = new ArrayList<>();
    List<String> sortedNames = new ArrayList<>();
    for (EtdUnit etdunit : etdunits) {
      // Ignore any unnamed etdunits. This is only necessary when running etdunit
      // tests via
      // a persistent database
      // (e.g. Postgres) as unnamed groups may be created by other tests.
      if (etdunit.getName() == null) {
        continue;
      }
      names.add(etdunit.getName());
      sortedNames.add(etdunit.getName());
    }

    // Now, sort the "sortedNames" Arraylist
    Collections.sort(sortedNames);

    // Verify the sorted arraylist is still equal to the original (unsorted) one
    assertThat("findAllNameSort compareLists", names, equalTo(sortedNames));
  }

  @Test
  public void searchByName() throws SQLException, AuthorizeException {
    // We can find 2 etdunits so attempt to retrieve with offset 0 and a max of one
    List<EtdUnit> etdunits = etdunitService.search(context, "EtdUnit T", 0, 1);
    assertThat("search 1", etdunits, notNullValue());
    assertThat("search 2", etdunits.size(), equalTo(1));
    String firstEtdUnitName = etdunits.iterator().next().getName();
    assertTrue("search 3", firstEtdUnitName.equals("EtdUnit Two") || firstEtdUnitName.equals("EtdUnit Three"));

    // Retrieve the second etdunit
    etdunits = etdunitService.search(context, "EtdUnit T", 1, 2);
    assertThat("search 4", etdunits, notNullValue());
    assertThat("search 5", etdunits.size(), equalTo(1));
    String secondEtdUnitName = etdunits.iterator().next().getName();
    assertTrue("search 6", secondEtdUnitName.equals("EtdUnit Two") || secondEtdUnitName.equals("EtdUnit Three"));
  }

  @Test
  public void searchByID() throws SQLException {
    List<EtdUnit> searchResult = etdunitService.search(context, String.valueOf(etdunit1.getID()), 0, 10);
    assertThat("searchID 1", searchResult.size(), equalTo(1));
    assertThat("searchID 2", searchResult.iterator().next(), equalTo(etdunit1));
  }

  @Test
  public void searchByNameResultCount() throws SQLException {
    assertThat("searchByNameResultCount", etdunitService.searchResultCount(context, "EtdUnit T"), equalTo(2));
  }

  @Test
  public void searchByIdResultCount() throws SQLException {
    assertThat("searchByIdResultCount",
        etdunitService.searchResultCount(context, String.valueOf(etdunit1.getID())), equalTo(1));
  }

  @Test
  public void addCollection() throws SQLException, AuthorizeException, IOException {
    context.turnOffAuthorisationSystem();
    etdunitService.addCollection(context, etdunit1, collection1);
    etdunit1 = context.reloadEntity(etdunit1);
    assertTrue(etdunit1.isMember(collection1));
    context.restoreAuthSystemState();
  }

  @Test
  public void removeCollection() throws SQLException, AuthorizeException {
    context.turnOffAuthorisationSystem();
    etdunitService.addCollection(context, etdunit1, collection1);
    etdunit1 = context.reloadEntity(etdunit1);
    assertTrue(etdunit1.isMember(collection1));

    etdunitService.removeCollection(context, etdunit1, collection1);
    etdunit1 = context.reloadEntity(etdunit1);
    assertFalse(etdunit1.isMember(collection1));

    context.restoreAuthSystemState();
  }

  @Test
  public void getAllCollections() throws SQLException, AuthorizeException, IOException {
    context.turnOffAuthorisationSystem();
    etdunitService.addCollection(context, etdunit1, collection1);
    etdunitService.addCollection(context, etdunit1, collection2);

    etdunit1 = context.reloadEntity(etdunit1);

    assertTrue(etdunit1.isMember(collection1));
    assertTrue(etdunit1.isMember(collection2));

    List<Collection> allGroups = etdunitService.getAllCollections(context, etdunit1);

    assertTrue(allGroups.containsAll(Arrays.asList(collection1, collection2)));

    context.restoreAuthSystemState();
  }
}
