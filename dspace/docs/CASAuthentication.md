# CAS/LDAP Authentication

This document contains information about setting up CAS/LDAP authentication for
DSpace 7.

In the current implementation, CAS is used to authenticate users, while LDAP is
used to retrieve group information and for registering news users (i.e., create
a new EPerson for a user, if they are not already known by DRUM).

## Configuration

**Note** In the configuraton files, commas in values are treated as separators,
and so must be escaped if part of the value. For example, if the value for the
LDAP bind auth property ("drum.ldap.bind.auth") is is

```text
uid=foo,cn=bar,ou=baz,dc=quuz,dc=zot
```

The configuration entry would be:

```text
drum.ldap.bind.auth = uid=foo\,cn=bar\,ou=baz\,dc=quuz\,dc=zot
```

The following configuration properties are used for CAS/LDAP authentication:

* drum.cas.server.url - The URL to the CAS server, typically
    "https://login.umd.edu/cas/login"
* drum.cas.validate.url = The URL for CAS service ticket validation, typically
    "https://login.umd.edu/cas/serviceValidate"
* drum.cas.logout.url - The URL to the CAS logout endpoint.
    **Not currently used**
* drum.webui.cas.autoregister - "true" if an EPerson should be automatically
    created on a succesful login, "false" otherwise.

* drum.ldap.url - URL to the LDAP server to use for retrieving user information,
    typically "ldap://directory.umd.edu:636/dc=umd\,dc=edu"
* drum.ldap.bind.auth - LDAP bind authority
* drum.ldap.bind.password - the password for the LDAP bind authority
* drum.ldap.connect.timeout - number of milliseconds to wait for an LDAP
    connection.
* drum.ldap.read.timeout - number of milliseconds to wait for an LDAP response
    to a request.

## CAS/LDAP Authentication Workflow

The following describes in general terms the CAS authentication workflow. This
workflow is similar to the workflow for the DSpace Shibboleth implementation.

The overall CAS login process is as follows:

1) The user selects the CAS login method from the login page, redirecting
   the user to the CAS server. The URL will typically have the form:

   ```text
   <CAS_LOGIN_URL>?service=<SERVICE_URL>?redirectUrl=<REDIRECT_URL>
   ```

   where <CAS_LOGIN_URL> is the "drum.cas.server.url", <SERVICE_URL> is the URL
   the CAS server should return to after successful authentication (configured
   with a URL path of "/api/authn/cas" in
   `org.dspace.app.rest.security.WebSecurityConfiguration`) and the
   "redirectUrl" is the URL the browser should be redirected to when
   authentication is complete. The "service" and "redirectURl" are full URLs,
   with the "host" portion being the DSpace server URL.

   **Note:** For the DSpace backend application, the path of the "redirectUrl"
   should be "/server/login.html" to ensure that the temporary
   authentication cookie is properly converted into a HTTP header.

2. The browser is redirected to the CAS login URL.

3. User logs in using CAS

4. Once successfully authenticated, CAS redirects the browser to the "server"
   URL, where the `org.dspace.app.rest.security.CasLoginFilter` is
   "listening". The CasLoginFilter uses the
   `org.dspace.authenticate.CASAuthentication` class to validate the CAS
   response (using the "drum.cas.validate.url" property to determine the URL).
   LDAP is also queried to retrieve a user's groups, and also used to register
   a user with DRUM, if they do not already exist in the system.

5. The "successfulAuthentication" method in the CasLoginFilter then generates a
   JWT token, and stores in it in a *temporary* authentication cookie. (A cookie
   is needed because headers are ignored on redirect).

6. This filter then redirects the browser using the "redirectUrl" (after
   verifying it's a trusted URL). If not "redirectUrl" is provided, the
   URL in the "dspace.ui.url" property (redirecting to the Angular front-end)
   is used.

   **Note:** For the DRUM back-end, the path of "redirectUrl" should be
   "/server/login", to ensure the the temporary authentication cookie is
   correctly processed and destroyed. This causes the initial back-end "login"
   page to be displayed for a brief moment in the browser with a
   "Login Successful" popup (the DSpace Shibboleth implementation has the same
   behavior).

7. At this point, as the cookie is sent back in a request to /api/authn/login,
   which triggers the server-side to destroy the Cookie and move the JWT into a
   Header.

## CAS/LDAP File Placement

The `org.dspace.app.rest.security.CASLoginFilter` class is in the
"dspace-server-webapp" module, because there does not seem to be a Spring
mechanism for overriding the
`org.dspace.app.rest.security.WebSecurityConfiguration` class in which the
login filter classes are configured.

All other classes used for CAS/LDAP authentication are in the "additions"
module.
