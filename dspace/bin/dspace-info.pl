#!/usr/bin/env perl

###########################################################################
#
# dspace-info.pl
#
# Version: $Revision$
#
# Date: $Date$
#
# Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
# Institute of Technology.  All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#
# - Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
#
# - Redistributions in binary form must reproduce the above copyright
# notice, this list of conditions and the following disclaimer in the
# documentation and/or other materials provided with the distribution.
#
# - Neither the name of the Hewlett-Packard Company nor the name of the
# Massachusetts Institute of Technology nor the names of their
# contributors may be used to endorse or promote products derived from
# this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
# OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
# TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
# USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
# DAMAGE.
#
###########################################################################

# Simple script to check some DSpace site statistics, such
#  as the size of key directories, counts of significat DSpace
#  objects, etc.
#
# No arguments, just run dspace-info.pl 


use strict;
use File::Find;


###############################################
# CONFIGURE THESE VARIABLES! ##################
###############################################

# where is DSpace installed?
my $dspace_dir = "/dspace";

#where is the DATA for the database tables stored?
my $database_dir = "/dspace/database/data";



# error out if cannot locate above directories
die "Cannot find dspace directory tree $dspace_dir"     if( ! -d $dspace_dir   );
die "Cannot find database data directory $database_dir" if( ! -d $database_dir );


#############################################
# Begin statistics ##########################
#############################################


# find DSpace directories ###################

my $assetstore_dir = GetConfigParameter( "assetstore.dir" );
my $search_dir     = GetConfigParameter( "search.dir"     );
my $history_dir    = GetConfigParameter( "history.dir"    );
my $logs_dir       = GetConfigParameter( "log.dir"        );


# count DSpace objects ######################

my $bitstream_count     = CountRows( "bitstream"     );
my $bundle_count        = CountRows( "bundle"        );
my $collection_count    = CountRows( "collection"    );
my $community_count     = CountRows( "community"     );
my $dcvalue_count       = CountRows( "dcvalue"       );
my $eperson_count       = CountRows( "eperson"       );
my $item_count          = CountRows( "item"          );
my $handle_count        = CountRows( "handle"        );
my $group_count         = CountRows( "epersongroup"  );
my $workflowitem_count  = CountRows( "workflowitem"  );
my $workspaceitem_count = CountRows( "workspaceitem" );

# find sizes of dspace directories ##########

my $assetstore_size = DirectorySize( $assetstore_dir );
my $search_size     = DirectorySize( $search_dir     );
my $history_size    = DirectorySize( $history_dir    );
my $logs_size       = DirectorySize( $logs_dir       );
my $database_size   = DirectorySize( $database_dir   );

# look for missing logos ####################
my @communities_without_logos = FindCommunitiesWithoutLogos();
my @collections_without_logos = FindCollectionsWithoutLogos();

# look for deleted bitstreams
my @deleted_bitstreams        = FindDeletedBitstreams();

# look for bitstreams without policies
my @bitstreams_without_policies = FindBitstreamsWithoutPolicies();

# look for empty groups
my @empty_groups = FindEmptyGroups();

# look at subscriptions ####################
my @subscribed_collections       = FindSubscribedCollections();
my $subscribed_collections_count = $#subscribed_collections + 1;
my $subscription_count           = CountRows( "subscription" );
my @subscribers                  = FindSubscribers();
my $subscriber_count             = $#subscribers + 1;


############################################
# display report ###########################
############################################


print "DSpace site statistics for site: '" . GetConfigParameter("dspace.name") . "'\n";
print "Date: " . localtime() . "\n";
print "\n"; 
print "Size of Important Directories:\n";
print "\tAsset store:           $assetstore_size MB\n";
print "\tDatabase:              $database_size MB\n";
print "\tSearch Directory:      $search_size MB\n";
print "\tHistory Directory:     $history_size MB\n";
print "\tLogs Directory:        $logs_size MB\n";
print "\n";
print "\n";
print "Counts of Important DSpace Objects:\n";
print "\tEPeople:               $eperson_count\n";
print "\tCommunities:           $community_count\n";
print "\tCollections:           $collection_count\n";
print "\tItems:                 $item_count\n";
print "\n";
print "\tBundles:               $bundle_count\n";
print "\tBitstreams:            $bitstream_count\n";
print "\tDublin Core Elements:  $dcvalue_count\n";
print "\tEPerson Groups:        $group_count\n";
print "\tHandles:               $handle_count\n";
print "\tSubmissions Active:    $workspaceitem_count\n";
print "\tWorkflows Active:      $workflowitem_count\n";
print "\n";
print "\tSubscriptions:         $subscription_count\n";
print "\tSubscribers:           $subscriber_count\n";
print "\tSubscribed Collections: $subscribed_collections_count\n";
print "\n";
print "Potential Problems:\n";

# only show problems if they exist! ######

