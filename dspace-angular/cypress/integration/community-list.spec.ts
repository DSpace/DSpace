import { Options } from 'cypress-axe';
import { testA11y } from 'cypress/support/utils';

describe('Community List Page', () => {

    it('should pass accessibility tests', () => {
        cy.visit('/community-list');

        // <ds-community-list-page> tag must be loaded
        cy.get('ds-community-list-page').should('exist');

        // Open first Community (to show Collections)...that way we scan sub-elements as well
        cy.get('ds-community-list :nth-child(1) > .btn-group > .btn').click();

        // Analyze <ds-community-list-page> for accessibility issues
        // Disable heading-order checks until it is fixed
        testA11y('ds-community-list-page',
            {
                rules: {
                    'heading-order': { enabled: false }
                }
            } as Options
        );
    });
});
