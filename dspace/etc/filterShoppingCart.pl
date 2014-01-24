#! /usr/bin/perl

# Usage: filterShoppingCart.pl previousIDFile shoppingCartFile
#
# Returns a copy of the shoppingCartFile, but filtered to remove items from the previousIDFile.
# This is used to filter an export from the shoppingcart table so it is suitable for use in mothly reports.

$prevIdFile = $ARGV[0];
($shoppingCartFile = $ARGV[1]) || die "Usage: filterShoppingCart.pl previousIDFile shoppingCartFile\n";

my @prevIds = do {
    open my $fh, "<", $prevIdFile
        or die "could not open $prevIdFile: $!";
    <$fh>;
};

open(my $sfh, '<:encoding(UTF-8)', $shoppingCartFile)
    or die "Could not open file '$shoppingCartFile' $!";
 
# Iterate througgh each row of the shoppingCartFile
while (my $row = <$sfh>) {
    chomp $row;
    $row =~ /^([^,]+),/;
    $cartId = $1;

    # if the cartId is not in the previousIDFile, print this row
    if (not grep( /^$cartId$/, @prevIds ) ) {
	print "$row\n";
    }
}

