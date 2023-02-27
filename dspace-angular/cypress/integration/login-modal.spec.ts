import { TEST_ADMIN_PASSWORD, TEST_ADMIN_USER, TEST_ENTITY_PUBLICATION } from 'cypress/support';

const page = {
    openLoginMenu() {
        // Click the "Log In" dropdown menu in header
        cy.get('ds-themed-navbar [data-test="login-menu"]').click();
    },
    openUserMenu() {
        // Once logged in, click the User menu in header
        cy.get('ds-themed-navbar [data-test="user-menu"]').click();
    },
    submitLoginAndPasswordByPressingButton(email, password) {
        // Enter email
        cy.get('ds-themed-navbar [data-test="email"]').type(email);
        // Enter password
        cy.get('ds-themed-navbar [data-test="password"]').type(password);
        // Click login button
        cy.get('ds-themed-navbar [data-test="login-button"]').click();
    },
    submitLoginAndPasswordByPressingEnter(email, password) {
        // In opened Login modal, fill out email & password, then click Enter
        cy.get('ds-themed-navbar [data-test="email"]').type(email);
        cy.get('ds-themed-navbar [data-test="password"]').type(password);
        cy.get('ds-themed-navbar [data-test="password"]').type('{enter}');
    },
    submitLogoutByPressingButton() {
        // This is the POST command that will actually log us out
        cy.intercept('POST', '/server/api/authn/logout').as('logout');
        // Click logout button
        cy.get('ds-themed-navbar [data-test="logout-button"]').click();
        // Wait until above POST command responds before continuing
        // (This ensures next action waits until logout completes)
        cy.wait('@logout');
    }
};

describe('Login Modal', () => {
    it('should login when clicking button & stay on same page', () => {
        const ENTITYPAGE = '/entities/publication/' + TEST_ENTITY_PUBLICATION;
        cy.visit(ENTITYPAGE);

        // Login menu should exist
        cy.get('ds-log-in').should('exist');

        // Login, and the <ds-log-in> tag should no longer exist
        page.openLoginMenu();
        cy.get('.form-login').should('be.visible');

        page.submitLoginAndPasswordByPressingButton(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD);
        cy.get('ds-log-in').should('not.exist');

        // Verify we are still on the same page
        cy.url().should('include', ENTITYPAGE);

        // Open user menu, verify user menu & logout button now available
        page.openUserMenu();
        cy.get('ds-user-menu').should('be.visible');
        cy.get('ds-log-out').should('be.visible');
    });

    it('should login when clicking enter key & stay on same page', () => {
        cy.visit('/home');

        // Open login menu in header & verify <ds-log-in> tag is visible
        page.openLoginMenu();
        cy.get('.form-login').should('be.visible');

        // Login, and the <ds-log-in> tag should no longer exist
        page.submitLoginAndPasswordByPressingEnter(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD);
        cy.get('.form-login').should('not.exist');

        // Verify we are still on homepage
        cy.url().should('include', '/home');

        //  Open user menu, verify user menu & logout button now available
        page.openUserMenu();
        cy.get('ds-user-menu').should('be.visible');
        cy.get('ds-log-out').should('be.visible');
    });

    it('should support logout', () => {
        // First authenticate & access homepage
        cy.login(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD);
        cy.visit('/');

        // Verify ds-log-in tag doesn't exist, but ds-log-out tag does exist
        cy.get('ds-log-in').should('not.exist');
        cy.get('ds-log-out').should('exist');

        // Click logout button
        page.openUserMenu();
        page.submitLogoutByPressingButton();

        // Verify ds-log-in tag now exists
        cy.get('ds-log-in').should('exist');
        cy.get('ds-log-out').should('not.exist');
    });

    it('should allow new user registration', () => {
        cy.visit('/');

        page.openLoginMenu();

        // Registration link should be visible
        cy.get('ds-themed-navbar [data-test="register"]').should('be.visible');

        // Click registration link & you should go to registration page
        cy.get('ds-themed-navbar [data-test="register"]').click();
        cy.location('pathname').should('eq', '/register');
        cy.get('ds-register-email').should('exist');
    });

    it('should allow forgot password', () => {
        cy.visit('/');

        page.openLoginMenu();

        // Forgot password link should be visible
        cy.get('ds-themed-navbar [data-test="forgot"]').should('be.visible');

        // Click link & you should go to Forgot Password page
        cy.get('ds-themed-navbar [data-test="forgot"]').click();
        cy.location('pathname').should('eq', '/forgot');
        cy.get('ds-forgot-email').should('exist');
    });
});