if( $deleted_bitstreams[0] > 0 )
{
    print "  Deleted bitstreams:    $deleted_bitstreams[0]\n";
    print "  Size of deleted:       $deleted_bitstreams[1] MB\n"; 
    print "\n";
}
if( $#communities_without_logos >= 0 )
{
    my $count = $#communities_without_logos + 1;

    print "  Communities without Logos:  $count\n";
    foreach( @communities_without_logos )
    {
	my ($id, $name) = split /\|/;
	print "\t$id\t$name\n";
    } 
    print "\n";
}

if( $#collections_without_logos >= 0 )
{
    my $count = $#collections_without_logos + 1;

    print "  Collections without Logos:  $count\n";
    foreach( @collections_without_logos )
    {
	my ($id, $name) = split /\|/;
	print "\t$id\t$name\n";
    } 
    print "\n";
}

if( $#empty_groups >= 0 )
{
    my $count = $#empty_groups + 1;

    print "  Empty Groups:  $count\n";
    foreach( @empty_groups )
    {
	my ($id, $name) = split /\|/;
	print "\t$id\t$name\n";
    }
    print "\n";
}

if( $#bitstreams_without_policies >= 0 )
{
    my $count = $#bitstreams_without_policies + 1;

    print "  Bitstreams without policies:  $count\n";
    foreach( sort { $a <=> $b } @bitstreams_without_policies )
    {
	my ($id) = split /\|/;
	print "\t$id\n";
    } 
    print "\n";
}


################################################
# subroutines ##################################
################################################


sub CountRows
{
    my $tablename = shift;
    my $arg = "SELECT COUNT(*) from $tablename";

    my @results = ExecuteSQL( $arg );

    # make sure it's a number
    return 0 + $results[0];
}

# get a value from the dspace.cfg file
#  only gets the first match!
sub GetConfigParameter
{
    my $dirname = shift;
    my $return_value = "";

    open CONFIG, "grep $dirname $dspace_dir/config/dspace.cfg |";

    my $result = <CONFIG>;

#    chomp $result;

    if( $result =~ m/^.+\s*=\s*(.*)\s*$/ )
    {
       $return_value = $1;
    }

    close CONFIG;

    return $return_value;
}

# process a directory and find the size of all of its files
#  in megabytes
sub DirectorySize
{
    my $directory = shift;
   
    my $sum = 0;

    find sub { $sum += -s }, $directory;

    return int($sum/(1024*1024)*10)/10;
}


# find subscribed to collections, return results
sub FindSubscribedCollections
{
    my $arg = "SELECT DISTINCT ON (collection_id) collection_id FROM subscription";

    return ExecuteSQL( $arg );
}

# find all subscribers
sub FindSubscribers
{
    # FIXME - DISTINCT ON is non-standard SQL
    my $arg = "SELECT DISTINCT ON (eperson_id) eperson_id FROM subscription";

    return ExecuteSQL( $arg );
}

# find communities with no logos - return id, name
sub FindCommunitiesWithoutLogos
{
    my $arg = "SELECT community_id, name FROM community WHERE logo_bitstream_id IS NULL"; 

    return ExecuteSQL( $arg );
}

# find collections with no logos - return id, name
sub FindCollectionsWithoutLogos
{
    my $arg = "SELECT collection_id, name FROM collection WHERE logo_bitstream_id IS NULL"; 

    return ExecuteSQL( $arg );
}


# find bitstreams with no policies
sub FindBitstreamsWithoutPolicies
{
    my $arg = "SELECT bitstream_id FROM bitstream WHERE deleted<>true AND bitstream_id NOT IN (SELECT resource_id FROM resourcepolicy WHERE resource_type_id=0)"; 

    return ExecuteSQL( $arg );
}


# find empty eperson groups
sub FindEmptyGroups
{
    my $arg = "SELECT eperson_group_id, name from epersongroup WHERE eperson_group_id NOT IN (SELECT eperson_group_id FROM epersongroup2eperson)";

    return ExecuteSQL( $arg );
}

sub FindDeletedBitstreams
{
    my $arg = "SELECT COUNT(*) from bitstream where deleted=true";
    my @deleted_count = ExecuteSQL( $arg );

    $arg = "SELECT SUM(size) from bitstream where deleted=true";
    my @deleted_size = ExecuteSQL( $arg );

    return ($deleted_count[0], int($deleted_size[0]/(1024*1024)*10)/10);
}

# other possibilities
#  orphaned bundles
#  orphaned bitstreams
#  orphaned items
#  orphaned collections

# execute SQL, return array of results 
sub ExecuteSQL
{
    my $arg = shift;

    # do the SQL statement
    open SQLOUT, "psql -d dspace -A -c '$arg' | ";

    # slurp up the results
    my @results = <SQLOUT>;
    chomp( @results );
    close SQLOUT;

    # remove first and last rows
    pop @results;
    shift @results;

    return @results;
}
