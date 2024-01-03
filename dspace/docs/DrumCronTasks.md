# DRUM Cron Tasks

This document describes the tasks that run periodically using "cron", as of
DSpace 7.

DSpace provides a sample "crontab" file  at
<https://wiki.lyrasis.org/display/DSDOC7x/Scheduled+Tasks+via+Cron>

All of the sample tasks are performed, although sometimes on a different
schedule, except for the following:

* index-authority - We do not use an "authority" Solr core, so this task is not
  needed.

In addition, the following tasks are performed:

* generate-sitemaps - Generates sitemaps for use by search engines
* load-etd-nightly - Custom UMD task to load ProQuest ETD files
* curate - Run any Curation Tasks queued from the Admin UI

## Cron Container

In Kubernetes, the "drum" pod includes both a "drum" container, which runs the
actual Tomcat server for the DSpace back-end, and a "cron" container that runs
a "cron" process.

The "crontab" used by the "cron" process in the "cron" container is configured
in the "crontab.txt" file in each of the Kubernetes overlays of the "k8s-drum"
repository.

Note that "drum" and "cron" run in separate containers, and must share
directories between them, in order for each to access the changes made by
the other container. These shared directories are:

* /dspace/assetstorex
* /dspace/proquest
* /dspace/sitemaps

## Cron Tasks

### generate-sitemaps

Generates the sitemap entries for use by search engines.

To verify that the sitemaps are being generated:

1) In a web browser go to

   ```text
   <UI_SERVER_URL>/sitemap_index.xml
   ```

   where \<UI_SERVER_URL> is the URL of the Angular interface, i.e. on
   production:

   <https://drum.lib.umd.edu/sitemap_index.xml>

   Verify that the "lastmod" date has been updated within the last 8 hours.

----

### oai import

Updates the "oai" Solr core with new/modified documents for OAI-PMH harvesting.

1) Add a new item to DRUM

2) After the cron task has run, to verify that the item has been added to the
   "oai" core in Solr:

   a) Port-forward port 8983 for the "drum-solr-0" pod:

    ```bash
    $ kubectl port-forward drum-solr-0 8983
    ```

   b) In a web browser, go to <http://localhost:8983/solr/#/oai/query>

   c) In the "q" field, enter the following:

    ```text
    metadata.dc.title:"<ITEM TITLE>"
    ```

    where \<ITEM_TITLE> is the title of the item that was added in the previous
    step.

    Execute the query, and verify that a single entry is found.

**Note:** There is a known DSpace issue (see DSpace pull request 2856) that
deleted items are *not* removed from the "oai" Solr index.

----

### index-discovery

Cleans and updates the Discovery indexes ("search"), including removing any
deleted documents.

Deleting an item appears to immediately update the Solr "search" core, so
testing that this task actually works is difficult.

Verifying that the task actually ran can be done using Splunk:

1) In a web browser, go to <https://www.splunk.umd.edu:8000/en-US/app/UMD_svpaap_libr_dss_ssdr/search>

2) In the search textbox, enter the following query formatted as follows

```text
cluster_name=<K8S_CLUSTER> pod="drum-0" namespace=<K8S_NAMESPACE> sourcetype="kube:container:cron" (script OR index OR indexing) | reverse
```

which searches the last 24 hours of log entries for the works "script", "index",
or "indexing", where \<K8S_NAMESPACE> is the Kubernetes namespace
(i.e., "sandbox", "test", "qa", "prod"), and \<K8S_CLUSTER> is the Kubernetes
cluster containing the namespace (i.e., "test_cluster" for sandbox/test/qa, and
"prod_cluster" for prod).

For example to check the "sandbox" Kubernetes namespace:

```text
cluster_name=test_cluster pod="drum-0" namespace=sandbox sourcetype="kube:container:cron" (script OR index OR indexing) | reverse
```

Verify that the search results contain entries such as:

