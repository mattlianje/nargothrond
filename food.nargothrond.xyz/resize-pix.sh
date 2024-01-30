#!/bin/bash

# Uses ImageMagick to resize food pictures to a width of 500 pixels
format_food_pics() {
    local dir="$1/src/pix"

    command -v convert &> /dev/null || { echo "ImageMagick is not installed. Please install it to continue."; return 1; }
    [ -d "$dir" ] || { echo "Directory $dir does not exist."; return 1; }

    cd "$dir" || return 1

    for img in recipe*.jpeg; do
        [ -f "$img" ] && convert "$img" -resize 500x "$img" || { echo "Failed to resize $img"; return 1; }
    done

    cd - || return 1
    echo "All applicable images in $dir have been resized."
    return 0
}

main() {
    local script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
    format_food_pics "$script_dir"
}

main
