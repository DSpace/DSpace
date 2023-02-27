// ***********************************************
// This File is for Custom Cypress commands.
// See docs at https://docs.cypress.io/api/cypress-api/custom-commands
// ***********************************************

import { AuthTokenInfo, TOKENITEM } from 'src/app/core/auth/models/auth-token-info.model';
import { FALLBACK_TEST_REST_BASE_URL } from '.';

// Declare Cypress namespace to help with Intellisense & code completion in IDEs
// ALL custom commands MUST be listed here for code completion to work
// tslint:disable-next-line:no-namespace
declare global {
    namespace Cypress {
        interface Chainable<Subject = any> {
            /**
             * Login to backend before accessing the next page. Ensures that the next
             * call to "cy.visit()" will be authenticated as this user.
             * @param email email to login as
             * @param password password to login as
             */
            login(email: string, password: string): typeof login;

            /**
             * Login via form before accessing the next page. Useful to fill out login
             * form when a cy.visit() call is to an a page which requires authentication.
             * @param email email to login as
             * @param password password to login as
             */
             loginViaForm(email: string, password: string): typeof loginViaForm;
        }
    }
}

/**
 * Login user via REST API directly, and pass authentication token to UI via
 * the UI's dsAuthInfo cookie.
 * WARNING: WHILE THIS METHOD WORKS, OCCASIONALLY RANDOM AUTHENTICATION ERRORS OCCUR.
 * At this time "loginViaForm()" seems more consistent/stable.
 * @param email email to login as
 * @param password password to login as
 */
function login(email: string, password: string): void {
    // Cypress doesn't have access to the running application in Node.js.
    // So, it's not possible to inject or load the AppConfig or environment of the Angular UI.
    // Instead, we'll read our running application's config.json, which contains the configs &
    // is regenerated at runtime each time the Angular UI application starts up.
    cy.task('readUIConfig').then((str: string) => {
        // Parse config into a JSON object
        const config = JSON.parse(str);

        // Find the URL of our REST API. Have a fallback ready, just in case 'rest.baseUrl' cannot be found.
        let baseRestUrl = FALLBACK_TEST_REST_BASE_URL;
        if (!config.rest.baseUrl) {
            console.warn("Could not load 'rest.baseUrl' from config.json. Falling back to " + FALLBACK_TEST_REST_BASE_URL);
        } else {
            console.log("Found 'rest.baseUrl' in config.json. Using this REST API for login: " + config.rest.baseUrl);
            baseRestUrl = config.rest.baseUrl;
        }

        // To login via REST, first we have to do a GET to obtain a valid CSRF token
        cy.request( baseRestUrl + '/api/authn/status' )
        .then((response) => {
            // We should receive a CSRF token returned in a response header
            expect(response.headers).to.have.property('dspace-xsrf-token');
            const csrfToken = response.headers['dspace-xsrf-token'];

            // Now, send login POST request including that CSRF token
            cy.request({
                method: 'POST',
                url: baseRestUrl + '/api/authn/login',
                headers: { 'X-XSRF-TOKEN' : csrfToken},
                form: true, // indicates the body should be form urlencoded
                body: { user: email, password: password }
            }).then((resp) => {
                // We expect a successful login
                expect(resp.status).to.eq(200);
                // We expect to have a valid authorization header returned (with our auth token)
                expect(resp.headers).to.have.property('authorization');

                // Initialize our AuthTokenInfo object from the authorization header.
                const authheader = resp.headers.authorization as string;
                const authinfo: AuthTokenInfo = new AuthTokenInfo(authheader);

                // Save our AuthTokenInfo object to our dsAuthInfo UI cookie
                // This ensures the UI will recognize we are logged in on next "visit()"
                cy.setCookie(TOKENITEM, JSON.stringify(authinfo));
            });
        });

    });
}
// Add as a Cypress command (i.e. assign to 'cy.login')
Cypress.Commands.add('login', login);


/**
 * Login user via displayed login form
 * @param email email to login as
 * @param password password to login as
 */
 function loginViaForm(email: string, password: string): void {
    // Enter email
    cy.get('ds-log-in [data-test="email"]').type(email);
    // Enter password
    cy.get('ds-log-in [data-test="password"]').type(password);
    // Click login button
    cy.get('ds-log-in [data-test="login-button"]').click();
}
// Add as a Cypress command (i.e. assign to 'cy.loginViaForm')
Cypress.Commands.add('loginViaForm', loginViaForm);