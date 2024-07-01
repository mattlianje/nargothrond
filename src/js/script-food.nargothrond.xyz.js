function filterRecipes() {
    let input = document.getElementById('searchBar');
    let filter = input.value.toUpperCase();
    let container = document.querySelector('.recipe-container');
    let recipes = container.querySelectorAll('.recipe');

    for (let i = 0; i < recipes.length; i++) {
        let recipe = recipes[i];
        let name = recipe.getAttribute('data-name');
        if (name.toUpperCase().indexOf(filter) > -1) {
            recipe.style.display = "block";
        } else {
            recipe.style.display = "none";
        }
    }
}

