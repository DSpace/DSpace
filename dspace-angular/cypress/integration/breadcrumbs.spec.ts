import { TEST_ENTITY_PUBLICATION } from 'cypress/support';
import { testA11y } from 'cypress/support/utils';

describe('Breadcrumbs', () => {
    it('should pass accessibility tests', () => {
        // Visit an Item, as those have more breadcrumbs
        cy.visit('/entities/publication/' + TEST_ENTITY_PUBLICATION);

        // Wait for breadcrumbs to be visible
        cy.get('ds-breadcrumbs').should('be.visible');

        // Analyze <ds-breadcrumbs> for accessibility
        testA11y('ds-breadcrumbs');
    });
});
