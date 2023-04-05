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
    created on a successful login, "false" otherwise.

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
   authentication is complete. The "service" and "redirectUrl" parameters
   are full URLs, with the "host" portion being the DSpace server URL.

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

## CAS Authentication, Impersonated Users and Special Groups

DSpace 7 enables administrators to impersonate users by adding an
"X-On-Behalf-Of" header to the HTTP request. This header is handled by the
"org.dspace.app.rest.security.StatelessAuthenticationFilter" class in the
authetication filter chain (see the
"org.dspace.app.rest.security.WebSecurityConfiguration" class). The
"StatelessAuthenticationFilter" class calls the "switchContextUser" method on
the "org.dspace.core.Context" class, which sets the current user and clears the
special groups.

The special groups for the impersonated user are populated by the
"org.dspace.app.rest.security.AnonymousAdditionalAuthorizationFilter" class,
which despite its configuration as one of the first filters in
"WebSecurityConfiguration", is actually near the end of the filter chain.
This is because the "StatelessAuthenticationFilter" which the filter is supposed
to be "before" hasn't been added yet.

The "AnonymousAdditionalAuthorizationFilter" calls the "getSpecialGroups"
method of the "AuthenticationServiceImpl" class, which in turn calls the
"getSpecialGroups" method on each "AuthenticationMethod" implementation,
including in the "org.dspace.authenticate.CASAuthentication".

Note that the "getSpecialGroups" on the "CASAuthentication" is called for
*every* HTTP request.

In the normal authentication process, the special groups for a user are
populated when the request contains a "CAS_LDAP" ("cas.ldap") attribute in
the request's session. This occurs once as part of the CAS authentication
process, and the resulting special groups are added to the JWT token for the
user (and presented on subsequent requests). The LDAP server is queried
only once during this process.

For impersonated users, the special groups must be retrieved from LDAP, based on
the "currentUser" of the DSpace context. A single page retrieval by the
browser may require multiple HTTP requests, so it is impractical to query the
LDAP server for each request. In order to handle this, the
"edu.umd.lib.dspace.authenticate.impl.LdapServiceImpl" uses a simple in-memory
expiring cache to store the result of an LDAP server query for a limited
period of time, with a default expiration timeout of 5 minutes. This
limits the number of requests made to the LDAP server for any particular
impersonated user to one every 5 minutes.

The cache expiration timeout is controlled by the "drum.ldap.cacheTimeout"
configuration parameter. Note that it is possible to turn off caching by setting
this value to "0", but that is not recommended as it would result in excessive
numbers of queries to the campus LDAP server when impersonating users.
