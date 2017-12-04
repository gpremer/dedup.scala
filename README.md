= dedup =

A quick script to find duplicate image/video files.

It works by comparing file size and sha256 hashes. Will only work on
Unixy systems as it heavily relies on system commands.

The script expects 2 directories. The first is used to find all
duplicates the second to limit the duplicates to that directory.

The output is rough. I may improve on that when the need arises.

Oh, and the script needs a scala runtime...
