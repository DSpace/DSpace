"""
Barebones pure-python PostGreSQL

"""
# Copyright (C) 2001-2008 Barry Pederson <bp@barryp.org>
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this library; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301

import datetime
import errno
import exceptions
import re
import select
import socket
import sys
import types
try:
    from decimal import Decimal
except:
    Decimal = float
from struct import pack as _pack
from struct import unpack as _unpack

#
# See Python sys.version and sys.version_info
#
version = '2.0 alpha 2'
version_info = (2, 0, 0, 'alpha', 2)

#
# Module Globals specified by DB-API 2.0
#
apilevel = '2.0'
threadsafety = 1          # Threads may share the module, but not connections.
paramstyle = 'pyformat'   # we also understand plain-format

#
# Constructors specified by DB-API 2.0
#
Date = datetime.date
Time = datetime.time
Timestamp = datetime.datetime
DateFromTicks = datetime.date.fromtimestamp

def TimeFromTicks(t):
    dt = datetime.datetime.fromtimestamp(t)
    return datetime.time(dt.hour, dt.minute, dt.second)

TimestampFromTicks = datetime.datetime.fromtimestamp

class Binary(str):
    """
    Wrapper class for plain string to indicate it
    should be passed as a binary value to PostgreSQL.

    """
    pass

#
# Type identifiers specified by DB-API 2.0
#
STRING = object()
BINARY = object()
NUMBER = object()
DATETIME = object()
ROWID = object()

#
# Exception hierarchy from DB-API 2.0 spec
#
class Error(exceptions.StandardError):
    """
    Exception that is the base class of all other error
    exceptions. You can use this to catch all errors with one
    single 'except' statement. Warnings are not considered
    errors and thus should not use this class as base.

    """
    pass

class Warning(exceptions.StandardError):
    """
    Exception raised for important warnings like data
    truncations while inserting, etc.

    """
    pass

class InterfaceError(Error):
    """
    Exception raised for errors that are related to the
    database interface rather than the database itself.

    """
    pass

class DatabaseError(Error):
    """
    Exception raised for errors that are related to the
    database.

    """
    pass

class InternalError(DatabaseError):
    """
    Exception raised when the database encounters an internal
    error, e.g. the cursor is not valid anymore, the
    transaction is out of sync, etc.

    """
    pass

class OperationalError(DatabaseError):
    """
    Exception raised for errors that are related to the
    database's operation and not necessarily under the control
    of the programmer, e.g. an unexpected disconnect occurs,
    the data source name is not found, a transaction could not
    be processed, a memory allocation error occurred during
    processing, etc.

    """
    pass

class ProgrammingError(DatabaseError):
    """
    Exception raised for programming errors, e.g. table not
    found or already exists, syntax error in the SQL
    statement, wrong number of parameters specified, etc.

    """
    pass

class IntegrityError(DatabaseError):
    """
    Exception raised when the relational integrity of the
    database is affected, e.g. a foreign key check fails.

    """
    pass

class DataError(DatabaseError):
    """
    Exception raised for errors that are due to problems with
    the processed data like division by zero, numeric value
    out of range, etc.

    """
    pass

class NotSupportedError(DatabaseError):
    """
    Exception raised in case a method or database API was used
    which is not supported by the database, e.g. requesting a
    .rollback() on a connection that does not support
    transaction or has transactions turned off.

    """
    pass

#
# Custom exceptions raised by this driver
#

class PostgreSQL_Timeout(InterfaceError):
    """
    Exception raised by wait_notify() when timeout has expired.

    """
    pass


#
# Constants relating to Large Object support
#
INV_WRITE   = 0x00020000
INV_READ    = 0x00040000

SEEK_SET    = 0
SEEK_CUR    = 1
SEEK_END    = 2


################################
#
# Type conversion functions


_OCTAL_ESCAPE = re.compile(r'\\(\d\d\d)')

def _binary_to_python(s):
    """
    Convert a PgSQL binary value to a plain Python string.

    """
    s = _OCTAL_ESCAPE.sub(lambda x: chr(int(x.group(1), 8)), s)
    return Binary(s.replace('\\\\', '\\'))


def _bool_to_python(s):
    """
    Convert PgSQL boolean string to Python boolean

    """
    if s == 't':
        return True
    if s == 'f':
        return False
    raise InterfaceError('Boolean type came across as unknown value [%s]' % s)


def _char_to_python(s):
    """
    Convert character data, which should be utf-8 strings, to Python Unicode strings

    """
    return s.decode('utf-8')


def _date_to_python(s):
    """
    Convert date string to Python datetime.date object

    """
    y, m, d = s.split('-')
    return datetime.date(int(y), int(m), int(d))


