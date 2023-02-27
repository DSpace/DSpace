import { TEST_ENTITY_PUBLICATION } from 'cypress/support';
import { testA11y } from 'cypress/support/utils';

describe('Item Statistics Page', () => {
    const ITEMSTATISTICSPAGE = '/statistics/items/' + TEST_ENTITY_PUBLICATION;

    it('should load if you click on "Statistics" from an Item/Entity page', () => {
        cy.visit('/entities/publication/' + TEST_ENTITY_PUBLICATION);
        cy.get('ds-navbar ds-link-menu-item a[title="Statistics"]').click();
        cy.location('pathname').should('eq', ITEMSTATISTICSPAGE);
    });

    it('should contain element ds-item-statistics-page when navigating to an item statistics page', () => {
        cy.visit(ITEMSTATISTICSPAGE);
        cy.get('ds-item-statistics-page').should('exist');
        cy.get('ds-item-page').should('not.exist');
    });

    it('should contain a "Total visits" section', () => {
        cy.visit(ITEMSTATISTICSPAGE);
        cy.get('.' + TEST_ENTITY_PUBLICATION + '_TotalVisits').should('exist');
    });

    it('should contain a "Total visits per month" section', () => {
        cy.visit(ITEMSTATISTICSPAGE);
        cy.get('.' + TEST_ENTITY_PUBLICATION + '_TotalVisitsPerMonth').should('exist');
    });

    it('should pass accessibility tests', () => {
        cy.visit(ITEMSTATISTICSPAGE);

        // <ds-item-statistics-page> tag must be loaded
        cy.get('ds-item-statistics-page').should('exist');

        // Analyze <ds-item-statistics-page> for accessibility issues
        testA11y('ds-item-statistics-page');
    });
});
