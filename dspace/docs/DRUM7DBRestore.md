# Steps to restore drum 6 database dump to DSpace 7

1. Place the database dump in the `postgres-init` dir

    ```bash
    kubectl exec drum-db-0 -- pg_dump -O -U drum -d drum > postgres-init/drum.sql
    ```

2. Start the dspacedb container and wait for the restore to complete.

    ```bash
    docker compose -p d7 up -d dspacedb
    ```

    Check the logs:

    ```bash
    $ docker logs dspacedb
    ...
    ...
    ... LOG:  database system is ready to accept connections
    ```

3. In a separate terminal, remove the IRUS migration and related changes.

    ```bash
    # To workaround IRUS-UK Patch db migration conflict (LIBDRUM-652)
    docker exec -it dspacedb psql -U drum -c "DELETE FROM schema_version WHERE script='V6.0_2017.02.14__statistics-harvester.sql'"
    docker exec -it dspacedb psql -U drum -c "DROP TABLE OpenUrlTracker;"
    docker exec -it dspacedb psql -U drum -c "DROP SEQUENCE openurltracker_seq;"

    ```
