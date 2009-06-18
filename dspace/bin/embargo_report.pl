#!/usr/bin/env perl

###########################################################################
#
# embargo_report
#
# Version: $Revision: 1.0 $
#
# Date: $Date: 2007/02/22 $
#
# Based heavily on dspace-info.pl Copyright (c) 2002, Hewlett-Packard 
# Company and Massachusetts Institute of Technology.  All rights reserved.
#
###########################################################################

# Simple script to produce reports based on the number of embargoed
# items in DSpace

use strict;

############################################
# display report ###########################
############################################


my @bitstreams_with_end_dates = FindBitstreamsWithPoliciesEndDates();
my @embargoes_by_advisor = FindEmbargoesByAdvisor();
my @collections_with_embargoes = FindCollectionEmbargoes();
my $total_bitstreams = 0;

print "<h1>Embargoes Report</h1>";
print "<h2>Date: " . localtime() . "</h2>";

print "<h3>Embargoes by end date</h3>";
print "<table>";
print "<tr><th>Date</th><th>Number</th>";
foreach( @bitstreams_with_end_dates )
    {
        my ($date, $number) = split /\|/;
        print "<tr><td>$date</td><td align=center>$number</td></tr>";
        $total_bitstreams += $number;
    }
    print "</table>";

print "<h3>Total bitstreams with embargoes: $total_bitstreams</h3>";

print "<h3>Embargoes by collection</h3>";
print "<table>";
foreach ( @collections_with_embargoes)
  {
        my ($collection, $number) = split /\|/;
       print "<tr><td>$collection</td><td>$number</td></tr>"; 
    }
    print "</table>"; 

print "<h3>Embargoes by Advisor</h3>";
print "<table>";
foreach ( @embargoes_by_advisor)
  {
        my ($advisor, $number) = split /\|/;
       print "<tr><td>$advisor</td><td>$number</td></tr>"; 
    }
    print "</table>"; 


################################################
# subroutines ##################################
################################################

# find collection sizes
sub FindCollectionEmbargoes
{
    my $arg =
        "SELECT c1.name, COUNT(c2i1.item_id) FROM " .
            "collection c1, collection2item c2i1, item2bundle i2b1, " .
            "bundle2bitstream b2b1, bitstream bs, resourcepolicy rp " .
        "WHERE " .
            "c1.collection_id=c2i1.collection_id AND " .
            "c2i1.item_id=i2b1.item_id AND " .
            "i2b1.bundle_id=b2b1.bundle_id AND " .
            "b2b1.bitstream_id=bs.bitstream_id AND " .
	    "bs.bitstream_id=rp.resource_id AND " .
            "(rp.end_date > CURRENT_DATE OR " .
            "rp.end_date IS NULL) AND " .
            "rp.epersongroup_id = '164' AND " .
            "rp.end_date IS NOT NULL " .
        "GROUP BY c1.name ORDER BY c1.name";

    return ExecuteSQL( $arg );
}

sub FindEmbargoesByAdvisor
{
    my $arg =
        "SELECT dc.text_value, COUNT(i1.item_id) FROM " .
            "item i1, dcvalue dc, item2bundle i2b1, " .
            "bundle2bitstream b2b1, bitstream bs, resourcepolicy rp " .
        "WHERE " .
            "dc.dc_type_id='2' AND " .
            "dc.item_id=i1.item_id AND " .
            "i1.item_id=i2b1.item_id AND " .
            "i2b1.bundle_id=b2b1.bundle_id AND " .
            "b2b1.bitstream_id=bs.bitstream_id AND " .
	    "bs.bitstream_id=rp.resource_id AND " .
            "rp.end_date > CURRENT_DATE AND " .
            "rp.epersongroup_id = '164' AND " .
            "rp.end_date IS NOT NULL " .
        "GROUP BY dc.text_value ORDER BY dc.text_value";

    return ExecuteSQL( $arg );
}

# find bitstreams with end dates
sub FindBitstreamsWithPoliciesEndDates
{
my $arg = "SELECT resourcepolicy.end_date, COUNT(resourcepolicy.end_date) FROM bitstream,resourcepolicy" .
                 " WHERE" .
                 " bitstream.bitstream_id = resourcepolicy.resource_id" .
                 " AND resourcepolicy.end_date IS NOT NULL" .
                 " AND resourcepolicy.end_date > CURRENT_DATE" .
                 " AND resourcepolicy.epersongroup_id = '164'" .
                 " GROUP BY end_date ORDER BY end_date"; 

    return ExecuteSQL( $arg );
}

# execute SQL, return array of results 
sub ExecuteSQL
{
    my $arg = shift;

    # do the SQL statement
    open SQLOUT, "psql -d dspace -p 8001 -A -c '$arg' | ";

    # slurp up the results
    my @results = <SQLOUT>;
    chomp( @results );
    close SQLOUT;

    # remove first and last rows
    pop @results;
    shift @results;

    return @results;
}
