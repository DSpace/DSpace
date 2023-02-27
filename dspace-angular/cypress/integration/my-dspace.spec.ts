import { Options } from 'cypress-axe';
import { TEST_SUBMIT_USER, TEST_SUBMIT_USER_PASSWORD, TEST_SUBMIT_COLLECTION_NAME } from 'cypress/support';
import { testA11y } from 'cypress/support/utils';

describe('My DSpace page', () => {
    it('should display recent submissions and pass accessibility tests', () => {
        cy.visit('/mydspace');

        // This page is restricted, so we will be shown the login form. Fill it out & submit.
        cy.loginViaForm(TEST_SUBMIT_USER, TEST_SUBMIT_USER_PASSWORD);

        cy.get('ds-my-dspace-page').should('exist');

        // At least one recent submission should be displayed
        cy.get('[data-test="list-object"]').should('be.visible');

        // Click each filter toggle to open *every* filter
        // (As we want to scan filter section for accessibility issues as well)
        cy.get('.filter-toggle').click({ multiple: true });

        // Analyze <ds-my-dspace-page> for accessibility issues
        testA11y(
            {
                include: ['ds-my-dspace-page'],
                exclude: [
                    ['nouislider'] // Date filter slider is missing ARIA labels. Will be fixed by #1175
                ],
            },
            {
                rules: {
                    // Search filters fail these two "moderate" impact rules
                    'heading-order': { enabled: false },
                    'landmark-unique': { enabled: false }
                }
            } as Options
        );
    });

    it('should have a working detailed view that passes accessibility tests', () => {
        cy.visit('/mydspace');

        // This page is restricted, so we will be shown the login form. Fill it out & submit.
        cy.loginViaForm(TEST_SUBMIT_USER, TEST_SUBMIT_USER_PASSWORD);

        cy.get('ds-my-dspace-page').should('exist');

        // Click button in sidebar to display detailed view
        cy.get('ds-search-sidebar [data-test="detail-view"]').click();

        cy.get('ds-object-detail').should('exist');

        // Analyze <ds-search-page> for accessibility issues
        testA11y('ds-my-dspace-page',
            {
                rules: {
                    // Search filters fail these two "moderate" impact rules
                    'heading-order': { enabled: false },
                    'landmark-unique': { enabled: false }
                }
            } as Options
        );
    });

    // NOTE: Deleting existing submissions is exercised by submission.spec.ts
    it('should let you start a new submission & edit in-progress submissions', () => {
        cy.visit('/mydspace');

        // This page is restricted, so we will be shown the login form. Fill it out & submit.
        cy.loginViaForm(TEST_SUBMIT_USER, TEST_SUBMIT_USER_PASSWORD);

        // Open the New Submission dropdown
        cy.get('button[data-test="submission-dropdown"]').click();
        // Click on the "Item" type in that dropdown
        cy.get('#entityControlsDropdownMenu button[title="none"]').click();

        // This should display the <ds-create-item-parent-selector> (popup window)
        cy.get('ds-create-item-parent-selector').should('be.visible');

        // Type in a known Collection name in the search box
        cy.get('ds-authorized-collection-selector input[type="search"]').type(TEST_SUBMIT_COLLECTION_NAME);

        // Click on the button matching that known Collection name
        cy.get('ds-authorized-collection-selector button[title="' + TEST_SUBMIT_COLLECTION_NAME + '"]').click();

        // New URL should include /workspaceitems, as we've started a new submission
        cy.url().should('include', '/workspaceitems');

        // The Submission edit form tag should be visible
        cy.get('ds-submission-edit').should('be.visible');

        // A Collection menu button should exist & its value should be the selected collection
        cy.get('#collectionControlsMenuButton span').should('have.text', TEST_SUBMIT_COLLECTION_NAME);

        // Now that we've created a submission, we'll test that we can go back and Edit it.
        // Get our Submission URL, to parse out the ID of this new submission
        cy.location().then(fullUrl => {
            // This will be the full path (/workspaceitems/[id]/edit)
            const path = fullUrl.pathname;
            // Split on the slashes
            const subpaths = path.split('/');
            // Part 2 will be the [id] of the submission
            const id = subpaths[2];

            // Click the "Save for Later" button to save this submission
            cy.get('ds-submission-form-footer [data-test="save-for-later"]').click();

            // "Save for Later" should send us to MyDSpace
            cy.url().should('include', '/mydspace');

            // Close any open notifications, to make sure they don't get in the way of next steps
            cy.get('[data-dismiss="alert"]').click({multiple: true});

            // This is the GET command that will actually run the search
            cy.intercept('GET', '/server/api/discover/search/objects*').as('search-results');
            // On MyDSpace, find the submission we just created via its ID
            cy.get('[data-test="search-box"]').type(id);
            cy.get('[data-test="search-button"]').click();

            // Wait for search results to come back from the above GET command
            cy.wait('@search-results');

            // Click the Edit button for this in-progress submission
            cy.get('#edit_' + id).click();

            // Should send us back to the submission form
            cy.url().should('include', '/workspaceitems/' + id + '/edit');

            // Discard our new submission by clicking Discard in Submission form & confirming
            cy.get('ds-submission-form-footer [data-test="discard"]').click();
            cy.get('button#discard_submit').click();

            // Discarding should send us back to MyDSpace
            cy.url().should('include', '/mydspace');
        });
    });

    it('should let you import from external sources', () => {
        cy.visit('/mydspace');

        // This page is restricted, so we will be shown the login form. Fill it out & submit.
        cy.loginViaForm(TEST_SUBMIT_USER, TEST_SUBMIT_USER_PASSWORD);

        // Open the New Import dropdown
        cy.get('button[data-test="import-dropdown"]').click();
        // Click on the "Item" type in that dropdown
        cy.get('#importControlsDropdownMenu button[title="none"]').click();

        // New URL should include /import-external, as we've moved to the import page
        cy.url().should('include', '/import-external');

        // The external import searchbox should be visible
        cy.get('ds-submission-import-external-searchbar').should('be.visible');
    });

});
