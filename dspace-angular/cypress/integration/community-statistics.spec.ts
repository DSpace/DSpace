import { TEST_COMMUNITY } from 'cypress/support';
import { testA11y } from 'cypress/support/utils';

describe('Community Statistics Page', () => {
    const COMMUNITYSTATISTICSPAGE = '/statistics/communities/' + TEST_COMMUNITY;

    it('should load if you click on "Statistics" from a Community page', () => {
        cy.visit('/communities/' + TEST_COMMUNITY);
        cy.get('ds-navbar ds-link-menu-item a[title="Statistics"]').click();
        cy.location('pathname').should('eq', COMMUNITYSTATISTICSPAGE);
    });

    it('should contain a "Total visits" section', () => {
        cy.visit(COMMUNITYSTATISTICSPAGE);
        cy.get('.' + TEST_COMMUNITY + '_TotalVisits').should('exist');
    });

    it('should contain a "Total visits per month" section', () => {
        cy.visit(COMMUNITYSTATISTICSPAGE);
        cy.get('.' + TEST_COMMUNITY + '_TotalVisitsPerMonth').should('exist');
    });

    it('should pass accessibility tests', () => {
        cy.visit(COMMUNITYSTATISTICSPAGE);

        // <ds-community-statistics-page> tag must be loaded
        cy.get('ds-community-statistics-page').should('exist');

        // Analyze <ds-community-statistics-page> for accessibility issues
        testA11y('ds-community-statistics-page');
    });
});
