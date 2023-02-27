const path = require('path');

export const projectRoot = (relativePath) => {
  return path.resolve(__dirname, '..', relativePath);
};

export const globalCSSImports = () => {
  return [
    projectRoot(path.join('src', 'styles', '_variables.scss')),
    projectRoot(path.join('src', 'styles', '_mixins.scss')),
  ];
};


module.exports = {
  projectRoot,
  globalCSSImports
};
