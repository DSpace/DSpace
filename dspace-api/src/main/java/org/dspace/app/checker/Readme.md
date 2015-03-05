# org.dspace.app.checker additions

There are two new applications   ChecksumWorker and ChecksumHistoryWorker.

ChecksumWorker implements a more flexible version of ChecksumChecker which is not quite as silent about what it does.

It can restrict itself to working on bitstreams

 * in a given community, collection, or item,

 * whose last check result is a given value

 * whose last check result is not one of a list of given values

 * that have been checked last longer than a given time frame ago

 * that have been checked last within  a given time  from the current time

The ChecksumHistoryChecker works on the stored check sum results, eg it counts, prints, or deletes. It restricts itself to selected
check sum results in the same way as the ChecksumWorker  does.

# Examples

## checksum :  default parameters

The following checks a single bitstream, the one that was checked last the longest ago and whose check resulted in a value other than
BITSTREAM_NOT_FOUND, BITSTREAM_MARKED_DELETED, CHECKSUM_ALGORITHM_INVALID

~~~~
> dspace checksum
# org.dspace.checker.CheckBitstreamIterator(without_result=[BITSTREAM_NOT_FOUND,BITSTREAM_MARKED_DELETED,CHECKSUM_ALGORITHM_INVALID])
# Action check
# Max-Count 1
# Printing  m for CHECKSUM_MATCH, d for BITSTREAM_MARKED_DELETED, and E in all other cases
m
# worked on 1 bitstreams
~~~~

## checksum :  check only check bitstreams last checked more than 4 weeks ago - work on at most 100 bitstreams

The following checks only bitstreams, which were last checked longer than 4 weeks ago and  whose last check resulted in a value other than
BITSTREAM_NOT_FOUND, BITSTREAM_MARKED_DELETED, CHECKSUM_ALGORITHM_INVALID. The app chooses the 100 bitstreams whose last check date is the longest ago.

~~~~
> dspace checksum --count 100 --before 4w
# org.dspace.checker.CheckBitstreamIterator(without_result=[BITSTREAM_NOT_FOUND,BITSTREAM_MARKED_DELETED,CHECKSUM_ALGORITHM_INVALID], before=Thu Feb 05 11:39:09 EST 2015)
# Action check
# Max-Count 100
# Printing  m for CHECKSUM_MATCH, d for BITSTREAM_MARKED_DELETED, and E in all other cases
mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm
mmmmdmmmmmEmmmmmmmmm
# worked on 100 bitstreams
~~~~

There is a verbose option that prints more detailed information as checks are performed.

## checksum :  find the check date furthest into the past of all bitstreams not deleted

~~~~
> dspace checksum --do print  --exclude_result BITSTREAM_MARKED_DELETED
# org.dspace.checker.CheckBitstreamIterator(without_result=[BITSTREAM_MARKED_DELETED])
# Action print
# Max-Count 1
1 BITSTREAM.55954 CHECKSUM_MATCH internalId=125422936394914596080845538261956084146  delete=false  lastDate=2014-02-06 03:39:28.804

# worked on 1 bitstreams
~~~~

## checksum :  count the number of bitstreams in trouble

The following counts the number of bitstreams that are not deleted whose last check result indicates a problem.
A match_count other than 0 is a problem.

~~~~
> dspace checksum --do count -x BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH
# org.dspace.checker.CheckBitstreamIterator(without_result=[BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH])
# Action count
# Max-Count 1
match_count=25	without_result=[BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH]
~~~~

You can also count the number of problems that arose in a given time frame, eg between 2 and 4 weeks ago
~~~~
> dspace checksum --do count -x BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH --after 4w --before 2w
~~~~
or within the last 24 hours
~~~~
> dspace checksum --do count -x BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH --after 24h
~~~~


## print info on bitstreams in trouble

print info on last check results for bitstreams whose last chech result was  neither BITSTREAM_MARKED_DELETED nor CHECKSUM_MATCH


~~~~
> dspace checksum --do print -x BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH --count all
# org.dspace.checker.CheckBitstreamIterator(without_result=[BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH])
# Action print
# Max-Count -1
1 BITSTREAM.88134 CHECKSUM_NO_MATCH internalId=100001824138360574590188345356919067125  delete=false  lastDate=2015-02-18 14:47:35.973
2 BITSTREAM.55930 BITSTREAM_NOT_FOUND internalId=153938287619075174428757274234693753357  delete=false  lastDate=2015-03-05 11:08:20.911
...
~~~~

Printing in verbose mode includes information of each bitstreams enclosing item, collection, and communities:
~~~~
1 BITSTREAM.88134 CHECKSUM_NO_MATCH internalId=100001824138360574590188345356919067125  delete=false  lastDate=2015-02-18 14:47:35.973
1 BITSTREAM.88134 CHECKSUM_NO_MATCH algo=MD5 expected=changed2e68b0937967681638fbf6fc2 calculated=641f8452e68b0927967681638fbf6fc2
1 BITSTREAM.88134 CHECKSUM_NO_MATCH ITEM:88435/SNRTHS41014 COLLECTION:88435/scr01v405s9414 COMMUNITY:88435/kt1v5b7 COMMUNITY:88435/kt5h7c5
~~~~

You can also print information of stored check results as opposed to just the last result:

