import { testA11y } from 'cypress/support/utils';

describe('Browse By Title', () => {
    it('should pass accessibility tests', () => {
        cy.visit('/browse/title');

        // Wait for <ds-browse-by-title-page> to be visible
        cy.get('ds-browse-by-title-page').should('be.visible');

        // Analyze <ds-browse-by-title-page> for accessibility
        testA11y('ds-browse-by-title-page');
    });
});
