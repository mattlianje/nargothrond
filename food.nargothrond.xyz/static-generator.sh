#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)"
OUTPUT_DIR="$SCRIPT_DIR"
RECIPES_DIR="$SCRIPT_DIR/recipes"
INDEX_HTML="$OUTPUT_DIR/index.html"
GOLDEN_RATIO_HEIGHT=$(echo "500 / 1.618" | bc) # Height calculated based on the golden ratio

# Function to convert a markdown file to HTML
convert_md_to_html() {
    input_md="$1"
    output_html="${OUTPUT_DIR}/$(basename "${input_md%.md}.html")"

    # Start of the HTML document
    cat <<EOF > "$output_html"
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>${input_md%.md}</title>
    <link rel="stylesheet" href="https://latex.now.sh/style.min.css" />
    <link rel="stylesheet" href="css/styles.css" />
    <script src="js/script.js"></script>
</head>
<body class="light-theme">
EOF

    # Process the Markdown file
    while IFS= read -r line; do
        # Handle bold text
        line=$(echo "$line" | sed -e 's/\*\*\([^*]*\)\*\*/<strong>\1<\/strong>/g')

        # Check for Markdown image syntax and convert to HTML image tag
        if [[ "$line" =~ !\[.*\]\(.*\) ]]; then
            alt_text=$(echo "$line" | sed -n 's/!\[\(.*\)\](.*).*/\1/p')
            img_url=$(echo "$line" | sed -n 's/!.*(\(.*\)).*/\1/p')
            echo "<img src=\"$img_url\" alt=\"$alt_text\" style=\"width: 500px;\">" >> "$output_html"
        else
            case "$line" in
                \{*})
                    title=$(echo "$line" | sed -n 's/{\(.*\)}/\1/p')
                    echo "<h1>$title</h1>" >> "$output_html"
                    ;;
                \#\ *)
                    echo "<h1>${line:2}</h1>" >> "$output_html"
                    ;;
                \#\#\ *)
                    echo "<h2>${line:3}</h2>" >> "$output_html"
                    ;;
                \#\#\#\ *)
                    echo "<h3>${line:4}</h3>" >> "$output_html"
                    ;;
                -\ *)
                    echo "<li>${line:2}</li>" >> "$output_html"
                    ;;
                "")
                    echo "<p></p>" >> "$output_html"
                    ;;
                *)
                    echo "<p>$line</p>" >> "$output_html"
                    ;;
            esac
        fi
    done < "$input_md"

    # End of the HTML document
    cat <<EOF >> "$output_html"
<br>
<a href="index.html">Back to Home</a>
<label class="switch">
    <input type="checkbox" id="theme-toggle" onchange="toggleTheme(this.checked)">
    <span class="slider round"></span>
</label>
<script src="js/script.js"></script>
</body>
</html>
EOF

    echo "Conversion completed: $output_html"
}

# Convert all Markdown files in the directory
for md_file in "$SCRIPT_DIR"/*.md; do
    [ -e "$md_file" ] || continue
    convert_md_to_html "$md_file"
done

# Generate list of recipe links for index.html
recipe_links=()
for html_file in "$OUTPUT_DIR"/*.html; do
    [ -e "$html_file" ] || continue
    [ "$(basename "$html_file")" != "index.html" ] || continue
    title=$(basename "$html_file" .html)
    recipe_links+=("<li><a href=\"$(basename "$html_file")\">$title</a></li>")
done

# Create index.html with the recipe links
cat << EOF > "$INDEX_HTML"
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>food.nargothrond.xyz</title>
    <link rel="stylesheet" href="https://latex.now.sh/style.min.css" />
    <link rel="stylesheet" href="css/styles.css" />
    <style>
        #recipe-container {
            column-count: 3;
            margin: 20px;
        }
        .search-box {
            text-align: center;
            margin: 20px 0;
        }
        .header-with-icon {
            display: flex;
            align-items: center;
        }
        .header-with-icon img {
            margin-right: 10px;
            vertical-align: middle;
        }
        h1 {
            margin: 0;
            padding: 0;
            line-height: 1;
        }
    </style>
</head>
<body class="light-theme">
    <div class="header-with-icon">
        <img src="pix/food.png" alt="Food Icon" width="50" height="50">
        <h1>food.nargothrond.xyz</h1>
    </div>

    <p>No ads, no bloated JS spyware, just recipes.</p>

    <div class="search-box">
        <input type="text" id="recipe-search" placeholder="Search recipes..." onkeyup="filterRecipes()">
    </div>

    <ul id="recipe-container">
$(printf "%s\n" "${recipe_links[@]}")
    </ul>

    <p>
    <a href="https://nargothrond.xyz">Back to Home</a>
    </p>

    <label class="switch">
        <input type="checkbox" id="theme-toggle" onchange="toggleTheme(this.checked)">
        <span class="slider round"></span>
    </label>

    <script>
        function filterRecipes() {
            var input = document.getElementById('recipe-search');
            var filter = input.value.toUpperCase();
            var ul = document.getElementById("recipe-container");
            var li = ul.getElementsByTagName('li');
            var a, i, txtValue;

            for (i = 0; i < li.length; i++) {
                a = li[i].getElementsByTagName("a")[0];
                txtValue = a.textContent || a.innerText;
                if (txtValue.toUpperCase().indexOf(filter) > -1) {
                    li[i].style.display = "";
                } else {
                    li[i].style.display = "none";
                }
            }
        }
    </script>

    <script src="js/script.js"></script>
</body>
</html>
EOF

echo "Index created with recipe links."

