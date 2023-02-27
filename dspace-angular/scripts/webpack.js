const path = require('path');
const child_process = require('child_process');

const heapSize = 4096;
const webpackPath = path.join('node_modules', 'webpack', 'bin', 'webpack.js');

const params = [
  '--max_old_space_size=' + heapSize,
  webpackPath,
  ...process.argv.slice(2)
];

child_process.spawn('node', params, { stdio:'inherit' });
