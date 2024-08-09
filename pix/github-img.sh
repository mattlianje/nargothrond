#!/bin/bash


# Define the directory and the GitHub logo
DIR="."
GITHUB_LOGO="$DIR/github.png"
TEMP_LOGO="$DIR/github_resized.png"

# Clean up old generated images
find "$DIR" -name "*_github.png" -exec rm {} \;

# Iterate over all PNG files in the current directory
for IMAGE in "$DIR"/*.png; do
    # Skip the GitHub logo itself
    if [[ "$IMAGE" == "$GITHUB_LOGO" ]]; then
        continue
    fi

    # Define the output image name
    OUTPUT_IMAGE="${IMAGE%.png}_with_github.png"

    # Get dimensions of the original image
    WIDTH=$(magick identify -format %w "$IMAGE")
    HEIGHT=$(magick identify -format %h "$IMAGE")

    # Calculate the size for the GitHub logo (30% of the original image's height)
    LOGO_HEIGHT=$((HEIGHT / 3))

    # Resize the GitHub logo while maintaining transparency
    magick convert "$GITHUB_LOGO" -background none -resize x$LOGO_HEIGHT "$TEMP_LOGO"

    # Get dimensions of the resized GitHub logo
    LOGO_WIDTH=$(magick identify -format %w "$TEMP_LOGO")
    LOGO_HEIGHT=$(magick identify -format %h "$TEMP_LOGO")

    # Create a new image with increased width to accommodate the GitHub logo
    COMBINED_WIDTH=$((WIDTH + LOGO_WIDTH))
    magick convert -size ${COMBINED_WIDTH}x${HEIGHT} xc:none "$IMAGE" -geometry +0+0 -composite "$TEMP_LOGO" -geometry +${WIDTH}+0 -gravity SouthWest -composite "$OUTPUT_IMAGE"

    echo "Created $OUTPUT_IMAGE"
done

# Clean up temporary files
rm "$TEMP_LOGO"
