# DRUM Database Restore from DSpace 6

**Note:** The following steps describe using a DRUM DSpace 6 database snapshot
with DRUM running DSpace 7. See [dspace/docs/DrumDBRestore.md](DrumDBRestore.md)
for information about restoring a database snapshot from DRUM using DSpace 7.

1) Switch the appropriate Kubernetes namespace from which the database snapshot
   should be retrieved (the following example uses the Kubernetes "test"
   namespace):

   ```bash
   $ kubectl config use-context test
   ```

2) Run the following command to run "pg_dump" in the "drum-db-0" Kubernetes pod,
   placing the database dump in the `postgres-init` subdirectory:

    ```bash
    $ kubectl exec drum-db-0 -- pg_dump -O -U drum -d drum > postgres-init/drum.sql
    ```

3) Start the dspacedb container and wait for the restore to complete.

    ```bash
    $ docker compose -p d7 up -d dspacedb
    ```

    To determine if the restore is complete, run the following command, and wait
    for the "ready to accept connections" message:

    ```bash
    $ docker logs -f dspacedb
    ...
    ...
    ... LOG:  database system is ready to accept connections
    ```

    Hit `<Ctrl-C>` to exit the "docker logs" command and return to the terminal.

4) Run the following commands to remove the IRUS migration and related changes.

    ```bash
    # To workaround IRUS-UK Patch db migration conflict (LIBDRUM-652)
    $ docker exec -it dspacedb psql -U drum -c "DELETE FROM schema_version WHERE script='V6.0_2017.02.14__statistics-harvester.sql'"

    $ docker exec -it dspacedb psql -U drum -c "DROP TABLE OpenUrlTracker;"

    $ docker exec -it dspacedb psql -U drum -c "DROP SEQUENCE openurltracker_seq;"
    ```
