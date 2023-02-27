const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');

module.exports = {
  mode: 'production',
  entry: {
    mirador: './src/mirador-viewer/index.js'
  },
  output: {
    path: path.resolve(__dirname, '..' , 'dist/iiif/mirador'),
    filename: '[name].js'
  },
  devServer: {
    contentBase: '../dist/iiif/mirador',
  },
  resolve: {
    fallback: {
      url: false
    }},
  plugins: [new CopyWebpackPlugin({
    patterns: [
      {from: './src/mirador-viewer/mirador.html', to: './index.html'}
    ]
  })]
};
