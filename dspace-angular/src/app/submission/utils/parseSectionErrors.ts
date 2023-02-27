import { SubmissionObjectError } from '../../core/submission/models/submission-object.model';
import { default as parseSectionErrorPaths, SectionErrorPath } from './parseSectionErrorPaths';

/**
 * the following method accept an array of SubmissionObjectError and return a section errors object
 * @param {errors: SubmissionObjectError[]} errors
 * @returns {any}
 */
const parseSectionErrors = (errors: SubmissionObjectError[] = []): any => {
  const errorsList = Object.create({});

  errors.forEach((error: SubmissionObjectError) => {
    const paths: SectionErrorPath[] = parseSectionErrorPaths(error.paths);

    paths.forEach((path: SectionErrorPath) => {
      const sectionError = {path: path.originalPath, message: error.message};
      if (!errorsList[path.sectionId]) {
        errorsList[path.sectionId] = [];
      }
      errorsList[path.sectionId].push(sectionError);
    });
  });

  return errorsList;
};

export default parseSectionErrors;