```text
The script has started
Updating Index
Done with indexing
The script has completed
```

**Note:** These entries may be interleaved with other log entries.

----

### load-etd-nightly (ProQuest)

Custom UMD functionality to load ProQuest ETD files.

**Note:** The following steps should *not* be run in production, as it involves
the addition of an item.

1) From production, download an ETD Zip file from the
   "/dspace/proquest/processed/" directory (the following steps will use a
   notional file named "etdadmin_upload_945572.zip"):

   ```bash
   $ kubectl config use-context prod
   $ kubectl exec drum-0 -c cron -- /bin/bash -c "ls -ltr /dspace/proquest/processed"
   $ kubectl cp drum-0:/dspace/proquest/processed/etdadmin_upload_945572.zip -c cron etdadmin_upload_945572.zip
   ```

2) Switch to the Kubernetes namespace to verify (the following uses the
   Kubernetes "test" namespace), and copy to the file to the
   "/dspace/proquest/incoming" directory of the "cron" container of the "drum-0"
   pod:

   ```bash
   $ kubectl config use-context test
   $ kubectl etdadmin_upload_945572.zip cp drum-0:/dspace/proquest/incoming/etdadmin_upload_945572.zip -c cron
   ```

3) On your local workstation, extract the Zip file:

   ```bash
   $ unzip etdadmin_upload_945572.zip
   ```

   And then determine the title of the item by running the following:

   ```bash
   $ grep "DISS_title" *.xml
   ```

   This should return an entry such as:

   ```text
   RoigeMas_umd_0117E_23003_DATA.xml:    <DISS_title>Repositioning Cognitive Kinds</DISS_title>
   ```

   where “Repositioning Cognitive Kinds” is the item title.

4) After the cron task has run, verify the following:

    * The ETD Zip file ("etdadmin_upload_951888.zip" in the above) has been moved
      to "/dspace/proquest/processed".
    * An email will be sent to (on sandbox/test/qa to "mohideen@umd.edu",
      on "prod" to "lib-drum@umd.edu")
    * Search for the item in the DRUM repository using the title from the
      previous step. Verify that at least one entry is found (there may be
      multiple entries, if the ETD file had already been processed in the
      database snapshot being used).

----

### dspace stats-util -f

Removes entries in the Solr "statistics" core, where "isBot" flag is true.

**Note:** This cron task is most easily tested in production, because spiders
cannot reach the sandbox/test/qa servers.

To verify in production:

1) Port forward the Solr port (8983):

    ```bash
    $ kubectl config use-context prod
    $ kubectl port-forward drum-solr-0 8983
    ```

2) In a web browser, go to the followings URL:

    <http://localhost:8983/solr/statistics/select?q=isBot%3Atrue&sort=time+asc&fl=time&wt=json&indent=true>

    This a JSON list of "time" records, for Solr entries with "isBot: true",
    sorted in ascending order. The first entry should be on or after 2:30 am of
    the current date (as all other entries prior to that date/time will have been
    removed by the cron task.

To verify in sandbox/test/qa:

1) Run the following to create a "sample_spider_bot_request.json" file:

    ```bash
    cat << EOF > sample_spider_bot_request.json
    {
      "add": {
        "doc": {
            "ip":"192.168.1.1",
            "referrer":"https://example.com/",
            "dns":"example.com",
            "userAgent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:107.0) Gecko/20100101 Firefox/107.0",
            "isBot":true,
            "id":"12345678-abcd-1234-abcd-123456789ab",
            "type":0,
            "owningItem":["18da3919-f4c2-4beb-933d-68966542f471"],
            "owningColl":["d3f66d76-38ce-40cb-99f0-f2993f5e8288"],
            "owningComm":["38108566-d108-4773-9ce0-04159b7343ce"],
            "time":"2022-12-02T07:20:46.065Z",
            "bundleName":["THUMBNAIL"],
            "statistics_type":"view",
            "uid":"3b5e3154-5b4f-421b-a970-52b13b936695"
        }
      },
      "commit" :{}
    }
    EOF
    ```

