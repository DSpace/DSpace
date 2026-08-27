package StatsUtils;

# Copyright 2002, The Regents of The University of Michigan, All Rights Reserved
#
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject
# to the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.

# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
# IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
# CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
# TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
# SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

BEGIN
{
  require "strict.pm";
  strict::import();
}


# Perl CPAN modules
use CGI;

# set umask (for file uploads)
umask 000;

sub GetCollname
  {
    my ( $dbhP, $coll_id ) = @_;


    my $statement = qq{Select text_value from metadatavalue where dspace_object_id ='$coll_id' and metadata_field_id=64};
    
    my $sth = $dbhP->prepare($statement)
      or die "Couldn't prepare statement: " . $dbhP->errstr;

    # Read the matching records and print them out
    $sth->execute()             # Execute the query
      or die "Couldn't execute statement: " . $sth->errstr;

    my ( @data, $name );
    while (@data = $sth->fetchrow_array()) {
      $name = $data[0];
    }
    $sth->finish;

    return $name;
   
  }


sub GetTotalPerPage
  {
    return 100;

  }



sub GetSQLAddedSearch
  {

    my ( $collid, $enddt, $startdt, $type, $searchvalue ) = @_;
    
    $startdt =~ s,\/,-,gs;
    $enddt =~ s,\/,-,gs;
    $enddt .= qq{99999999999999999999};

    my $SQLAddedSearch;
    if ( $type eq 'publisher' )
      {
	
	$SQLAddedSearch     = qq{select distinct S.display_authors, S.display_title, S.handle, S.dateadded, S.numofbits, S.publisher from statsdata S , collection2item CI, handle H where CI.collection_id='$collid' and CI.item_id = H.resource_id and H.handle =S.handle and S.dateadded <='$enddt' and S.dateadded >= '$startdt' and LOWER(S.$type) like '%$searchvalue%' order by S.display_title, S.handle};    
      }
    else
      {
	$SQLAddedSearch     = qq{select distinct S.display_authors, S.display_title, S.handle, S.dateadded, S.numofbits, S.publisher from statsdata S , collection2item CI, handle H where CI.collection_id='$collid' and CI.item_id = H.resource_id and H.handle =S.handle and S.dateadded <='$enddt' and S.dateadded >= '$startdt' and S.$type like '%$searchvalue%' order by S.display_title, S.handle};    
      }
    return $SQLAddedSearch;

  }

sub GetSQLAddedNonSearch
{

  my ( $collid, $enddt, $startdt ) = @_;

  $startdt =~ s,\/,-,gs;
  $enddt =~ s,\/,-,gs;
  $enddt .= qq{99999999999999999999};

  my $SQLAddedNonSearch  = qq{select distinct S.display_authors, S.display_title, S.handle, S.dateadded, S.numofbits, S.publisher from statsdata S, collection2item CI, handle H where CI.collection_id='$collid' and CI.item_id = H.resource_id and H.handle =S.handle and S.dateadded <='$enddt' and S.dateadded >= '$startdt' order by S.display_title, S.handle};

  return $SQLAddedNonSearch;

}

sub GetSQLAccessSearch
  {
    my ( $collid, $enddt, $startdt, $type, $searchvalue ) = @_;




    my $SQLAccessSearch;
    if ( $type eq 'publisher' )
      {

	$SQLAccessSearch    = qq{select C.colldt, C.collid_uuid, C.collname, C.handle, C.title, C.publisher, C.inside_count, C.out_count, C.bit_count,  C.itemuminside, C.itemnonuminside,  C.itemumoutside, C.itemnonumoutside, C.bitum, C.bitnonum, C.itemoutsidereferer, C.bitreferer from consolidatedstatstable C, statsdata S, collection2item CI, handle H where CI.collection_id='$collid' and CI.item_id = H.resource_id and H.handle =C.handle and C.colldt<='$enddt' and C.colldt>='$startdt' and C.Handle = S.handle and LOWER(C.$type) like '%$searchvalue%' order by C.title, C.handle};
      }
    else
      {
	$SQLAccessSearch    = qq{select C.colldt, C.collid_uuid, C.collname, C.handle, C.title, C.publisher, C.inside_count, C.out_count, C.bit_count,  C.itemuminside, C.itemnonuminside,  C.itemumoutside, C.itemnonumoutside, C.bitum, C.bitnonum, C.itemoutsidereferer, C.bitreferer from consolidatedstatstable C, statsdata S, collection2item CI, handle H where CI.collection_id='$collid' and CI.item_id = H.resource_id and H.handle =C.handle  and C.colldt<='$enddt' and C.colldt>='$startdt' and C.Handle = S.handle and S.$type like '%$searchvalue%' order by C.title, C.handle};
      }
    return $SQLAccessSearch;

  }

