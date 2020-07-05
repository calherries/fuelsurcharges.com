module.exports = {
    plugins: [
        require('tailwindcss'),
        require('autoprefixer'),
        require('@fullhuman/postcss-purgecss')({
            content: [
                './resources/html/home.html',
                './src/fuelsurcharges/core.cljs'
            ],
            defaultExtractor: content => Array.from(content.matchAll(/:?([A-Za-z0-9-_:]+)/g)).map(x => x[1]) || []
        }),
        require('cssnano')
    ]
}