2) Port forward the Solr port (8983)

    ```bash
    $ kubectl port-forward drum-solr-0 8983
    ```

3) Run the following "curl" command to add the request to Solr:

    ```bash
    $ curl -X POST -H 'Content-Type: application/json' 'http://localhost:8983/solr/statistics/update' --data-binary @sample_spider_bot_request.json
    ```

4) In a web browser go to

    <http://localhost:8983/solr/statistics/select?q=isBot%3Atrue&sort=time+asc&wt=json&indent=true>

    Verify that there is one entry listed.

5) After the cron task has run, in a web browser go to

    <http://localhost:8983/solr/statistics/select?q=isBot%3Atrue&sort=time+asc&wt=json&indent=true>

    and verify that no entries are displayed.

    **Note:** If running the cron task manually, be sure to force Solr to commit
    by running:

    ```bash
    $ curl http://localhost:8983/solr/statistics/update?commit=true
    ```

----

### dspace sub-daily

Sends an email to any users who have "subscribed" to a Collection, notifying
them of newly added content.

**Note:** In DSpace 7.4, there is no way for a user to subscribe/unsubscribe to
a collection via the GUI (this functionality is implemented in DSpace 7.5).
Existing subscriptions should still work.

Also, in DSpace 7.5, this task is changed to "subscription-send"

**Note:** The following steps should *not* be run in production, as it involves
the addition of an item.

Since the GUI functionality for adding a subscription is not available in
DSpace 7.4, the following procedure adds a subscription directly to the Postgres
database:

1) Add a subscription to the "subscription" table in Postgres

    a) Login in to DRUM as an "administrator" user.

    b) Select "Access Control | People" from the administrative sidebar. The
       "EPeople" page will be displayed.

    c) On the "EPeople" page, look up your username and note the value in the
       "ID" field

    d) Find the collection you want to subscribe to, and note the collection ID
       of the collection from the URL. For example, for the
       "Aerospace Engineering Theses and Dissertations" collection,
       <https://drum.sandbox.lib.umd.edu/collections/8976365e-2edf-4fb5-a706-dec3e5c01983>,
       the collection ID is "8976365e-2edf-4fb5-a706-dec3e5c01983"

    e) Access the "drum-db-0" container running Postgres:

    ```bash
    $ kubectl exec -it drum-db-0 -- /bin/bash
    ```

    f) Run the following command in the "drum-db-0" container to access the "psql" client

    ```bash
    drum-db-0$ psql --username=drum --dbname=drum
    ```

    g) Run the following command to determine the next valid subscription id:

    ```bash
    psql$ select MAX(subscription_id)+1 from subscription;
    ```

    h) To add a subscription, the psql command will have the form:

    ```text
    INSERT INTO subscription (subscription_id, eperson_id, collection_id) VALUES (<SUBSCRIPTION_ID>,'<EPERSON_ID>', '<COLLECTION_ID>');
    ```

    where \<SUBSCRIPTION_ID> is the id number from the previous step,
    \<EPERSON_ID> is your eperson ID, and \<COLLECTION_ID> is the collection ID.
    For example, if the next subscription id is 605, your eperson ID is
    "c9d13f20-0c95-4844-b8a6-254d128c4f14", and the collection ID is
    "8976365e-2edf-4fb5-a706-dec3e5c01983", the command would be:

    ```bash
    psql$ INSERT INTO subscription (subscription_id, eperson_id, collection_id) VALUES (605, 'c9d13f20-0c95-4844-b8a6-254d128c4f14', '8976365e-2edf-4fb5-a706-dec3e5c01983');
    ```

2) Log in to DRUM as an administrator, and add a new item to the collection
   that has been subscribed to.

3) After the cron task has run, verify that you receive an email indicating that
   the new item has been added to the collection

