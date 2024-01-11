/* Toggles from latex-css dark to light w/ slider */

function applyTheme() {
    const theme = localStorage.getItem('theme');
    if (theme === 'latex-dark') {
        document.body.classList.add('latex-dark');
    } else {
        document.body.classList.remove('latex-dark');
    }
}

// Apply the theme as soon as possible
applyTheme();

function toggleTheme(isDark) {
    localStorage.setItem('theme', isDark ? 'latex-dark' : 'light-theme');
    applyTheme();
}