sub GetSQLAccessNonSearch
  {
    my ( $collid, $enddt, $startdt ) = @_;

    my $SQLAccessNonSearch = qq{select C.colldt, C.collid_uuid, C.collname, C.handle, C.title, C.publisher, C.inside_count, C.out_count, C.bit_count,  C.itemuminside, C.itemnonuminside,  C.itemumoutside, C.itemnonumoutside, C.bitum, C.bitnonum, C.itemoutsidereferer, C.bitreferer from consolidatedstatstable C , collection2item CI, handle H where CI.collection_id='$collid' and CI.item_id = H.resource_id and H.handle =C.handle  and C.colldt<='$enddt' and C.colldt>='$startdt' order by C.title, C.handle};

    return $SQLAccessNonSearch;

  }



sub GetSQLAccessSearchFiles
  {
    my ( $collid, $enddt, $startdt, $type, $searchvalue ) = @_;

    my $SQLAccessSearch;
    if ( $type eq 'publisher' )
      {
	$SQLAccessSearch    = qq{select B.colldt, B.collid_uuid, B.filename, B.handle, B.bitum, B.bitnonum, B.bitreferer, S.title, S.authors from bitspecificdata B, statsdata S, collection2item CI, handle H where CI.collection_id='$collid' and CI.item_id = H.resource_id and H.handle =B.handle  and B.colldt<='$enddt' and B.colldt>='$startdt' and B.Handle = B.handle and LOWER(B.$type) like '%$searchvalue%' order by B.handle, B.filename};
      }
    else
      {
	$SQLAccessSearch    = qq{select B.colldt, B.collid_uuid, B.filename, B.handle, B.bitum, B.bitnonum, B.bitreferer, S.title, S.authors from bitspecificdata B, statsdata S, collection2item CI, handle H where CI.collection_id='$collid' and CI.item_id = H.resource_id and H.handle =B.handle and B.colldt<='$enddt' and B.colldt>='$startdt' and B.Handle = S.handle and S.$type like '%$searchvalue%' order by B.handle, B.filename};
      }
    return $SQLAccessSearch;

  }

sub GetSQLAccessNonSearchFiles
  {
    my ( $collid, $enddt, $startdt ) = @_;

    my $SQLAccessNonSearch = qq{select B.colldt, B.collid, B.filename, B.handle, B.bitum, B.bitnonum, B.bitreferer, S.title, S.authors from bitspecificdata B, collection2item CI, handle H, statsdata S  where CI.collection_id='$collid' and CI.item_id = H.resource_id and H.handle =B.handle  and B.Handle = S.handle and B.colldt<='$enddt' and B.colldt>='$startdt' order by B.handle, B.filename};

    return $SQLAccessNonSearch;

  }


sub GetRestOfData
  {
    my ( $dbhP, $handle ) = @_;

    my $statement = qq{select display_authors, display_title, dateadded, numofbits from statsdata where handle='$handle'};
    
    my $sth = $dbhP->prepare($statement)
      or die "Couldn't prepare statement: " . $dbhP->errstr;

    # Read the matching records and print them out
    $sth->execute()             # Execute the query
      or die "Couldn't execute statement: " . $sth->errstr;

    my $authors = '';
    my $dateAdded = '';
    my $bitCount = 0;
    my @data;
    my ( $authors, $title, $dateAdded, @data );
    while (@data = $sth->fetchrow_array()) {
      $authors           = $data[0];
      $title             = $data[1];
      $dateAdded         = $data[2];
      $bitCount          = $data[3];
    }
    $sth->finish;


    #Sometimes the list of authors is so long that it's hard to display them.
    #only allow 4 authors 
    if ( $authors =~ m,.*?\;.*?\;.*?\;.*?\;.*, )
      {
         $authors =~ s,(.*?;.*?;.*?;.*?;).*,$1 \.\.\. ,s;
      }

    return ( $authors, $title, $dateAdded, $bitCount );
    
  }


