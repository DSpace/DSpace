import { Options } from 'cypress-axe';
import { TEST_ENTITY_PUBLICATION } from 'cypress/support';
import { testA11y } from 'cypress/support/utils';

describe('Item  Page', () => {
    const ITEMPAGE = '/items/' + TEST_ENTITY_PUBLICATION;
    const ENTITYPAGE = '/entities/publication/' + TEST_ENTITY_PUBLICATION;

    // Test that entities will redirect to /entities/[type]/[uuid] when accessed via /items/[uuid]
    it('should redirect to the entity page when navigating to an item page', () => {
        cy.visit(ITEMPAGE);
        cy.location('pathname').should('eq', ENTITYPAGE);
    });

    it('should pass accessibility tests', () => {
        cy.visit(ENTITYPAGE);

        // <ds-item-page> tag must be loaded
        cy.get('ds-item-page').should('exist');

        // Analyze <ds-item-page> for accessibility issues
        // Disable heading-order checks until it is fixed
        testA11y('ds-item-page',
            {
                rules: {
                    'heading-order': { enabled: false }
                }
            } as Options
        );
    });
});
