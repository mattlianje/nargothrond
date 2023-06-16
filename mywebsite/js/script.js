window.addEventListener('DOMContentLoaded', (event) => {
    const toggle = document.getElementById('theme-toggle');

    // Load the theme from local storage
    const theme = localStorage.getItem('theme');
    if (theme) {
        document.body.className = theme;
        toggle.checked = (theme === 'dark-theme');
    }

    toggle.addEventListener('change', function() {
        const theme = this.checked ? 'dark-theme' : 'light-theme';

        // Save the theme to local storage
        localStorage.setItem('theme', theme);

        document.body.className = theme;
    });
});
