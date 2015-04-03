# coding=utf-8
# This work is licensed!
# pylint: disable=W0702,R0201,C0111,W0613,R0914

"""
    We have a cursor to db database.
"""
from collections import defaultdict
import os
import sys
import logging
_logger = logging.getLogger( )


class db_assets( object ):

    def __init__( self, cursor ):
        cursor.execute( """
            select
                handle, item_id, internal_id, name
            from handle
                inner join item2bundle
                    on handle.resource_id=item2bundle.item_id
                inner join bundle2bitstream
                    on item2bundle.bundle_id=bundle2bitstream.bundle_id
                inner join bitstream
                    on bundle2bitstream.bitstream_id=bitstream.bitstream_id;
        """ )
        objs = cursor.fetchall()
        self.d = defaultdict(list)
        for handle, item_id, internal_id, name in objs:
            self.d[handle].append( (name, internal_id) )

    def _to_local(self, asset_id):
        return os.path.join( asset_id[0:2], asset_id[2:4], asset_id[4:6], asset_id )

    def local_path( self, handle, name ):
        for item in self.d.get(handle, []):
            if item[0] == name:
                return self._to_local(item[1])
        return None

    def abs_path( self, dspace_conf, handle, name ):
        """
            Get abs path either from assetstore.dir or lr.dspace.dir/assetstore
        """
        assetstore_dir = dspace_conf["assetstore.dir"]
        if assetstore_dir is None:
            assetstore_dir = os.path.join(
                dspace_conf["lr.dspace.dir"], "assetstore" )

        local_path = self.local_path(handle, name)
        if local_path is None:
            sys.exit(1)
        return os.path.join( assetstore_dir, local_path )


def do( cursor, conf, kwargs ):
    """
        Get abs path to bitstream identified by handle/nam
    """
    db = db_assets(cursor)
    if "handle" not in kwargs or "name" not in kwargs:
        _logger.warning( "No handle/name specified on the command line" )
        return None
    print db.abs_path( conf, kwargs["handle"], kwargs["name"] )