sub GetDownloadDataAdded 
  {
    my ( $dbhP, $dateRange, $statement ) = @_;

    my $sth = $dbhP->prepare($statement)
      or die "Couldn't prepare statement: " . $dbhP->errstr;

    # Read the matching records and print them out
    $sth->execute()             # Execute the query
      or die "Couldn't execute statement: " . $sth->errstr;

    my $buffer = qq{dates\thandle\ttitle\tauthors\tpublisher\tdateAdded\tbitCount\n};
    my ($authors, $title, $handle, $dateAdded, $bitCount, $publisher, @data );
    while ( @data = $sth->fetchrow_array() )  {
      $authors          = $data[0];
      $title            = $data[1];
      $handle           = $data[2];
      $dateAdded        = $data[3];
      $bitCount         = $data[4];
      $publisher        = $data[5];

      $buffer .= qq{$dateRange\t$handle\t$title\t$authors\t$publisher\t$dateAdded\t$bitCount\n};

    }
    $sth->finish;

    return $buffer;

  }



sub GetItemId
  {
    my ( $dbhP, $handle ) = @_;

    my $statement = qq{select resource_id from handle where handle='$handle'};

    my $sth = $dbhP->prepare($statement)
      or die "Couldn't prepare statement: " . $dbhP->errstr;

    # Read the matching records and print them out
    $sth->execute()             # Execute the query
      or die "Couldn't execute statement: " . $sth->errstr;
    
    my ( $item_id, @data );
    while (@data = $sth->fetchrow_array()) {
      $item_id = $data[0];
    }
    $sth->finish;
    
    return $item_id;

  }

sub GetRestOfDataForPR
  {
    my ( $dbhP, $handle ) = @_;

    my $item_id = &GetItemId ( $dbhP, $handle );

    my $doi = &GetValue ( $dbhP, $item_id, 24 );
    my $issn = &GetValue ( $dbhP, $item_id, 21 );
    
    return ( $doi, $issn );

  }


sub GetValue 
  {
    my ( $dbhP, $item_id, $id ) = @_;
    
    if ( ( $item_id ) && ( $id ) )
      {
	#my $statement = qq{select text_value from metadatavalue where item_id=$item_id and metadata_field_id=$id};
	my $statement = qq{select text_value from metadatavalue where dspace_object_id='$item_id' and metadata_field_id=$id};

	my $sth = $dbhP->prepare($statement)
	  or die "Couldn't prepare statement: " . $dbhP->errstr;

	# Read the matching records and print them out
	$sth->execute()             # Execute the query
	  or die "Couldn't execute statement: " . $sth->errstr;
    
	my ( $value, @data );
	while (@data = $sth->fetchrow_array()) {
	  $value .= $data[0];
	  $value .= qq{ ;};
	  
	}
	$sth->finish;
	
	if ( $value =~ m,(.*)\;, )
	  {
	    $value =~ s,(.*)\;,$1,gs;
	  }
	
	return $value;
      }
    else
      { 
	return '';
      }
    

  }


sub GetCount 
  {
    my ( $dbhP ) = @_;

    my $count = 1;

    #Get the next available WorkingDir
    my %rootHash;
    my $statement = qq{SELECT max(count)  FROM ZipDir};
    my $sth = $dbhP->prepare($statement)
      or die "Couldn't prepare statement: " . $dbhP->errstr;

    # Read the matching records and print them out
    $sth->execute()             # Execute the query
      or die "Couldn't execute statement: " . $sth->errstr;
    
    my ( @data );
    while (@data = $sth->fetchrow_array()) {
      $count = $data[0];
    }
    $sth->finish;

    my $NextCount = $count + 1;
    if ($NextCount == 1000)
      {
        my $statement = qq{DELETE FROM ZipDir;};
	&ProcessSQL ( $dbhP, $statement );
        $NextCount = 1;
      }
    my $statement = qq{INSERT INTO ZipDir values ($NextCount)};
    &ProcessSQL ( $dbhP, $statement );

    return $count;

  }

sub ProcessSQL
  {
    my ( $dbhP, $statement ) = @_;

    my $sth = $dbhP->prepare($statement)
      or die "Couldn't prepare statement: " . $dbhP->errstr;

    # Read the matching records and print them out
    $sth->execute()             # Execute the query
      or die "Couldn't execute statement: " . $sth->errstr;
    $sth->finish;
  }


