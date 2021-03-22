// Theme switching support
var localStorageThemeKey = 'cds_theme';

function loadTheme() {
    var theme = 'auto';
    var actualTheme;
    if (window.localStorage.getItem(localStorageThemeKey)) {
        theme = window.localStorage.getItem(localStorageThemeKey);
    }

    if (theme === 'auto') {
        actualTheme = (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
    } else {
        actualTheme = theme;
    }

    if (actualTheme === 'dark') {
        $('body').addClass('dark-mode');
        $('nav.main-header').addClass('navbar-dark').removeClass('navbar-white navbar-light');
    } else {
        $('body').removeClass('dark-mode');
        $('nav.main-header').removeClass('navbar-dark').addClass('navbar-white navbar-light');
    }

    $('[data-theme]').find('i.fas').removeClass('fa-check');
    $('[data-theme=' + theme + '] i.fas').addClass('fa-check');
}

$(function() {
    $('[data-theme]').click(function() {
        window.localStorage.setItem(localStorageThemeKey, $(this).data('theme'));
        loadTheme();
    });

    if (!window.localStorage) {
        $('.theme-menu').hide();
    } else {
        // Load the currently configured theme
        loadTheme();
    }
});

// If the browser supports switching themes, update the theme based on the browser setting
if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function() {
        loadTheme();
    });
}