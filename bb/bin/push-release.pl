#!/usr/bin/env perl

use strict;
use warnings;

use FindBin;
use IPC::Cmd qw[run can_run];
use Getopt::Long;

use constant APK_DIR => "$FindBin::Bin/../build/outputs/apk/release/";
use constant APK => "bb-release.apk";
use constant OUTPUT_JSON => 'output.json';
use constant GCS_BUCKET => 'gs://burner-board-apk/BurnerBoardApps';

my $debug   = 0;
my $help    = 0;
my $dir     = APK_DIR;
my $apk     = APK;
my $bucket  = GCS_BUCKET;
my $jq      = can_run('jq');
my $gsutil  = can_run('gsutil');

GetOptions(
    debug       => \$debug,
    "dir=s"     => \$dir,
    "apk=s"     => \$apk,
    "jq=s"      => \$jq,
    "gsutil=s"  => \$gsutil,
    "bucket=s"  => \$bucket,
    help        => \$help,
) or die usage();

if( $help ) {
    print usage();
    exit 0;
}

### Make sure jq & gsutil are in our path
unless( can_run($jq) ) {
    die "You need to install jq: https://stedolan.github.io/jq/download/";
}

unless( can_run($gsutil) ) {
    die "You need to install gsutil: https://cloud.google.com/storage/docs/gsutil_install";
}

### Find version
my $version;
{   print "\nDetermining APK version\n";
    unless( scalar run(
        command => [$jq, ".[0].apkInfo.versionCode", $dir .'/'. OUTPUT_JSON],
        verbose => $debug,
        buffer  => \$version,
    ) ) {
        die "Could not get version information: $version\n";
    }

    chomp($version);
    print "Found APK version: $version\n";
}

### Upload
{   print "\nUploading APK to GCS\n";
    my $upload;
    my $target = $bucket .'/bb-'. $version . '.apk';

    unless( scalar run(
        command => [ $gsutil, 'cp', '-a', 'public-read', $dir .'/'. $apk, $target ],
        verbose => $debug,
        buffer  => \$upload,
    ) ) {
        die "Could not upload APK to GCS: $upload\n";
    }

    print "Uploaded release to: $target\n";
}

sub usage {
    my $me = $FindBin::Script;
    return qq!
  $0 [--help] |
     [--debug]
     [--dir=/path/to/apk/release]   # default: $dir
     [--apk=name.apk]               # default: $apk
     [--jq=/path/to/jq]             # default: $jq
     [--gsutil=/path/to/gsutil]     # default: $gsutil
     [--bucket=gs://bucket/path]    # default: $bucket
    \n!;
}
