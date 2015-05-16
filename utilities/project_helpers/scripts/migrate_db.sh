#!/bin/bash
dbname_new="$1"
dbname_old="$2"
NEWPORT=5433
if sudo -u postgres createdb -p $NEWPORT -O dspace $dbname_new ;then
	echo "Created db $dbname_new";
else
	echo "Failed to create db $dbname_new, maybe it exists.";
	echo "Should we drop it?"
	echo "This will run \"sudo -u postgres dropdb -p $NEWPORT $dbname_new\""
	read -p "Are you sure? [y/n] " -n 1 -r
	echo    # (optional) move to a new line
	if [[ $REPLY =~ ^[Yy]$ ]]
	then
	    # do dangerous stuff
	    sudo -u postgres dropdb -p $NEWPORT $dbname_new
	    if ! sudo -u postgres createdb -p $NEWPORT -O dspace $dbname_new;then
		echo "Failed to create db $dbname_new";
		exit 1;
	    fi
	else
	    echo "Exiting"
	    exit 0;
	fi
fi

sudo -u postgres pg_dump $dbname_old | sudo -u postgres psql -p $NEWPORT $dbname_new
