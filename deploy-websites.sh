#!/bin/bash

copy_site() {
    local site_name="$1"
    local remote_path="$2"
    local local_path="$script_dir/src/$site_name"

    echo "Synchronizing $local_path to $remote_path..."
    rsync -avz "$local_path/" "$remote_path/"
}

main() {
    script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"

    copy_site "matthieucourt.xyz" "root@nargothrond.xyz:/var/www/matthieucourt.xyz"
    copy_site "code.nargothrond.xyz" "root@nargothrond.xyz:/var/www/code.nargothrond.xyz"
    copy_site "food.nargothrond.xyz" "root@nargothrond.xyz:/var/www/food.nargothrond.xyz"
    copy_site "nargothrond.xyz" "root@nargothrond.xyz:/var/www/nargothrond.xyz"

    echo "All websites have been synchronized."
}

main