----

### filter-media

Extracts full text from documents and creates thumbnail images.

To verify in production:

1) Find an item that was added on the previous day and verify that a
   thumbnail image has been created.

To verify in sandbox/test/qa:

1) Add a new item to a collection. Note that a thumbnail image has not been
   created

2) After the cron task has run, verify that a thumbnail images has been created
   for the item added the previous step.

----

### doi-organiser

Sends information about new and changed DOIs to the DOI registration agency.

To verify in production:

1) Find an item that was added on the previous day and verify that it has a
   "DRUM DOI" entry, with an identifier starting with "https://doi.org".

To verify in sandbox/test/qa:

1) Add a new item to a collection. Note that a "DRUM DOI" entry has not been
   created.

2) After the cron task has run, verify that the item now has a "DRUM DOI" entry,
   with an identifier starting with "https://doi.org".

## Untestable Cron Tasks

The following cron tasks are currently not testable, as there is no apparent
way to access the functionality through the GUI.

### curate

As of DSpace 7.4, there does not appear a way to assign a curation task to a
queue (in DSpace 6 there was a "Queue" button along with a "Start" button on the
"System Curation Tasks" page).

Therefore, does not appear to be a way to test this issue, as there are no
curation tasks to run via the queue.

## Broken Cron Tasks

The following cron tasks are "broken" as of DSpace 7.4, with the expectation
that they will be fixed in subsequent DSpace releases:

### checker/checker-emailer

Verifies the checksums of all files stored in DSpace, and notify the system
administrator whether any checksums were found to be different.

There are two open DSpace issues related checksum checker, indicating that it
does not work:

* <https://github.com/DSpace/DSpace/issues/8570>
* <https://github.com/DSpace/DSpace/pull/8500>

These may possibly be fixed in DSpace 7.6.

Also, the current implementation has "checker-emailer -All", and it is seems
unlikely that "-All" is valid flag (it should probably be "--All").

----

### cleanup

Remove deleted bitstreams from the assetstore.

Currently fails with an "org.hibernate.exception.ConstraintViolationException"
when run manually.

The following DSpace issues indicate that this functionality is not working:

The following DSpace issues:

* <https://github.com/DSpace/DSpace/issues/7348>
* <https://github.com/DSpace/DSpace/pull/2992>
* <https://github.com/DSpace/DSpace/pull/8660> - Fixed in DSpace 7.5

----

## Abandoned/Legacy Cron Tasks

The following cron tasks are either not working, or represent legacy
functionality that DSpace no longer recommends using. They were removed
as part of the DSpace 7 upgrade.

### index-discovery -o/stats-util -o

The "-o" ("optimize") option is no longer supported (even though the
functionality still exists in the DSpace class).

These tasks are no longer included in the DSpace sample "crontab" file, and have
been removed.

----

### stat-monthly/stat-general/stat-report-monthly/stat-report-general

Generates statistics reports based on the DSpace log.

This functionality was broken (by UMD) for over a year in DSpace 6, when DRUM
was moved to Kubernetes.

This functionality is not working DSpace 7.4, and will likely never be
implemented (see <https://github.com/DSpace/DSpace/issues/2852#issuecomment-1307797383>)
which says:

> General consensus was that this log-based statistics feature should not be
> added into 7.x.

----

### stats-util -s

"Shards" the Solr "statistics" core, placing each year in its own Solr index.

This task has never been run (has always been commented out). According to
<https://wiki.lyrasis.org/display/DSDOC7x/SOLR+Statistics+Maintenance#SOLRStatisticsMaintenance-SolrShardingByYear>:

> The DSpace tool described below for managing Solr data through yearly
> sharding no longer functions in DSpace 7.x (see also
> <https://github.com/DSpace/DSpace/issues/8478>).
> Using these tools to manage Solr shards is no longer recommended.

This task is no longer included in the DSpace sample "crontab" file, and has
been removed.