class _SimpleTzInfo(datetime.tzinfo):
    """
    Concrete subclass of datetime.tzinfo that can represent
    the hour and minute offsets PgSQL supplies in the
    '... with time zone' types.

    """
    def __init__(self, tz):
        super(_SimpleTzInfo, self).__init__()
        if ':' in tz:
            hour, minute = tz.split(':')
        else:
            hour = tz
            minute = 0
        hour = int(hour)
        if hour < 0:
            minute = -minute
        self.offset = datetime.timedelta(hours=hour, minutes=minute)

    def dst(self, dt):
        return None

    def utcoffset(self, dt):
        return self.offset


def _time_to_python(timepart):
    """
    Convert time string to Python datetime.time object

    """
    if '+' in timepart:
        timepart, tz = timepart.split('+')
        tz = _SimpleTzInfo(tz)
    elif '-' in timepart:
        timepart, tz = timepart.split('-')
        tz = _SimpleTzInfo('-' + tz)
    else:
        tz = None

    hour, minute, second = timepart.split(':')
    if '.' in second:
        second, frac = second.split('.')
        frac = int(Decimal('0.' + frac) * 1000000)
    else:
        frac = 0

    return datetime.time(int(hour), int(minute), int(second), frac, tz)


def _timestamp_to_python(s):
    """
    Convert timestamp string to Python datetime.datetime object

    """
    datepart, timepart = s.split(' ')
    d = _date_to_python(datepart)
    t = _time_to_python(timepart)
    return datetime.datetime(d.year, d.month, d.day,
        t.hour, t.minute, t.second,
        t.microsecond, t.tzinfo)


_ESCAPE_CHARS = re.compile("[\x00-\x1f'\\\\\x7f-\xff]")
def _binary_to_pgsql(b):
    """
    Convert a python string (probably subclassed as 'Binary') to
    a PgSQL bytea.

    """
    return "E'%s'::bytea" % _ESCAPE_CHARS.sub(lambda x: '\\\\%03o' % ord(x.group(0)), b)


def _datetime_to_pgsql(dt):
    """
    Convert Python datetime.datetime to PgSQL timestamp.
    """
    if dt.tzinfo:
        return "'%s'::timestamp with time zone" % dt.isoformat(' ')
    return "'%s'::timestamp" % dt.isoformat(' ')


def _time_to_pgsql(t):
    """
    Convert Python datetime.time to PgSQL time.

    """
    if t.tzinfo:
        return "'%s'::time with time zone" % t.isoformat()
    return "'%s'::time" % t.isoformat()


################
#
# Helper classes and functions
#

def _parseDSN(s):
    """
    Parse a string containing PostgreSQL libpq-style connection info in the form:

       "keyword1=val1 keyword2='val2 with space' keyword3 = val3"

    into a dictionary::

       {'keyword1': 'val1', 'keyword2': 'val2 with space', 'keyword3': 'val3'}

    Returns empty dict if s is empty string or None.
    """
    if not s:
        return {}

    result = {}
    state = 1
    buf = ''
    for ch in s.strip():
        if state == 1:        # reading keyword
            if ch in '=':
                keyword = buf.strip()
                buf = ''
                state = 2
            else:
                buf += ch
        elif state == 2:        # have read '='
            if ch == "'":
                state = 3
            elif ch != ' ':
                buf = ch
                state = 4
        elif state == 3:        # reading single-quoted val
            if ch == "'":
                result[keyword] = buf
                buf = ''
                state = 1
            else:
                buf += ch
        elif state == 4:        # reading non-quoted val
            if ch == ' ':
                result[keyword] = buf
                buf = ''
                state = 1
            else:
                buf += ch
    if state == 4:              # was reading non-quoted val when string ran out
        result[keyword] = buf
    return result


class _LargeObject(object):
    """
    Make a PostgreSQL Large Object look somewhat like
    a Python file.  Should be created from Connection object
    open or create methods.

    """
    def __init__(self, client, fd):
        self.__client = client
        self.__fd = fd

    def __del__(self):
        if self.__client:
            self.close()

    def close(self):
        """
        Close an opened Large Object
        """
        try:
            self.__client._lo_funcall('lo_close', self.__fd)
        finally:
            self.__client = self.__fd = None

    def flush(self):
        pass

    def read(self, readlen):
        return self.__client._lo_funcall('loread', self.__fd, readlen)

    def seek(self, offset, whence):
        self.__client._lo_funcall('lo_lseek', self.__fd, offset, whence)

    def tell(self):
        r = self.__client._lo_funcall('lo_tell', self.__fd)
        return _unpack('!i', r)[0]

    def write(self, data):
        """
        Write data to lobj, return number of bytes written
        """
        r = self.__client._lo_funcall('lowrite', self.__fd, data)
        return _unpack('!i', r)[0]


