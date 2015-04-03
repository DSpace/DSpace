#!/usr/bin/python

import psycopg2
import sys

cur = None

def find_place(item_id, new_metadata_field_id):
    cur.execute("""select coalesce(max(place),0) FROM metadatavalue where item_id=%(item_id)s AND metadata_field_id=%(new_metadata_field_id)s""", {'item_id': item_id, 'new_metadata_field_id': new_metadata_field_id})
    row = cur.fetchone()
    return row[0]

def fetch_metadata(old_metadata_field_id):
    cur.execute("""select metadata_value_id,item_id from metadatavalue where metadata_field_id = %(old_metadata_field_id)s order by item_id, metadata_field_id, place""", {'old_metadata_field_id': old_metadata_field_id})
    return cur.fetchall()

def update_metadata(metadata_value_id, new_metadata_field_id, place):
    cur.execute("""update metadatavalue set metadata_field_id=%(new_metadata_field_id)s,place=%(place)s where metadata_value_id=%(metadata_value_id)s""", {'place': place, 'new_metadata_field_id': new_metadata_field_id, 'metadata_value_id': metadata_value_id})

def lookup_field_name(metadata_field_id):
    res = None
    cur.execute("""select msr.short_id||'.'||mfr.element||coalesce('.'||mfr.qualifier,'') from metadatafieldregistry mfr inner join metadataschemaregistry msr on (mfr.metadata_schema_id=msr.metadata_schema_id) where mfr.metadata_field_id=%(metadata_field_id)s""", {'metadata_field_id': metadata_field_id})
    row = cur.fetchone()
    if row:
        res = row[0]
    return res

def prompt():
    res = False
    answ = raw_input('Press [y] or [Y] to continue: ')
    if answ.lower() == 'y':
        res = True
    return res

def main():
    global cur
    argv = sys.argv
    if len(argv) != 4:
         sys.exit('Usage: %s <db connection string> <old metadata field id> <new metadata field id' % sys.argv[0])

    db_connection_string = argv[1]
    old_metadata_field_id = int(argv[2])
    new_metadata_field_id = int(argv[3])

    try:
        conn = psycopg2.connect(db_connection_string)
    except:
        sys.exit('Unable to connect to the database using: %s' % db_connection_string)
    cur = conn.cursor()

    old_metadata_field = lookup_field_name(old_metadata_field_id)
    new_metadata_field = lookup_field_name(new_metadata_field_id)

    if old_metadata_field is None:
        sys.exit("Metadata field id %d doesn't exist" % old_metadata_field_id)
    if new_metadata_field is None:
        sys.exit("Metadata field id %d doesn't exist" % new_metadata_field_id)

    rows = fetch_metadata(old_metadata_field_id)
    nrows = len(rows)
    if nrows == 0:
        print "No rows found with metadata_field_id %d (%s)" % (old_metadata_field_id, old_metadata_field)
        sys.exit()

    print "About to update %d rows with metadata_field_id %d (%s) to metadata_field_id %d (%s)" % (len(rows), old_metadata_field_id, old_metadata_field, new_metadata_field_id, new_metadata_field)
    cont = prompt()
    if not cont: 
        sys.exit("Aborting")

    for row in rows:
        metadata_value_id = row[0]
        item_id = row[1]
        place = find_place(item_id, new_metadata_field_id)
        place += 1
        print "Updating metadata_value_id %d to metadata_field_id %d at place %d ... " % (metadata_value_id, new_metadata_field_id, place),
        update_metadata(metadata_value_id, new_metadata_field_id, place)
        print "OK"
    conn.commit()

if __name__ == "__main__":
    main()
