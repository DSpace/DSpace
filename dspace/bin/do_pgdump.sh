DBNAME="dspace_sedici"
DBUSER="root"
DBHOST="localhost"
export PGPASSWORD="root"
FILENAME="$DBNAME-`date +%d-%m-%Y_%H%M`"
DESTFILENAME="`pwd`/$FILENAME.tgz"

echo "Iniciando pg_dump de BD $DBNAME@$DBHOST ..."
pg_dump -U $DBUSER -h $DBHOST -f /tmp/$FILENAME.tar -F t $DBNAME

echo "Transformando backup de BD ..."
pg_restore -Ft -O -f /tmp/$FILENAME.sql /tmp/$FILENAME.tar

echo "Comprimiendo ..."
tar --directory /tmp -czf $DESTFILENAME $FILENAME.sql

echo "Se genero el backup en $DESTFILENAME"
rm /tmp/$FILENAME.tar
rm /tmp/$FILENAME.sql
export PGPASSWORD=""
