import { testA11y } from 'cypress/support/utils';

describe('Header', () => {
    it('should pass accessibility tests', () => {
        cy.visit('/');

        // Header must first be visible
        cy.get('ds-header').should('be.visible');

        // Analyze <ds-header> for accessibility
        testA11y({
            include: ['ds-header'],
            exclude: [
                ['#search-navbar-container'], // search in navbar has duplicative ID. Will be fixed in #1174
                ['.dropdownLogin']            // "Log in" link has color contrast issues. Will be fixed in #1149
            ],
        });
    });
});
