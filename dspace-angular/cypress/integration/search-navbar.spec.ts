import { TEST_SEARCH_TERM } from 'cypress/support';

const page = {
    fillOutQueryInNavBar(query) {
        // Click the magnifying glass
        cy.get('ds-themed-navbar [data-test="header-search-icon"]').click();
        // Fill out a query in input that appears
        cy.get('ds-themed-navbar [data-test="header-search-box"]').type(query);
    },
    submitQueryByPressingEnter() {
        cy.get('ds-themed-navbar [data-test="header-search-box"]').type('{enter}');
    },
    submitQueryByPressingIcon() {
        cy.get('ds-themed-navbar [data-test="header-search-icon"]').click();
    }
};

describe('Search from Navigation Bar', () => {
    // NOTE: these tests currently assume this query will return results!
    const query = TEST_SEARCH_TERM;

    it('should go to search page with correct query if submitted (from home)', () => {
        cy.visit('/');
        // This is the GET command that will actually run the search
        cy.intercept('GET', '/server/api/discover/search/objects*').as('search-results');
        // Run the search
        page.fillOutQueryInNavBar(query);
        page.submitQueryByPressingEnter();
        // New URL should include query param
        cy.url().should('include', 'query=' + query);
        // Wait for search results to come back from the above GET command
        cy.wait('@search-results');
        // At least one search result should be displayed
        cy.get('[data-test="list-object"]').should('be.visible');
    });

    it('should go to search page with correct query if submitted (from search)', () => {
        cy.visit('/search');
        // This is the GET command that will actually run the search
        cy.intercept('GET', '/server/api/discover/search/objects*').as('search-results');
        // Run the search
        page.fillOutQueryInNavBar(query);
        page.submitQueryByPressingEnter();
        // New URL should include query param
        cy.url().should('include', 'query=' + query);
        // Wait for search results to come back from the above GET command
        cy.wait('@search-results');
        // At least one search result should be displayed
        cy.get('[data-test="list-object"]').should('be.visible');
    });

    it('should allow user to also submit query by clicking icon', () => {
        cy.visit('/');
        // This is the GET command that will actually run the search
        cy.intercept('GET', '/server/api/discover/search/objects*').as('search-results');
        // Run the search
        page.fillOutQueryInNavBar(query);
        page.submitQueryByPressingIcon();
        // New URL should include query param
        cy.url().should('include', 'query=' + query);
        // Wait for search results to come back from the above GET command
        cy.wait('@search-results');
        // At least one search result should be displayed
        cy.get('[data-test="list-object"]').should('be.visible');
    });
});
