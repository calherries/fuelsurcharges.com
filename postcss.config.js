module.exports = {
    plugins: [
        require('tailwindcss'),
        require('autoprefixer'),
        require('@fullhuman/postcss-purgecss')({
            // Specify the paths to all of the template files in your project
            content: [
                './resources/html/home.html',
                './src/fuelsurcharges/core.cljs'
            ],
            // This will extract all css selectors and tags used in your cljs files
            defaultExtractor: content => Array.from(content.matchAll(/:?([A-Za-z0-9-_:]+)/g)).map(x => x[1]) || []
        }),
    ]
}
