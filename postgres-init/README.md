# Database initialization for docker-compose

Place your database dump file in this directory, and the dspacedb container
will use it to initialize the database.

To get a dump from k8s environment:

```
kubectl exec drum-db-0 -- pg_dump -C -O -U drum -d drum > postgres-init/drum.sql

# Comment the line starting with 'CREATE DATABASE drum'
vim postgres-init/drum.sql
```

## Delete local database data

The postgres container will use the initialization dump only if the
database is not previously initialized. If you would like to reinitialize the
database, stop the dspacedb container and delete the volume.

```
docker-compose down
docker volume rm drum_pgdata
```