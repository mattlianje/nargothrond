#!/bin/bash

update_site() {
    local site_name="$1"
    local remote_path="$2"
    local local_path="$script_dir/$site_name"

    [ "$site_name" == "food.nargothrond.xyz" ] && local_path="${local_path}/src"

    echo "Updating CSS and JS in $local_path..."

    mkdir -p "$local_path/css" "$local_path/js"

    rm -rf "$local_path/css/"*
    rm -rf "$local_path/js/"*

    cp -R "$css_dir/" "$local_path/"
    cp -R "$js_dir/" "$local_path/"

    echo "Synchronizing $local_path to $remote_path..."
    rsync -avz "$local_path/" "$remote_path/"
}

main() {
    script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
    css_dir="$script_dir/css"
    js_dir="$script_dir/js"

    update_site "matthieucourt.xyz" "root@nargothrond.xyz:/var/www/matthieucourt.xyz"
    update_site "code.nargothrond.xyz" "root@nargothrond.xyz:/var/www/code.nargothrond.xyz"
    update_site "food.nargothrond.xyz" "root@nargothrond.xyz:/var/www/food.nargothrond.xyz"
    update_site "nargothrond.xyz" "root@nargothrond.xyz:/var/www/nargothrond.xyz"

    echo "All websites have been updated and synchronized."
}

main