sub GetLineAccessForDownload
  {
    my ( $dbhP, $collid, $dateRange, $PHandle, $PHandle,  $PInside, $PInsideUM, $PInsideNONUM, $POutside, $POutsideUM, $POutsideNONUM, $PBit, $PBitUM, $PBitNONUM, $PPublisher, $PItemOutsideReferer, $PBitReferer ) = @_;

    $PItemOutsideReferer =~ s,null\; ,,gs;
    $PItemOutsideReferer =~ s,\;+ *,;,gs;
    $PItemOutsideReferer =~ s,\;+,;,gs;
    if ( $PItemOutsideReferer =~ m,^\;.*, )
    {
        $PItemOutsideReferer =~ s,^\;(.*),$1,s;
    }
    $PItemOutsideReferer =~ s,\;, \; ,gs;
    

    $PBitReferer =~ s,null\; ,,gs;
    $PBitReferer =~ s,\;+ *,;,gs;
    $PBitReferer =~ s,\;+,;,gs;
    if ( $PBitReferer =~ m,^\;.*, )
    {
        $PBitReferer =~ s,^\;(.*),$1,s;
    }
    $PBitReferer =~ s,\;, \; ,gs;

    my $Line;
    my ( $authors, $title, $dateAdded, $bitCount ) = &GetRestOfData ( $dbhP, $PHandle );
    if ( $collid == 6 )
      {
	my ( $doi, $issn ) = &GetRestOfDataForPR ( $dbhP, $PHandle );
	$Line =  qq{$dateRange\t$PHandle\t$title\t$authors\t$PInside\t$PInsideUM\t$PInsideNONUM\t$POutside\t$POutsideUM\t$POutsideNONUM\t$PItemOutsideReferer\t$PBit\t$PBitUM\t$PBitNONUM\t$PBitReferer\t$PPublisher\t$dateAdded\t$bitCount\t$doi\t$issn\n};
      }
    else
      {
	$Line =  qq{$dateRange\t$PHandle\t$title\t$authors\t$PInside\t$PInsideUM\t$PInsideNONUM\t$POutside\t$POutsideUM\t$POutsideNONUM\t$PItemOutsideReferer\t$PBit\t$PBitUM\t$PBitNONUM\t$PBitReferer\t$PPublisher\t$dateAdded\t$bitCount\n};
      }

    return $Line;
  }



sub GetLineAccessForDownloadFiles
  {
    my ( $dbhP, $collid, $dateRange, $PHandle, $Pfilename,  $PBit, $PBitUM, $PBitNONUM,  $PBitReferer, $title, $author ) = @_;


    $PBitReferer =~ s,null\; ,,gs;
    $PBitReferer =~ s,\;+ *,;,gs;
    $PBitReferer =~ s,\;+,;,gs;
    if ( $PBitReferer =~ m,^\;.*, )
    {
        $PBitReferer =~ s,^\;(.*),$1,s;
    }
    $PBitReferer =~ s,\;, \; ,gs;
    $PBitReferer =~ s,\t, ,gs;
    $PBitReferer =~ s,\n, ,gs;


    my $Line =  qq{$dateRange\t$PHandle\t$title\t$author\t$Pfilename\t$PBit\t$PBitUM\t$PBitNONUM\t$PBitReferer\t\n};

    return $Line;
  }



