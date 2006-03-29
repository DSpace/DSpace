#!/usr/bin/perl -w

###########################################################################
#
# checkkeys.pl
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

# Simple tool to compare two properties files, finding out which keys
# are present in one file but not the other.  It 


use strict;

if ($#ARGV != 1)
{
	die "Usage: checkkeys.pl <master> <tocheck>\n";
}

die "Can't open $ARGV[0]" if (! -e $ARGV[0]);
die "Can't open $ARGV[1]" if (! -e $ARGV[1]);


my $master_keys = read_keys($ARGV[0]);
my $tocheck_keys = read_keys($ARGV[1]);

print "IN $ARGV[0] BUT NOT IN $ARGV[1]:\n";
print_missing($master_keys, $tocheck_keys);

print "\n\nIN $ARGV[1] BUT NOT IN $ARGV[0]:\n";
print_missing($tocheck_keys, $master_keys);


sub print_missing
{
	my ($reference, $test) = @_;

	my $k;

	foreach $k (sort keys %{$reference})
	{
		if (!defined ${$test}{$k})
		{
			print "  " . $k . "\n";
		}
	}
}

sub read_keys
{
	my ($file) = @_;
	my %k;

	open MSGIN, "$file";

	while (<MSGIN>)
	{
		# remove line endings
		chomp();
		chop() if (/\r/);

		my $line = $_;

		if ($line =~ /.+jsp\./)
		{
			print STDERR "Warning: $file line $.: Suspect two keys on same line\n";
		}

		if ($line =~ /=/)
		{
			$line =~ /^([^\s]+)\s*=/;
			my $propname = $1;
			$k{$propname} = 1;
		}
		elsif ($line ne "" && $line !~ /^#/)
		{
			print STDERR "Warning: $file line $.: Line isn't a comment, property, or blank line\n";
		}
	}

	close MSGIN;
	return \%k;
}
