# dedup.scala #

A quick script to find duplicate image/video files.

It works by comparing file size and sha256 hashes. Will only work on
Unixy systems as it heavily relies on system commands.

The script expects 2 directories. The first is used to find all
duplicates the second to limit the duplicates to that directory.

The output is quite rough. First there's the list of all duplicate files in parentheses, one group at a time. After that comes a list of all duplicate files in the second directory given on the command line. I may improve on the output when the need arises. For now it does what I need.

Oh, and the script needs a scala runtime...