sub GetDownloadDataAccess
{
  my ( $dbhP, $collid, $dateRange, $statement ) = @_;

   my $sth = $dbhP->prepare($statement)
      or die "Couldn't prepare statement: " . $dbhP->errstr;

    # Read the matching records and print them out
    $sth->execute()             # Execute the query
      or die "Couldn't execute statement: " . $sth->errstr;

    my $SHandle;
    my $PHandle;
    my $PTitle;
    my $PInside = 0;
    my $POutside = 0;
    my $PBit = 0;

    my $PInsideUM = 0;
    my $POutsideUM = 0;
    my $PBitUM = 0;
    my $PInsideNONUM = 0;
    my $POutsideNONUM = 0;
    my $PBitNONUM = 0;

    my $PPublisher = '';
    my $Line;
    my $CreateLine = 0;
    my $RecCount = 0;

    my $PItemOutsideReferer;
    my $PBitReferer;

    if ( $collid == 6 )
      {
	$Line =  qq{dates\thandle\ttitle\tauthors\tInside\tInsideUm\tInsideNonUM\tOutside\tOutsideUM\tOutsideNonUM\tItemOutsideReferer\tBit\tBitUm\tBitNonUM\tBitReferer\tPublisher\tdateAdded\tbitCount\tdoi\tissn\n};
      }
    else
      {
	$Line =  qq{dates\thandle\ttitle\tauthors\tInside\tInsideUM\tInsideNonUM\tOutside\tOutsideUM\tOutsideNonUM\tItemOutsideReferer\tBit\tBitUM\tBitNonUM\tBitReferer\tPublisher\tdateAdded\tbitCount\t\n};
      }
    my $buffer;
   $buffer = $Line;

    my ( $colldt, $collid, $collname, $handle, $title, $publisher, $inside_count, $out_count, $bit_count,  $insideum_count, $insidenonum_count, $outum_count,  $outnonum_count, $bitum_count, $bitnonum_count, $itemoutsidereferer, $bitreferer, @data );
    while ( @data = $sth->fetchrow_array() )  {
      $colldt           = $data[0];
      $collid           = $data[1];
      $collname         = $data[2];
      $publisher        = $data[5];

      $handle           = $data[3];
      $title            = $data[4];
      $inside_count     = $data[6];
      $out_count        = $data[7];
      $bit_count        = $data[8];

      $insideum_count        = $data[9];
      $insidenonum_count     = $data[10];
      $outum_count           = $data[11];
      $outnonum_count        = $data[12];
      $bitum_count           = $data[13];
      $bitnonum_count        = $data[14];

      $itemoutsidereferer    = $data[15];
      $bitreferer            = $data[16];

      if ( $PHandle eq $handle )
      {
	$PPublisher = $publisher;
	$PInside = $PInside + $inside_count;
	$POutside = $POutside + $out_count;
	$PBit = $PBit + $bit_count;

	$PInsideUM = $PInsideUM + $insideum_count;
	$PInsideNONUM = $PInsideNONUM + $insidenonum_count;
	$POutsideUM = $POutsideUM + $outum_count;
	$POutsideNONUM = $POutsideNONUM + $outnonum_count;
	$PBitUM = $PBitUM + $bitum_count;
	$PBitNONUM = $PBitNONUM + $bitnonum_count;

	$PItemOutsideReferer = qq{$PItemOutsideReferer $itemoutsidereferer};
	$PBitReferer = qq{$PBitReferer $bitreferer};


	$SHandle = $handle;

	$CreateLine = 1;

      }
      else
      {
	if ( $CreateLine )
	  {
	    if ( $dateRange ne 'count' )
	      {
		$Line = &GetLineAccessForDownload ( $dbhP, $collid, $dateRange, $PHandle, $PHandle,  $PInside, $PInsideUM, $PInsideNONUM, $POutside, $POutsideUM, $POutsideNONUM, $PBit, $PBitUM, $PBitNONUM, $PPublisher, $PItemOutsideReferer, $PBitReferer ) ;
		$buffer .= $Line;
	      }
	    $RecCount = $RecCount + 1;
	  }

	$PPublisher = $publisher;
	$PInside = $inside_count;
	$POutside = $out_count;
	$PBit = $bit_count;

	$PInsideUM = $insideum_count;
	$PInsideNONUM = $insidenonum_count;
	$POutsideUM = $outum_count;
	$POutsideNONUM = $outnonum_count;
	$PBitUM = $bitum_count;
	$PBitNONUM = $bitnonum_count;

	$PItemOutsideReferer = $itemoutsidereferer;
	$PBitReferer = $bitreferer;

	$CreateLine = 1;
      }
      $PHandle = $handle;
    }
    $sth->finish;

    #The last record has not been put in
    $RecCount = $RecCount + 1;
  
     my $TotalPages = $RecCount/&GetTotalPerPage();
     $TotalPages = int ( $TotalPages );
     if ( $RecCount % &GetTotalPerPage() )
     {
       $TotalPages = $TotalPages + 1;
     }

    if ( $CreateLine )
      {
	if ( $PHandle eq $SHandle )
	  {
#	    $PPublisher = $publisher;
#	    $PInside = $PInside + $inside_count;
#	    $POutside = $POutside + $out_count;
#	    $PBit = $PBit + $bit_count;
	    
#	    $PInsideUM = $PInsideUM + $insideum_count;
#	    $PInsideNONUM = $PInsideNONUM + $insidenonum_count;
#	    $POutsideUM = $POutsideUM + $outum_count;
#	    $POutsideNONUM = $POutsideNONUM + $outnonum_count;
#	    $PBitUM = $PBitUM + $bitum_count;
#	    $PBitNONUM = $PBitNONUM + $bitnonum_count;

#	    $PItemOutsideReferer = qq{$PItemOutsideReferer $itemoutsidereferer};
#            $PBitReferer = qq{$PBitReferer $bitreferer};

	  }

	if ( $dateRange ne 'count' )
	  {
	    $Line = &GetLineAccessForDownload ( $dbhP, $collid, $dateRange, $PHandle, $PHandle,  $PInside, $PInsideUM, $PInsideNONUM, $POutside, $POutsideUM, $POutsideNONUM, $PBit, $PBitUM, $PBitNONUM, $PPublisher, $PItemOutsideReferer, $PBitReferer ) ;
	    $buffer .= $Line;
	  }
      }
  
  return ( $buffer, $RecCount, $TotalPages );

}



