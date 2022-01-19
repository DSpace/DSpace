# Docker Compose Resources

***
:warning: **NOT PRODUCTION READY**  The below Docker Compose resources are not guaranteed "production ready" at this time. They have been built for development/testing only. Therefore, DSpace Docker images may not be fully secured or up-to-date. While you are welcome to base your own images on these DSpace images/resources, these should not be used "as is" in any production scenario.
***

## root directory Resources
- docker-compose.yml
  - Docker compose file to orchestrate DSpace 7 REST components
- docker-compose-cli
  - Docker compose file to run DSpace CLI tasks within a running DSpace instance in Docker

## dspace/src/main/docker-compose resources

- cli.assetstore.yml
  - Docker compose file that will download and install a default assetstore.
- cli.ingest.yml
  - Docker compose file that will run an AIP ingest into DSpace 7.
- db.entities.yml
  - Docker compose file that pre-populate a database instance using a SQL dump.  The default dataset is the configurable entities test dataset.
- docker-compose-angular.yml
  - Docker compose file that will start a published DSpace angular container that interacts with the branch.
- docker-compose-https.yml
  - Docker compose file that will run both the UI and REST API via HTTPS on localhost (on https://localhost/ and https://localhost/server/).
- docker-compose-iiif.yml
  - Docker compose file that runs a Cantaloupe IIIF image server in Docker, for testing IIIF integration.
- docker-compose-oidc.yml
  - Docker compose file that runs Keycloak in Docker, for testing OIDC (OpenID Connect) authentication integration.
- docker-compose-shibboleth.yml
  - Docker compose file that will start a *test/demo* Shibboleth SP container (in Apache HTTPD) that proxies requests to the DSpace container

## To refresh / pull DSpace images from Dockerhub
```
docker-compose -f docker-compose.yml -f docker-compose-cli.yml pull
```

## To build DSpace images using code in your branch
```
docker-compose -f docker-compose.yml -f docker-compose-cli.yml build
```

## Run DSpace 7 REST from your current branch
```
docker-compose -p d7 up -d
```
REST API will be available at http://localhost:8080/server/

## Run DSpace 7 REST and Angular from your branch

```
# This starts 2 containers: dspace (backend) and dspace-angular
docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-angular.yml up -d
```
* UI will be available at http://localhost:4000/
* REST API will be available at http://localhost:8080/server/

## Run DSpace 7 REST and Angular via HTTPS on localhost

```
# This starts 3 containers: dspace (backend), dspace-angular, dspace-https
docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-angular.yml -f dspace/src/main/docker-compose/docker-compose-https.yml up -d
```

* UI will be available at https://localhost/
* REST API will be available at https://localhost/server/
* NOTE: Your browser will likely warn you that these URLs are "insecure" or "unsafe".
This is because a localhost SSL certificate is generated on the fly and used for HTTPS.

## Run DSpace 7 REST with a IIIF Image Server from your branch
*Only useful for testing IIIF support in a development environment*

This command starts our `dspace-iiif` container alongside the REST API.
That container provides a [Cantaloupe image server](https://cantaloupe-project.github.io/),
which can be used when IIIF support is enabled in DSpace (`iiif.enabled=true`).

```
# This starts 2 containers: dspace (backend) and dspace-iiif
docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-iiif.yml up -d

# Optionally, you can also start dspace-angular by adding it to the '-f' list like:
docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-angular.yml -f dspace/src/main/docker-compose/docker-compose-iiif.yml up -d
```

The Cantaloupe image server will be available at http://localhost:8182/, which means the default settings
for `iiif.image.server` will work with this container. You will however need to ensure `iiif.enabled=true` and 
one or more Items have IIIF enabled (the metadata field `dspace.iiif.enabled` must be set to "true")

## Run DSpace 7 REST with Keycloak for OIDC (OpenID Connect) auth
*Only useful for testing OIDC authentication support in a development environment*

DSpace includes a custom `dspace-oidc` container which can be run alongside the REST API.
The `dspace-oidc` container provides a [Keycloak server](https://www.keycloak.org/),
which can be used when OIDC authentication is enabled in DSpace,
i.e. when `org.dspace.authenticate.OidcAuthentication` is listed in the `AuthenticationMethod` configs.

```
# This starts 4 main containers: dspace (backend), dspace-angular, dspace-https, dspace-oidc
docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-angular.yml -f dspace/src/main/docker-compose/docker-compose-https.yml -f dspace/src/main/docker-compose/docker-compose-oidc.yml up -d
```
* UI will be available at https://localhost/
* REST API will be available at https://localhost/server/
* Keycloak will be available at https://localhost/keycloak/
    * NOTE: Keycloak takes a while (~2-3mins) to boot up. Please be patient.
* NOTE: Your browser will likely warn you that these URLs are "insecure" or "unsafe".
  This is because a localhost SSL certificate is generated on the fly and used for HTTPS.

After starting all 4 containers (dspace, dspace-angular, dspace-https, and dspace-oidc) above,
additional steps are required to enabled OIDC authentication:

1. Update your `local.cfg` (if you don't have one create it in `[src]/dspace/config/local.cfg`) to enable & configure OIDC:
   ```
   # Enable both Password auth & OIDC
   plugin.sequence.org.dspace.authenticate.AuthenticationMethod = org.dspace.authenticate.PasswordAuthentication
   plugin.sequence.org.dspace.authenticate.AuthenticationMethod = org.dspace.authenticate.OIDCAuthentication

   # Settings for OIDC
   # Must be the HTTPS URL
   authentication-oidc.auth-server-url = https://localhost
   authentication-oidc.realm = dspace-realm
   authentication-oidc.token-endpoint = ${authentication-oidc.auth-server-url}/auth/realms/${authentication-oidc.realm}/protocol/openid-connect/token
   authentication-oidc.authorize-endpoint = ${authentication-oidc.auth-server-url}/auth/realms/${authentication-oidc.realm}/protocol/openid-connect/auth
   authentication-oidc.user-info-endpoint = ${authentication-oidc.auth-server-url}/auth/realms/${authentication-oidc.realm}/protocol/openid-connect/userinfo
   authentication-oidc.client-id = dspace-rest
   # NOTE: THIS IS PURPOSEFULLY EMPTY & WILL BE SET BELOW
   authentication-oidc.client-secret =
   authentication-oidc.scopes = openid,email,profile
   authentication-oidc.can-self-register = false
   authentication-oidc.user-info.email = email
   authentication-oidc.user-info.first-name = given_name
   authentication-oidc.user-info.last-name = family_name
   authentication-oidc.redirect-url = ${dspace.server.url}/api/authn/oidc
   ```
3. Visit the Keycloak Admin Console via https://localhost/auth
   (if it doesn't respond immediately be patient. Keycloak sometimes takes a minute or two).
4. Login as default Admin (admin/admin)
5. Add a new Realm (hover over realm selector). Name it "dspace-realm"
6. Add a new Client (Clients -> Create). Give it a Client ID of "dspace-rest" & a root URL of https://localhost/server
7. Edit that Client, on the "Settings" tab change the "Access Type" dropdown from "public" to "confidential". Click Save
8. Now for that Client, a "Credentials" tab appears. There you'll find the Secret key.
   This key MUST be added to this setting in your `local.cfg`:
   ```
   authentication-oidc.client-secret = zsrDp1sJDyAbRiOAba9NrISQ7Lrlo07b
   ```
9. Finally, create a test user (e.g. named "dspace") for this new Realm.
   * Click on "Users" menu.
   * Click "Add user" button.
   * Username: 'dspace', email: 'dspace@test.edu', First Name: 'DSpace', Last Name: 'OIDC'. Click "Save".
   * Click "Credentials" tab for that user.
   * Set a Password (e.g. "dspace"). Turn "Temporary" to "OFF". Click "Set Password"
10. Now, test it out. Access the UI at https://localhost and attempt to login via OIDC using the new user.

## Run DSpace 7 REST and Shibboleth SP (in Apache) from your branch

*Only useful for testing Shibboleth in a development environment*

This Shibboleth container uses https://samltest.id/ as an IdP (see `../docker/dspace-shibboleth/`).
Therefore, for Shibboleth login to work properly, you MUST make your DSpace site available to the external web.

One option is to use a development proxy service like https://ngrok.com/, which creates a temporary public proxy for your localhost.
The remainder of these instructions assume you are using ngrok (though other proxies may be used).

1. If you use ngrok, start it first (in order to obtain a random URL that looks like https://a6eb2e55ad17.ngrok.io):
   ```
   ./ngrok http 443
   ```

2. Then, update your `local.cfg` (if you don't have one create it in `[src]/dspace/config/local.cfg`) to use that ngrok URL & configure Shibboleth:
   ```
   # NOTE: dspace.server.url MUST be available externally to use with https://samltest.id/.
   # In this example we are assuming you are using ngrok.
   dspace.server.url=https://[subdomain].ngrok.io/server

   # Enable both Password auth & Shibboleth
   plugin.sequence.org.dspace.authenticate.AuthenticationMethod = org.dspace.authenticate.PasswordAuthentication
   plugin.sequence.org.dspace.authenticate.AuthenticationMethod = org.dspace.authenticate.ShibAuthentication
   
   # Settings for https://samltest.id/
   authentication-shibboleth.netid-header = uid
   authentication-shibboleth.email-header = mail
   authentication-shibboleth.firstname-header = givenName
   authentication-shibboleth.lastname-header = sn
   authentication-shibboleth.role-header = role
   ```

3. Build the Shibboleth container (if you haven't built or pulled it before):
   ```
   cd [dspace-src]
   docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-shibboleth.yml build
   ```

4. Start all containers, passing your public hostname as the `DSPACE_HOSTNAME` environment variable:
   ```
   DSPACE_HOSTNAME=[subdomain].ngrok.io docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-shibboleth.yml up -d
   ```
   NOTE: For Windows you MUST either set the environment variable separately, or use the 'env' command provided with Git/Cygwin
   (you may already have this command if you are running Git for Windows). See https://superuser.com/a/1079563
   ```
   env DSPACE_HOSTNAME=[subdomain].ngrok.io docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-shibboleth.yml up -d
   ```

5. Finally, for https://samltest.id/, you need to upload your Shibboleth Metadata for the site to "trust" you.
   Using the form at https://samltest.id/upload.php, enter in
   `https://[subdomain].ngrok.io/Shibboleth.sso/Metadata` and click "Fetch!"
      * Note: If samltest.id still says you are untrusted, restart your Shibboleth daemon! (This may be necessary to download the IdP Metadata from samltest.id)
        ```
        docker exec -it dspace-shibboleth /bin/bash
        service shibd stop
        service shibd start
        ```

6. At this point, if all went well, your site should work!  Try it at
   https://[subdomain].ngrok.io/server/
7. If you want to include Angular UI as well, then you'll need a few extra steps:
      * Update `environment.dev.ts` in this directory as follows:
        ```
        rest: {
          ssl: true,
          host: '[subdomain].ngrok.io',
          port: 443,
          // NOTE: Space is capitalized because 'namespace' is a reserved string in TypeScript
          nameSpace: '/server'
        }
        ```
      * Spin up the `dspace-angular` container alongside the others, e.g.
        ```
        DSPACE_HOSTNAME=[subdomain].ngrok.io docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/docker-compose-angular.yml -f dspace/src/main/docker-compose/docker-compose-shibboleth.yml up -d
        ```

## Run DSpace 7 REST and Angular from local branches

_The system will be started in 2 steps. Each step shares the same docker network._

From DSpace/DSpace
```
docker-compose -p d7 up -d
```

From DSpace/DSpace-angular (build as needed)
```
docker-compose -p d7 -f docker/docker-compose.yml up -d
```

## Ingest Option 1: Ingesting test content from AIP files into a running DSpace 7 instance

Prerequisites
- Start DSpace 7 using one of the options listed above
- Build the DSpace CLI image if needed.  See the instructions above.

Create an admin account.  By default, the dspace-cli container runs the dspace command.
```
docker-compose -p d7 -f docker-compose-cli.yml run --rm dspace-cli create-administrator -e test@test.edu -f admin -l user -p admin -c en
```

Download a Zip file of AIP content and ingest test data
```
docker-compose -p d7 -f docker-compose-cli.yml -f dspace/src/main/docker-compose/cli.ingest.yml run --rm dspace-cli
```

## Ingest Option 2: Ingest Entities Test Data
_Remove your d7 volumes if you already ingested content into your docker volumes_

Start DSpace REST with a postgres database dump downloaded from the internet.
```
docker-compose -p d7 -f docker-compose.yml -f dspace/src/main/docker-compose/db.entities.yml up -d
```

Download an assetstore from a tar file on the internet.
```
docker-compose -p d7 -f docker-compose-cli.yml -f dspace/src/main/docker-compose/cli.assetstore.yml run dspace-cli
```

## Modify DSpace Configuration in Docker
While your Docker containers are running, you may directly modify any configurations under
`[dspace-src]/dspace/config/`. Those config changes will be synced to the container.
(This works because our `docker-compose.yml` mounts the `[src]/dspace/config` directory from the host into the running Docker instance.)

Many DSpace configuration settings will reload automatically (after a few seconds).  However, configurations which are cached by DSpace (or by Spring Boot) may require you to quickly reboot the Docker containers by running `docker-compose -p d7 down` followed by `docker-compose -p d7 up -d`.

## Running DSpace CLI scripts in Docker
While the Docker containers are running, you can use the DSpace CLI image to run any DSpace commandline script (i.e. any command that normally can be run by `[dspace]/bin/dspace`).  The general format is:

```
docker-compose -p d7 -f docker-compose-cli.yml run --rm dspace-cli [command] [parameters]
```

So, for example, to reindex all content in Discovery, normally you'd run `./dspace index-discovery -b` from commandline.  Using our DSpace CLI image, that command becomes:

```
docker-compose -p d7 -f docker-compose-cli.yml run --rm dspace-cli index-discovery -b
```

Similarly, you can see the value of any DSpace configuration (in local.cfg or dspace.cfg) by running:

```
# Output the value of `dspace.ui.url` from running Docker instance
docker-compose -p d7 -f docker-compose-cli.yml run --rm dspace-cli dsprop -p dspace.ui.url
```
