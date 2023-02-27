import { hasValue } from '../../shared/empty.util';

/**
 * An interface to represent the path of a section error
 */
export interface SectionErrorPath {

  /**
   * The section id
   */
  sectionId: string;

  /**
   * The form field id
   */
  fieldId?: string;

  /**
   * The form field index
   */
  fieldIndex?: number;

  /**
   * The complete path
   */
  originalPath: string;
}

const regex = /([^\/]+)/g;
// const regex = /\/sections\/(.*)\/(.*)\/(.*)/;
const regexShort = /\/sections\/(.*)/;

/**
 * The following method accept an array of section path strings and return a path object
 * @param {string | string[]} path
 * @returns {SectionErrorPath[]}
 */
const parseSectionErrorPaths = (path: string | string[]): SectionErrorPath[] => {
  const paths = typeof path === 'string' ? [path] : path;

  return paths.map((item) => {
      if (item.match(regex) && item.match(regex).length > 2) {
        return {
          sectionId: item.match(regex)[1],
          fieldId: item.match(regex)[2],
          fieldIndex: hasValue(item.match(regex)[3]) ? +item.match(regex)[3] : 0,
          originalPath: item,
        };
      } else {
        return {
          sectionId: item.match(regexShort)[1],
          originalPath: item,
        };
      }

    }
  );
};

export default parseSectionErrorPaths;