class _PgType(object):
    """
    Helper class to hold info for mapping from pgsql types
    to Python objecs.

    """
    def __init__(self, name, converter, type_id):
        self.name = name
        self.converter = converter
        self.type_id = type_id
        self.oid = None

_DEFAULT_PGTYPE = _PgType('unknown', _char_to_python, 'unknown')


class _ResultSet(object):
    """
    Helper class only used internally by the Connection class for
    building up result sets.

    """
    def __init__(self):
        self.completed = None
        self.conversion = None
        self.description = None
        self.error = None
        self.null_byte_count = 0
        self.num_fields = 0
        self.rows = None
        self.messages = []

    def set_description(self, description):
        self.description = description
        self.num_fields = len(description)
        self.null_byte_count = (self.num_fields + 7) >> 3
        self.rows = []


class Connection(object):
    """
    connection objects are created by calling this module's connect function.

    """
    def __init__(self, dsn=None, username='', password='',
        host=None, dbname='', port='', opt=''):
        self.__backend_pid = None
        self.__backend_key = None
        self.__socket = None
        self.__input_buffer = ''
        self.__authenticated = 0
        self.__ready = 0
        self.__result = None
        self.__current_result = None
        self.__notify_queue = []
        self.__func_result = None
        self.__lo_funcs = {}
        self.__lo_funcnames = {}
        self._pg_types = {}
        self._oid_map = {}
        self._python_converters = []

        #
        # Come up with a reasonable default host for
        # win32 and presumably Unix platforms
        #
        if host == None:
            if sys.platform == 'win32':
                host = '127.0.0.1'
            else:
                host = '/tmp/.s.PGSQL.5432'

        args = _parseDSN(dsn)

        if not args.has_key('host'):
            args['host'] = host
        if not args.has_key('port'):
            args['port'] = port or 5432
        if not args.has_key('dbname'):
            args['dbname'] = dbname
        if not args.has_key('user'):
            args['user'] = username
        if not args.has_key('password'):
            args['password'] = password
        if not args.has_key('options'):
            args['options'] = opt

        if args['host'].startswith('/'):
            s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
            s.connect(args['host'])
        else:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect((args['host'], int(args['port'])))

        if not args['user']:
            #
            # If no userid specified in the args, try to use the userid
            # this process is running under, if we can figure that out.
            #
            try:
                import os, pwd
                args['user'] = pwd.getpwuid(os.getuid())[0]
            except:
                pass

        self.__socket = s
        self.__passwd = args['password']
        self.__userid = args['user']

        #
        # Send startup packet specifying protocol version 2.0
        #  (works with PostgreSQL 6.3 or higher?)
        #
        self.__send(_pack('!ihh64s32s64s64s64s', 296, 2, 0, args['dbname'],
                            args['user'], args['options'], '', ''))
        while not self.__ready:
            self.__read_response()

        #
        # Get type info from the backend to help put together some dictionaries
        # to help in converting Pgsql types to Python types.
        #
        self._initialize_types()
        self.__initialize_type_map()


    def __del__(self):
        if self.__socket:
            self.__send('X')
            self.__socket.close()
            self.__socket = None


    def _get_conversion(self, oid):
        """
        Given an oid of a PgSQL type, come up with a Python callable
        that will turn a string holding a representation of the value
        into a Python object

        """
        return self._oid_map.get(oid, _DEFAULT_PGTYPE).converter


    def __initialize_type_map(self):
        """
        Query the backend to find out a mapping for type_oid -> type_name, and
        then lookup the map of type_name -> conversion_function, to come up
        with a map of type_oid -> conversion_function
        """
        cur = self.cursor()
        cur.execute("SET CLIENT_ENCODING to 'UNICODE'")
        cur.execute("SET STANDARD_CONFORMING_STRINGS to 'ON'")

        cur.execute('SELECT oid, typname FROM pg_type')

        for oid, name in cur:
            self._register_oid(int(oid), name)


    def __lo_init(self):
        #
        # Make up a dictionary mapping function names beginning with "lo"
        # to function oids (there may be some non-lobject functions
        # in there, but that should be harmless)
        #
        result = self._execute("SELECT proname, oid FROM pg_proc WHERE proname like 'lo%'")
        for proname, oid in result.rows:
            self.__lo_funcs[proname] = oid
            self.__lo_funcnames[oid] = proname


    def __new_result(self):
        #
        # Start a new ResultSet
        #
        if self.__result is None:
            self.__result = []
        self.__current_result = _ResultSet()
        self.__result.append(self.__current_result)


    def _python_to_sql(self, obj):
        """
        Convert a Python object to a utf-8 string suitable for insertion
        into an SQL statement.

        """
        escape_string = True

        for klass, converter in self._python_converters:
            if isinstance(obj, klass):
                obj = converter(obj)
                escape_string = False
                break

        if obj is None:
            return 'NULL'

        if isinstance(obj, unicode):
            obj = obj.encode('utf-8')

        if escape_string and isinstance(obj, str):
            return "E'%s'" % _ESCAPE_CHARS.sub(lambda x: '\\x%02x' % ord(x.group(0)), obj)

        return obj


    def __read_bytes(self, nBytes):
        #
        # Read the specified number of bytes from the backend
        #
        while len(self.__input_buffer) < nBytes:
            d = self.__recv(4096)
            if d:
                self.__input_buffer += d
            else:
                raise OperationalError('Connection to backend closed')
        result, self.__input_buffer = self.__input_buffer[:nBytes], self.__input_buffer[nBytes:]
        return result


    def __read_string(self, terminator='\0'):
        #
        # Read a something-terminated string from the backend
        # (the terminator isn't returned as part of the result)
        #
        while True:
            if terminator in self.__input_buffer:
                result, self.__input_buffer = self.__input_buffer.split(terminator, 1)
                return result
            else:
                # need more data
                d = self.__recv(4096)
                if d:
                    self.__input_buffer += d
                else:
                    raise OperationalError('Connection to backend closed')


    def __read_response(self):
        #
        # Read a single response from the backend
        #  Looks at the next byte, and calls a more specific
        #  method the handle the rest of the response
        #
        #  PostgreSQL responses begin with a single character <c>, this
        #  method looks up a method named _pkt_<c> and calls that
        #  to handle the response
        #
        pkt_type = self.__read_bytes(1)

        try:
            getattr(self, '_pkt_' + pkt_type)()
        except AttributeError:
            raise InterfaceError('Unrecognized packet type from server: %s' % pkt_type)


    def __read_row(self, ascii=True):
        #
        # Read an ASCII or Binary Row
        #
        result = self.__current_result

        # check if we need to use longs (more than 32 fields)
        if result.null_byte_count > 4:
            null_bits = 0L
            field_mask = 128L
        else:
            null_bits = 0
            field_mask = 128

        # read bytes holding null bits and setup the field mask
        # to point at the first (leftmost) field
        if result.null_byte_count:
            for ch in self.__read_bytes(result.null_byte_count):
                null_bits = (null_bits << 8) | ord(ch)
            field_mask <<= (result.null_byte_count - 1) * 8

        # read each field into a row
        row = []
        for field_num in range(result.num_fields):
            if null_bits & field_mask:
                # field has data present, read what was sent
                field_size = _unpack('!i', self.__read_bytes(4))[0]
                if ascii:
                    field_size -= 4
                data = self.__read_bytes(field_size)
                row.append(result.conversion[field_num](data))
            else:
                # field has no data (is null)
                row.append(None)
            field_mask >>= 1

        result.rows.append(row)


    def __recv(self, bufsize):
        while True:
            try:
                return self.__socket.recv(bufsize)
            except socket.error, serr:
                if serr[0] != errno.EINTR:
                    raise


    def _register_oid(self, oid, name):
        """
        Tie a numeric type oid to a name, which we may have already
        registered a conversion function for.  If not, register a
        default conversion function.

        """
        if name in self._pg_types:
            pg_type = self._pg_types[name]
        else:
            self._pg_types[name] = pg_type = _PgType(name, _char_to_python, 'oid:%d:%s' % (oid, name))

        pg_type.oid = oid
        self._oid_map[oid] = pg_type


    def __send(self, data):
        #
        # Send data to the backend, make sure it's all sent
        #
        if self.__socket is None:
            raise InterfaceError('Connection not open')

        while data:
            try:
                nSent = self.__socket.send(data)
            except socket.error, serr:
                if serr[0] != errno.EINTR:
                    raise
                continue
            data = data[nSent:]


    def __wait_response(self, timeout):
        #
        # Wait for something to be in the input buffer, timeout
        # is a floating-point number of seconds, zero means
        # timeout immediately, < 0 means don't timeout (call blocks
        # indefinitely)
        #
        if self.__input_buffer:
            return 1

        if timeout >= 0:
            r, _, _ = select.select([self.__socket], [], [], timeout)
        else:
            r, _, _ = select.select([self.__socket], [], [])

        if r:
            return 1
        else:
            return 0



    #-----------------------------------
    #  Packet Handling Methods
    #

    def _pkt_A(self):
        #
        # Notification Response
        #
        pid = _unpack('!i', self.__read_bytes(4))[0]
        self.__notify_queue.append((self.__read_string(), pid))


    def _pkt_B(self):
        #
        # Binary Row
        #
        self.__read_row(ascii=False)


    def _pkt_C(self):
        #
        # Completed Response
        #
        self.__current_result.completed = self.__read_string()
        self.__new_result()


    def _pkt_D(self):
        #
        # ASCII Row
        #
        self.__read_row()


    def _pkt_E(self):
        #
        # Error Response
        #
        error_msg = self.__read_string()
        exc = DatabaseError(error_msg)

        if self.__current_result:
            self.__current_result.error = exc
            self.__new_result()
        else:
            raise exc


    def _pkt_G(self):
        #
        # CopyIn Response from self.stdin if available, or
        # sys.stdin   Supplies the final terminating line:
        #  '\.' (one backslash followd by a period) if it
        # doesn't appear in the input
        #
        if hasattr(self, 'stdin') and self.stdin:
            stdin = self.stdin
        else:
            stdin = sys.stdin

        lastline = None
        while True:
            s = stdin.readline()
            if (not s) or (s == '\\.\n'):
                break
            self.__send(s)
            lastline = s
        if lastline and (lastline[-1] != '\n'):
            self.__send('\n')
        self.__send('\\.\n')


    def _pkt_H(self):
        #
        # CopyOut Response to self.stdout if available, or
        # sys.stdout    Doesn't write the final terminating line:
        #  '\.'  (one backslash followed by a period)
        #
        if hasattr(self, 'stdout') and self.stdout:
            stdout = self.stdout
        else:
            stdout = sys.stdout

        while True:
            s = self.__read_string('\n')
            if s == '\\.':
                break
            else:
                stdout.write(s)
                stdout.write('\n')


    def _pkt_I(self):
        #
        # EmptyQuery Response
        #
        pass


    def _pkt_K(self):
        #
        # Backend Key data
        #
        self.__backend_pid, self.__backend_key = _unpack('!ii', self.__read_bytes(8))


    def _pkt_N(self):
        #
        # Notice Response
        #
        n = self.__read_string()
        self.__current_result.messages.append((Warning, n))


    def _pkt_P(self):
        #
        # Cursor Response
        #
        cursor = self.__read_string()


    def _pkt_R(self):
        #
        # Startup Response
        #
        code = _unpack('!i', self.__read_bytes(4))[0]
        if code == 0:
            self.__authenticated = 1
            #print 'Authenticated!'
        elif code == 1:
            raise InterfaceError('Kerberos V4 authentication is required by server, but not supported by this client')
        elif code == 2:
            raise InterfaceError('Kerberos V5 authentication is required by server, but not supported by this client')
        elif code == 3:
            self.__send(_pack('!i', len(self.__passwd)+5) + self.__passwd + '\0')
        elif code == 4:
            salt = self.__read_bytes(2)
            try:
                import crypt
            except:
                raise InterfaceError('Encrypted authentication is required by server, but Python crypt module not available')
            cpwd = crypt.crypt(self.__passwd, salt)
            self.__send(_pack('!i', len(cpwd)+5) + cpwd + '\0')
        elif code == 5:
            import md5 as hashlib

            m = hashlib.new(self.__passwd + self.__userid).hexdigest()
            m = hashlib.new(m + self.__read_bytes(4)).hexdigest()
            m = 'md5' + m + '\0'
            self.__send(_pack('!i', len(m)+4) + m)
        else:
            raise InterfaceError('Unknown startup response code: R%d (unknown password encryption?)' % code)


    def _pkt_T(self):
        #
        # Row Description
        #
        nFields = _unpack('!h', self.__read_bytes(2))[0]
        descr = []
        for i in range(nFields):
            fieldname = self.__read_string()
            oid, type_size, type_modifier = _unpack('!ihi', self.__read_bytes(10))
            descr.append((fieldname, oid, type_size, type_modifier))

        description = []
        for name, oid, size, modifier in descr:
            pg_type = self._oid_map.get(oid, _DEFAULT_PGTYPE)
            description.append((name, pg_type.type_id, None, None, None, None, None))

        # Save the field description list
        self.__current_result.set_description(description)

        # build a list of field conversion functions we can use against each row
        self.__current_result.conversion = [self._get_conversion(d[1]) for d in descr]


    def _pkt_V(self):
        #
        # Function call response
        #
        self.__func_result = None
        while True:
            ch = self.__read_bytes(1)
            if ch == '0':
                break
            if ch == 'G':
                result_size = _unpack('!i', self.__read_bytes(4))[0]
                self.__func_result = self.__read_bytes(result_size)
            else:
                raise InterfaceError('Unexpected byte: [%s] in Function call reponse' % ch)


    def _pkt_Z(self):
        #
        # Ready for Query
        #
        self.__ready = 1
        #print 'Ready for Query'


    #--------------------------------------
    # Helper func for _LargeObject
    #
    def _lo_funcall(self, name, *args):
        return apply(self.funcall, (self.__lo_funcs[name],) + args)


    #--------------------------------------
    # Helper function for Cursor objects
    #
    def _execute(self, cmd, args=None):
        if isinstance(cmd, unicode):
            cmd = cmd.encode('utf-8')

        while args is not None:
            if isinstance(args, (tuple, list)):
                # Replace plain-format markers with fixed-up tuple parameters
                cmd = cmd % tuple([self._python_to_sql(a) for a in args])
                break
            elif isinstance(args, dict):
                # replace pyformat markers with dictionary parameters
                cmd = cmd % dict([(k, self._python_to_sql(v)) for k, v in args.items()])
                break
            else:
                # Args wasn't a tuple, list, or dict: wrap it up
                # in a tuple and retry
                args = (args,)

        self.__ready = 0
        self.__result = None
        self.__new_result()
        self.__send('Q'+cmd+'\0')
        while not self.__ready:
            self.__read_response()
        result, self.__result = self.__result[:-1], None

        # Convert old-style results to what the new Cursor class expects
        result = result[0]
        result.query = cmd
        return result


    def _initialize_types(self):
        """
        Setup mappings between Python and PgSQL types.  Subclasses may
        want to override or extend this.

        """
        #
        ## Map PgSQL -> Python
        #
        self.register_pgsql(['char', 'varchar', 'text'],
            _char_to_python, STRING)
        self.register_pgsql('bytea', _binary_to_python, BINARY)

        self.register_pgsql(['int2', 'int4'], int, NUMBER)
        self.register_pgsql('int8', long, NUMBER)
        self.register_pgsql(['float4', 'float8'], float, NUMBER)
        self.register_pgsql('numeric', Decimal, NUMBER)

        self.register_pgsql('oid', long, ROWID)
        self.register_pgsql('bool', _bool_to_python, 'bool')

        self.register_pgsql('date', _date_to_python, DATETIME)
        self.register_pgsql(['time', 'timetz'], _time_to_python, DATETIME)
        self.register_pgsql(['timestamp', 'timestamptz'],
            _timestamp_to_python, DATETIME)

        #
        ## Map Python -> PgSQL
        #  the order matters, so put subclasses before superclasses
        #   (such as datetime before date)
        #
        self.register_python(datetime.datetime, _datetime_to_pgsql)
        self.register_python(datetime.date, lambda x: "'%s'::date" % str(x))
        self.register_python(datetime.time, _time_to_pgsql)
        self.register_python(Binary, _binary_to_pgsql)


    #--------------------------------------
    # Public methods
    #

    def close(self):
        """
        Close the connection now (rather than whenever __del__ is
        called).  The connection will be unusable from this point
        forward; an Error (or subclass) exception will be raised
        if any operation is attempted with the connection. The
        same applies to all cursor objects trying to use the
        connection.

        """
        if self.__socket is None:
            raise InterfaceError("Can't close connection that's not open")
        self.__del__()


    def commit(self):
        """
        Commit any pending transaction to the database.

        """
        self._execute('COMMIT')


    def cursor(self):
        """
        Get a new cursor object using this connection.

        """
        return Cursor(self)


    def funcall(self, oid, *args):
        """
        Low-level call to PostgreSQL function, you must supply
        the oid of the function, and have the args supplied as
        ints or strings.

        """
        self.__ready = 0
        self.__send(_pack('!2sIi', 'F\0', oid, len(args)))
        for arg in args:
            atype = type(arg)
            if (atype == types.LongType) and (arg >= 0):
                # Make sure positive longs, such as OIDs, get
                # sent back as unsigned ints
                self.__send(_pack('!iI', 4, arg))
            elif (atype == types.IntType) or (atype == types.LongType):
                self.__send(_pack('!ii', 4, arg))
            else:
                self.__send(_pack('!i', len(arg)))
                self.__send(arg)

        while not self.__ready:
            self.__read_response()
        result, self.__func_result = self.__func_result, None
        return result


    def lo_create(self, mode=INV_READ|INV_WRITE):
        """
        Return the oid of a new Large Object, created with the specified mode

        """
        if not self.__lo_funcs:
            self.__lo_init()
        r = self.funcall(self.__lo_funcs['lo_creat'], mode)
        return _unpack('!i', r)[0]


    def lo_open(self, oid, mode=INV_READ|INV_WRITE):
        """
        Open the Large Object with the specified oid, returns
        a file-like object

        """
        if not self.__lo_funcs:
            self.__lo_init()
        r = self.funcall(self.__lo_funcs['lo_open'], oid, mode)
        fd = _unpack('!i', r)[0]
        lobj =  _LargeObject(self, fd)
        lobj.seek(0, SEEK_SET)
        return lobj


    def lo_unlink(self, oid):
        """
        Delete the specified Large Object

        """
        if not self.__lo_funcs:
            self.__lo_init()
        self.funcall(self.__lo_funcs['lo_unlink'], oid)


    def register_pgsql(self, typenames, converter, type_id):
        """
        For a PgSQL typename or list of typenames, register a callable
        that converts strings of those values into Python objects, and
        a type_id object that will be used to identify the type in
        result descriptions.

        """
        # if the first arg is just a single string, put it into a list
        #
        if isinstance(typenames, basestring):
            typenames = [typenames]

        for name in typenames:
            #
            # See if we've already done '_register_oid' on this name
            #
            if name in self._pg_types:
                oid = self._pg_types[name].oid
            else:
                oid = None

            self._pg_types[name] = pg_type = _PgType(name, converter, type_id)

            #
            # Update oid_map if we already did _register_oid on this name
            #
            if oid is not None:
                self._oid_map[oid] = pg_type


    def register_python(self, klass, converter):
        """
        Register a callable for converting a Python object
        to a string suitable for use as a value in an SQL statement.  The
        result should ideally be a utf-8 encoded plain string, or else a
        unicode string.

        Converters are searched in the order they're added, so be sure
        to register more specific types before general times (for example,
        datetime.datetime before datetime.date).

        """
        self._python_converters.append((klass, converter))


    def rollback(self):
        """
        Cause the the database to roll back to the start of any
        pending transaction.

        """
        self._execute('ROLLBACK')


    def wait_for_notify(self, timeout=-1):
        """
        Wait for an async notification from the backend, which comes
        when another client executes the SQL command:

           NOTIFY name

        where 'name' is an arbitrary string. timeout is specified in
        floating- point seconds, -1 means no timeout, 0 means timeout
        immediately if nothing is available.

        In practice though the timeout is a timeout to wait for the
        beginning of a message from the backend. Once a message has
        begun, the client will wait for the entire message to finish no
        matter how long it takes.

        Return value is a tuple: (name, pid) where 'name' string
        specified in the NOTIFY command, and 'pid' is the pid of the
        backend process that processed the command.

        Raises a PostgreSQL_Timeout exception on timeout

        """
        while True:
            if self.__notify_queue:
                result, self.__notify_queue = self.__notify_queue[0], self.__notify_queue[1:]
                return result
            if self.__wait_response(timeout):
                self.__read_response()
            else:
                raise PostgreSQL_Timeout()

