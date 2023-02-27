import { commonExports } from './webpack.common';

module.exports = Object.assign({}, commonExports, {
  target: 'web',
});
