#!/bin/bash

SCRIPT_DIR="$(dirname "$0")"
SOURCE_DIR="$SCRIPT_DIR/mywebsite/*"
DESTINATION_DIR="root@nargothrond.xyz:/var/www/mysite"

rsync -avz $SOURCE_DIR $DESTINATION_DIR
