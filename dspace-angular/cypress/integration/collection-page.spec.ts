import { TEST_COLLECTION } from 'cypress/support';
import { testA11y } from 'cypress/support/utils';

describe('Collection Page', () => {

    it('should pass accessibility tests', () => {
        cy.visit('/collections/' + TEST_COLLECTION);

        // <ds-collection-page> tag must be loaded
        cy.get('ds-collection-page').should('exist');

        // Analyze <ds-collection-page> for accessibility issues
        testA11y('ds-collection-page');
    });
});
