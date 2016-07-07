#!/bin/sh
if [ $# -eq 0 ]
  then
    echo "Usage: $0 data_dir"
    exit 1
fi
TS=`date`
DATE=`date +"%Y-%m"`
SRC=$1
DST_HOST="the_dest_server"
DST_DIR="/some_where/backup/data"
echo $TS>${SRC}/LAST_UPDATE
rsync -avz -e ssh $SRC $DST_HOST:$DST_DIR/$DATE