#
# DB API 2.0 extension:
#   All exception classes defined by the DB API standard should be
#   exposed on the Connection objects as attributes (in addition
#   to being available at module scope).
#
#   These attributes simplify error handling in multi-connection
#   environments.

Connection.Error = Error
Connection.Warning = Warning
Connection.InterfaceError = InterfaceError
Connection.DatabaseError = DatabaseError
Connection.InternalError = InternalError
Connection.OperationalError = OperationalError
Connection.ProgrammingError = ProgrammingError
Connection.IntegrityError = IntegrityError
Connection.DataError = DataError
Connection.NotSupportedError = NotSupportedError


class Cursor(object):
    """
    Cursor objects are created by calling a connection's cursor() method,
    and are used to manage the context of a fetch operation.

    Cursors created from the same connection are not isolated, i.e., any changes
    done to the database by a cursor are immediately visible by the
    other cursors.

    Cursors created from different connections are isolated.

    """
    def __init__(self, conn):
        """
        Create a cursor from a given bpgsql Connection object.

        """
        self.arraysize = 1
        self.connection = conn
        self.description = None
        self.lastrowid = None
        self.messages = []
        self.rowcount = -1
        self.rownumber = None
        self.__rows = None
        self.query = ''


    def __iter__(self):
        """
        Return an iterator for the result set this cursor holds.

        """
        return self


    def close(self):
        """
        Close the cursor now (rather than whenever __del__ is
        called).  The cursor will be unusable from this point
        forward; an Error (or subclass) exception will be raised
        if any operation is attempted with the cursor.

        """
        self.__init__(None)


    def execute(self, cmd, args=None):
        """
        Execute a database operation (query or command).
        Parameters may be provided as sequence or
        mapping or singleton argument and will be bound to variables
        in the operation. Variables are specified in format (...WHERE foo=%s...)
        or pyformat (...WHERE foo=%(name)s...) paramstyles.

        """
        self.rowcount = -1
        self.rownumber = None
        self.description = None
        self.lastrowid = None
        self.__rows = None
        self.messages = []

        result = self.connection._execute(cmd, args)

        if result.error:
            raise result.error

        self.description = result.description
        self.__rows = result.rows
        self.messages = result.messages
        self.query = result.query

        try:
            words = result.completed.split(' ')
            self.rowcount = int(words[-1])
            if words[0] == 'INSERT':
                try:
                    self.lastrowid = int(words[-2])
                except:
                    pass
        except:
            pass

        if self.__rows is not None:
            self.rowcount = len(self.__rows)
            self.rownumber = 0


    def executemany(self, cmd,  seq_of_parameters):
        """
        Execute a database operation (query or command) against
        all parameter sequences or mappings found in the
        sequence seq_of_parameters.

        """
        for p in seq_of_parameters:
            self.execute(cmd, p)

        # Don't want to leave the value of the last execute() call
        self.rowcount = -1


    def fetchall(self):
        """
        Fetch all remaining rows of a query set, as a list of lists.
        An empty list is returned if no more rows are available.
        An Error is raised if no result set exists

        """
        if self.__rows is None:
            raise Error('No result set available')

        return self.fetchmany(self.rowcount - self.rownumber)


    def fetchone(self):
        """
        Fetch the next row of the result set as a list of fields, or None if
        no more are available.  Will raise an Error if no
        result set exists.

        """
        try:
            return self.next()
        except StopIteration:
            return None


    def fetchmany(self, size=None):
        """
        Fetch all the specified number of rows of a query set, as a list of lists.
        If no size is specified, then the cursor's .arraysize property is used.
        An empty list is returned if no more rows are available.
        An Error is raised if no result set exists

        """
        if self.__rows is None:
            raise Error('No result set available')

        if size is None:
            size = self.arraysize

        n = self.rownumber
        self.rownumber += size
        return self.__rows[n:self.rownumber]


    def next(self):
        """
        Return the next row of a result set.  Raises StopIteration
        if no more rows are available.  Raises an Error if no result set
        exists.

        """
        if self.__rows is None:
            raise Error('No result set available')

        n = self.rownumber
        if n >= self.rowcount:
            raise StopIteration

        self.rownumber += 1
        return self.__rows[n]


    def scroll(self, n, mode='relative'):
        """
        Scroll the cursor in the result set to a new position according
        to mode.

        If mode is 'relative' (default), value is taken as offset to
        the current position in the result set, if set to 'absolute',
        value states an absolute target position.

        An IndexError will be raised in case a scroll operation would
        leave the result set. In this case, the cursor position unchanged.

        """
        if self.__rows is None:
            raise Error('No result set available')

        if mode == 'relative':
            newpos = self.rownumber + n
        elif mode == 'absolute':
            newpos = n
        else:
            raise ProgrammingError('Unknown scroll mode [%s]' % mode)

        if (newpos < 0) or (newpos >= self.rowcount):
            raise IndexError('scroll(%d, "%s") target position: %d outsize of range: 0..%d' % (n, mode, newpos, self.rowcount-1))

        self.rownumber = newpos


    def setinputsizes(self, sizes):
        """
        Intented to be used before a call to execute() or executemany() to
        predefine memory areas for the operation's parameters.

        Doesn't actually do anything in this client.

        """
        pass


    def setoutputsize(self, size, column=None):
        """
        Set a column buffer size for fetches of large columns
        (e.g. LONGs, BLOBs, etc.).

        Doesn't actually do anything in this client.

        """
        pass


def connect(dsn=None, username='', password='',
            host=None, dbname='', port='', opt='', **extra):
    """
    Connect to a PostgreSQL database.

    The dsn, if used, is in the format used by the PostgreSQL libpq C library, which is one
    or more "keyword=value" pairs separated by spaces.  Values that are single-quoted may
    contain spaces.  Spaces around the '=' chars are ignored.  Recognized keywords are:

          host, port, dbname, user, password, options

    For example:

          cnx = bpgsql.connect("host=127.0.0.1 dbname=mydb user=jake")

    """
    return Connection(dsn, username, password, host, dbname, port, opt)

# ---- EOF ----
