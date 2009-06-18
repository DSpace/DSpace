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

my @embargo_dump = EmbargoDump();

foreach( @embargo_dump )
    {
        print  "$_\n";
    }



################################################
# subroutines ##################################
################################################

sub EmbargoDump
{
    my $arg = <<EOF;
SELECT
   DISTINCT ON (h1.handle) 
   h1.handle,
   i1.item_id,
   bs.bitstream_id,
   (SELECT
      dc.text_value 
    FROM 
      dcvalue dc
    WHERE 
      dc.dc_type_id='64' AND
      dc.item_id=i1.item_id
    LIMIT 1) as title,
   (SELECT
      dc.text_value 
    FROM 
      dcvalue dc
    WHERE 
      dc.dc_type_id='2' AND
      dc.item_id=i1.item_id
    LIMIT 1) as advisor,
    (SELECT
       dc.text_value
    FROM
       dcvalue dc
    WHERE
       dc.dc_type_id='3' AND
       dc.item_id=i1.item_id
    LIMIT 1) as author,
    (SELECT
       dc.text_value
    FROM
       dcvalue dc
    WHERE
       dc.dc_type_id='69' AND
       dc.item_id=i1.item_id
    LIMIT 1) as department,
    (SELECT
       dc.text_value
    FROM
       dcvalue dc
    WHERE
       dc.dc_type_id='66' AND
       dc.item_id=i1.item_id
    LIMIT 1) as type,
    rp.end_date
FROM
   handle h1,
   item i1, 
   item2bundle i2b1,
   bundle2bitstream b2b1, 
   bitstream bs, 
   resourcepolicy rp 
WHERE 
   h1.resource_id=i1.item_id AND
   i1.item_id=i2b1.item_id AND
   i2b1.bundle_id=b2b1.bundle_id AND
   b2b1.bitstream_id=bs.bitstream_id AND
   bs.bitstream_id=rp.resource_id AND
   (rp.end_date > CURRENT_DATE OR
   rp.end_date IS NULL) AND
   rp.epersongroup_id = '164' 
;
EOF

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
