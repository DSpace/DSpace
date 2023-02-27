import { testA11y } from 'cypress/support/utils';

describe('Browse By Author', () => {
    it('should pass accessibility tests', () => {
        cy.visit('/browse/author');

        // Wait for <ds-browse-by-metadata-page> to be visible
        cy.get('ds-browse-by-metadata-page').should('be.visible');

        // Analyze <ds-browse-by-metadata-page> for accessibility
        testA11y('ds-browse-by-metadata-page');
    });
});