sub GetDownloadDataAccessFiles
{
  my ( $dbhP, $collid, $dateRange, $statement ) = @_;

   my $sth = $dbhP->prepare($statement)
      or die "Couldn't prepare statement: " . $dbhP->errstr;

    # Read the matching records and print them out
    $sth->execute()             # Execute the query
      or die "Couldn't execute statement: " . $sth->errstr;

    my $SHandle;
    my $SFilename;
    my $PHandle;
    my $PFilename;
    my $PBit = 0;

    my $PBitUM = 0;
    my $PBitNONUM = 0;

    my $Line;
    my $CreateLine = 0;
    my $RecCount = 0;

    my $PBitReferer;
  
  $Line =  qq{dates\thandle\ttitle\tauthor\tfilename\tBit\tBitUm\tBitNonUM\tBitReferer\t\n};

  my $buffer;
  $buffer = $Line;

  my ( $colldt, $collid, $filename, $handle, $bit_count, $bitum_count, $bitnonum_count, $bitreferer, $title, $author, @data );
    while ( @data = $sth->fetchrow_array() )  {

      $colldt           = $data[0];
      $collid           = $data[1];
      $filename         = $data[2];


      #title and author ????
      $title           = $data[7];
      $title           =~ s,\t, ,gs;
      $title           =~ s,\n, ,gs;

      $author          = $data[8];
      $author          =~ s,\t, ,gs;
      $author          =~ s,\n, ,gs;

      $handle           = $data[3];
 
      $bitum_count           = $data[4];
      $bitnonum_count        = $data[5];

      $bitreferer            = $data[6];

      if ( ( $PHandle eq $handle ) && ( $PFilename eq $filename ) )
      {
	$PBitUM = $PBitUM + $bitum_count;
	$PBitNONUM = $PBitNONUM + $bitnonum_count;

	$PBitReferer = qq{$PBitReferer $bitreferer};

	$SHandle = $handle;
	$SFilename = $filename;

	$CreateLine = 1;

      }
      else
      {
	if ( $CreateLine )
	  {
	    if ( $dateRange ne 'count' )
	      {
		$PBit = $PBitUM + $PBitNONUM;
		$Line = &GetLineAccessForDownloadFiles ( $dbhP, $collid, $dateRange, $PHandle, $PFilename,  $PBit, $PBitUM, $PBitNONUM, $PBitReferer, $title, $author ) ;
		$buffer .= $Line;
	      }
	    $RecCount = $RecCount + 1;
	  }


	$PBitUM = $bitum_count;
	$PBitNONUM = $bitnonum_count;

	$PBitReferer = $bitreferer;

	$CreateLine = 1;
      }
      $PHandle = $handle;
      $PFilename = $filename;
    }
    $sth->finish;

    if ( $CreateLine )
      {
	if ( ( $PHandle eq $SHandle ) && ( $PFilename eq $SFilename ) )
	  {

#	    $PBitUM = $PBitUM + $bitum_count;
#	    $PBitNONUM = $PBitNONUM + $bitnonum_count;

#            $PBitReferer = qq{$PBitReferer $bitreferer};

	  }

	if ( $dateRange ne 'count' )
	  {
		$PBit = $PBitUM + $PBitNONUM;
		$Line = &GetLineAccessForDownloadFiles ( $dbhP, $collid, $dateRange, $PHandle, $PFilename,  $PBit, $PBitUM, $PBitNONUM, $PBitReferer, $title, $author ) ;
	    $buffer .= $Line;
	  }
      }
  
  return ( $buffer );

}



# ---------------------------------------------------------------------
1; # Truth
