# -*- coding: utf-8 -*-
# See main file for license.

import utils
import sys
import logging

_logger = logging.getLogger("common")


class document(object):
    """
        Class representing one document from index.
    """

    def __init__( self, d, env_dict ):
        self._d = d

    def dict( self ):
        return self._d

    @property
    def id_str(self):
        return self._d.get("id", "unknown")

has_backend = False
#noinspection PyBroadException
try:
    import sunburnt as solr
    has_backend = True
except:
    _logger.error("Please, install sunburnt otherwise no indexer (solr) interaction possible!")


#noinspection PyBroadException
class adapter(object):
    """
        Class representing indexer.
    """

    #=================================================
    # local class for backend sharing option
    #=================================================

    #noinspection PyUnusedLocal,PyRedeclaration
    class solr_backend(object):
        def __init__( self, adapter_inst ):
            self.adapter_inst = adapter_inst

        def __enter__(self):
            return self.adapter_inst.backend

        # noinspection PyShadowingBuiltins
        def __exit__(self, type, value, traceback):
            pass

    #=================================================
    # ctor
    #=================================================

    def __init__( self, env_dict ):
        self.errors = 0
        self.auto_commit = env_dict["indexer"]["autocommit"]
        self.host = env_dict["indexer"]["backend_host"]
        self.mode = env_dict["indexer"]["mode"]
        self.retry_timeout = env_dict["indexer"]["retry_timeout"]
        self.backend = adapter.adapter_connect(
            self.host, self.mode, self.retry_timeout, raise_ex=False)

    #=================================================
    # static methods
    #=================================================

    @staticmethod
    def adapter_connect( host, mode, retry_timeout, raise_ex=False ):
        """
            Connect to backend.
        """
        try:
            _logger.info("Connecting to indexer [%s, %s].", host, mode)
            return solr.SolrInterface(host,
                                      mode=mode,
                                      retry_timeout=retry_timeout)
        except:
            _logger.exception("Connecting to indexer failed.")
            if raise_ex:
                raise
        return None

    #=================================================
    # operations
    #=================================================

    def valid( self ):
        """ Return if it is properly initialised. """
        return not self.backend is None

    #noinspection PyUnusedLocal
    def add( self, document, **kwargs ):
        """ Add a document  to index. """
        exc = None
        try:
            with adapter.solr_backend(self) as backend:
                backend.add( [document], **kwargs )
            if self.auto_commit:
                self.commit()
            return True
        except Exception, e:
            import traceback
            traceback.print_exc(file=sys.__stdout__)
            exc = e
            self.errors += 1
            if self.errors > 0:
                self.errors = 0
                _logger.exception(u"Could not add document to index [%s]\n[%s].",
                                 document.id_str,
                                 utils.uni(e))
                return False
            # try again - reconnect
        #
        _logger.warning("Soft error in adapter.add [%s].", exc)
        self.backend = adapter.adapter_connect(self.host, self.mode, self.retry_timeout)
        return self.add(document)

    def search( self, query_dict, fields=None, pages_count=10 ):
        """ Return search result. """
        try:
            with adapter.solr_backend(self) as backend:
                resp = backend.search(**query_dict)
                return resp
        except Exception, e:
            _logger.exception(u"Could not query backend [%s]." % utils.uni(e))
        return -1

    def query_generic( self, fields=None, pages_count=10, **kwargs ):
        """ Return search result. """
        try:
            with adapter.solr_backend(self) as backend:
                query = backend.query(**kwargs).paginate(rows=pages_count)
                if fields:
                    query = query.field_limit(fields)
                return query.execute()
        except Exception, e:
            _logger.warning(u"Could not query backend [%s]." % utils.uni(e))
        return -1

    def delete( self, docs_array=None, queries=None ):
        """ Return search result. """
        try:
            with adapter.solr_backend(self) as backend:
                if queries is not None:
                    backend.delete(queries=queries)
                else:
                    backend.delete(docs=docs_array)
                return True
        except Exception, e:
            _logger.warning(u"Could not delete from backend [%s]." % utils.uni(e))
        return False

    def commit( self ):
        # be sure we can write
        #_logger.info("Trying to commit to index.")
        try:
            with adapter.solr_backend(self) as backend:
                #_logger.info("Committing to index.")
                backend.commit()
                #_logger.info("Committed.")
                return True
        except Exception, e:
            _logger.warning(u"Could not commit in backend [%s]." % utils.uni(e))
        return False

    def optimise( self, maxSegments=None ):
        _logger.info("Optimising index.")
        try:
            with adapter.solr_backend(self) as backend:
                backend.optimize(waitSearcher=None, maxSegments=maxSegments)
                _logger.info("Optimised.")
                return True
        except Exception, e:
            _logger.warning(u"Could not optimise backend [%s]." % utils.uni(e))
        return False