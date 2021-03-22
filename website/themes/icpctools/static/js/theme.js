function loadTheme() {
    var theme = (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
    if (theme === 'dark') {
        $('body').addClass('dark-mode');
        $('nav.main-header').addClass('navbar-dark').removeClass('navbar-white navbar-light');
    } else {
        $('body').removeClass('dark-mode');
        $('nav.main-header').removeClass('navbar-dark').addClass('navbar-white navbar-light');
    }
}

$(function() {
    loadTheme();
});

// If the browser supports switching themes, update the theme based on the browser setting
if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function() {
        loadTheme();
    });
}