~~~~
> dspace checksum --do history -x BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH --count all
# org.dspace.checker.CheckBitstreamIterator(without_result=[BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH])
# Action history
# Max-Count -1
BITSTREAM.88134 result=CHECKSUM_NO_MATCH lastDate=Wed Feb 18 00:00:00 EST 2015
BITSTREAM.88134 result=CHECKSUM_MATCH lastDate=Wed Feb 05 00:00:00 EST 2014
BITSTREAM.55930 result=BITSTREAM_NOT_FOUND lastDate=Thu Mar 05 00:00:00 EST 2015
BITSTREAM.55930 result=CHECKSUM_MATCH lastDate=Thu Feb 06 00:00:00 EST 2014
~~~~

The following lists bitstreams whose files could not be found

~~~~
> dspace checksum --do print --include_result BITSTREAM_NOT_FOUND --count all
# org.dspace.checker.CheckBitstreamIterator(with_result=BITSTREAM_NOT_FOUND)
# Action print
# Max-Count -1
1 BITSTREAM.55930 result=BITSTREAM_NOT_FOUND lastDate=Thu Mar 05 00:00:00 EST 2015
...
~~~~

After restoring files you can recompute the checksum as follows
    > dspace checksum --do print --include_result BITSTREAM_NOT_FOUND --count all


## print and count stored checksum results

DSpace keeps not only the last check sum results. It stores results for later reference.
The checksumhistory command can print, count, and delete selected check sum results.
Run without parameters it simply counts all stored checksum results.  As in the checksum command results can be filtered:

Count all  BITSTREAM_NOT_FOUND results
~~~~
> dspace checkhistory --include_result BITSTREAM_NOT_FOUND
~~~~

Count results that are not BITSTREAM_MARKED_DELETED or CHECKSUM_MATCH for bitstreams in a the collection with handle 88435/SNRTHS41014
~~~~
> dspace checkhistory --exclude_result BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH --root 88435/SNRTHS41014
~~~~

Print results that are not BITSTREAM_MARKED_DELETED or CHECKSUM_MATCH for bitstreams in a the collection with handle 88435/SNRTHS41014
~~~~
> dspace checkhistory --exclude_result BITSTREAM_MARKED_DELETED,CHECKSUM_MATCH --root 88435/SNRTHS41014 --do print
~~~~


## delete stored checksum results

At some point the amount of stored checksums grows beyond what yoy want or need to keep.
You can selectively delete stored checksum in the same way that you can print or count them.

To delete all CHECKSUM_MATCH results that are older than 5 years do
~~~~
> dspace checkhistory --include_result CHECKSUM_MATCH --do delete  --before 5y
~~~~



# Command Usage

## ChecksumWorker
~~~~
usage: ChecksumWorker:

 -a,--after             Work on bitstreams last checked after (current
                        time minus given duration)
 -b,--before            Work on bitstreams last checked before (current
                        time minus given duration)
 -c,--count             Work on at most the given number of bitstreams,
                        positive number or 'all'
 -d,--do                action to apply to bitstreams
 -h,--help              Print this help
 -i,--include_result    Work on bitstreams whose last result matches the
                        given result
 -l,--loop              Work on bitstreams whose last result is not one of
                        BITSTREAM_NOT_FOUND, BITSTREAM_MARKED_DELETED, CHECKSUM_ALGORITHM_INVALID
 -r,--root              Work on bitstream in given Community, Collection,
                        Item, or on the given Bitstream, give root as handle or TYPE.ID)
 -v,--verbose           Be verbose
 -x,--exclude_result    Work on bitstreams whose last result is not one of
                        the given results (use a comma separated list)

Available do actions that may be applied to selected bitstreams:
	check, print, history, delete, count
	default action: check
	check  	computes a checkum for selected bitstreams and compares with the epected checksum
	print  	prints selected bitstreams, the last check result, its internal id, and check date
	count  	counts how may bitstreams match with the given parameters
	history	prints the complete checksum history of selected bitstreams

Available checksum results:
	BITSTREAM_NOT_FOUND
	BITSTREAM_INFO_NOT_FOUND
	BITSTREAM_NOT_PROCESSED
	BITSTREAM_MARKED_DELETED
	CHECKSUM_MATCH
	CHECKSUM_NO_MATCH
	CHECKSUM_ALGORITHM_INVALID

Give duration using y(year) w(week), d(days), h(hours) m(minutes):
	4w for 4 weeks, 30d for 30 days, or 15m for 15 minutes
~~~~

## ChecksumHistoryWorker
~~~~
usage: ChecksumHistoryWorker:

 -a,--after             Work on bitstreams last checked after (current
                        time minus given duration)
 -b,--before            Work on bitstreams last checked before (current
                        time minus given duration)
 -d,--do                action to apply to bitstreams
 -h,--help              Print this help
 -i,--include_result    Work on bitstreams whose last result matches the
                        given result
 -r,--root              Work on bitstream in given Community, Collection,
                        Item, or on the given Bitstream, give root as handle or TYPE.ID)
 -x,--exclude_result    Work on bitstreams whose last result is not one of
                        the given results (use a comma separated list)

Available do actions that may be applied to selected bitstreams:
	delete, print, count
	default action: count
	print  	prints selected checksum history entries
	count  	counts how may checksum history entries match with the given parameters
	delete 	deletes selected checksum history entries"

Available checksum results:
	BITSTREAM_NOT_FOUND
	BITSTREAM_INFO_NOT_FOUND
	BITSTREAM_NOT_PROCESSED
	BITSTREAM_MARKED_DELETED
	CHECKSUM_MATCH
	CHECKSUM_NO_MATCH
	CHECKSUM_ALGORITHM_INVALID

Give duration using y(year) w(week), d(days), h(hours) m(minutes):
	4w for 4 weeks, 30d for 30 days, or 15m for 15 minutes

~~~~
