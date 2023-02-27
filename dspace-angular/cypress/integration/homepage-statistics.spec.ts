import { testA11y } from 'cypress/support/utils';

describe('Site Statistics Page', () => {
    it('should load if you click on "Statistics" from homepage', () => {
        cy.visit('/');
        cy.get('ds-navbar ds-link-menu-item a[title="Statistics"]').click();
        cy.location('pathname').should('eq', '/statistics');
    });

    it('should pass accessibility tests', () => {
        cy.visit('/statistics');

        // <ds-site-statistics-page> tag must be loaded
        cy.get('ds-site-statistics-page').should('exist');

        // Analyze <ds-site-statistics-page> for accessibility issues
        testA11y('ds-site-statistics-page');
    });
});
