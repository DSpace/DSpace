module.exports = {
  plugins: [
    require('postcss-import')(),
    require('postcss-preset-env')(),
    require('postcss-apply')(),
    require('postcss-responsive-type')()
  ]
};
