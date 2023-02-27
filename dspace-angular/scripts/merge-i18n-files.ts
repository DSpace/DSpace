import { projectRoot} from '../webpack/helpers';
const commander = require('commander');
const fs = require('fs');
const JSON5 = require('json5');
const _cliProgress = require('cli-progress');
const _ = require('lodash');

const program = new commander.Command();
program.version('1.0.0', '-v, --version');

const LANGUAGE_FILES_LOCATION = 'src/assets/i18n';

parseCliInput();

/**
 * Purpose: Allows customization of i18n labels from within themes
 * e.g. Customize the label "menu.section.browse_global" to display "Browse DSpace" rather than "All of DSpace"
 *
 * This script uses the i18n files found in a source directory to override settings in files with the same
 * name in a destination directory. Only the i18n labels to be overridden need be in the source files.
 *
 * Execution (using custom theme):
 * ```
 *   yarn merge-i18n -s src/themes/custom/assets/i18n
 * ```
 *
 * Input parameters:
 * * Output directory: The directory in which the original i18n files are stored
 *   - Defaults to src/assets/i18n (the default i18n file location)
 *   - This is where the final output files will be written
 * * Source directory: The directory with override files
 *   - Required
 *   - Recommended to place override files in the theme directory under assets/i18n (but this is not required)
 *   - Files must have matching names in both source and destination directories, for example:
 *        en.json5 in the source directory will be merged with en.json5 in the destination directory
 *        fr.json5 in the source directory will be merged with fr.json5 in the destination directory
 */
function parseCliInput() {
  program
    .option('-d, --output-dir <output-dir>', 'output dir when running script on all language files', projectRoot(LANGUAGE_FILES_LOCATION))
    .option('-s, --source-dir <source-dir>', 'source dir of transalations to be merged')
    .usage('(-s <source-dir> [-d <output-dir>])')
    .parse(process.argv);

  if (program.outputDir && program.sourceDir) {
    if (!fs.existsSync(program.outputDir) && !fs.lstatSync(program.outputDir).isDirectory() ) {
      console.error('Output does not exist or is not a directory.');
      console.log(program.outputHelp());
      process.exit(1);
    }
    if (!fs.existsSync(program.sourceDir) && !fs.lstatSync(program.sourceDir).isDirectory() ) {
      console.error('Source does not exist or is not a directory.');
      console.log(program.outputHelp());
      process.exit(1);
    }
    fs.readdirSync(projectRoot(program.sourceDir)).forEach(file => {
      if (fs.existsSync(program.outputDir + '/' + file) ) {
        console.log('Merging: ' + program.outputDir + '/' + file + ' with ' + program.sourceDir + '/' + file);
        mergeFileWithSource(program.sourceDir + '/' + file, program.outputDir + '/' + file);
      }
    });
  } else {
    console.error('Source or Output parameter is missing.');
    console.log(program.outputHelp());
    process.exit(1);
  }
}

/**
 * Reads source file and output file to merge the contents
 *  > Iterates over the source file keys
 *  > Updates values for each key and adds new keys as needed
 *  > Updates the output file with the new merged json
 * @param pathToSourceFile Valid path to source file to merge from
 * @param pathToOutputFile Valid path to merge and write output
 */
function mergeFileWithSource(pathToSourceFile, pathToOutputFile) {
  const progressBar = new _cliProgress.SingleBar({}, _cliProgress.Presets.shades_classic);
  progressBar.start(100, 0);

  const sourceFile = fs.readFileSync(pathToSourceFile, 'utf8');
  progressBar.update(10);
  const outputFile = fs.readFileSync(pathToOutputFile, 'utf8');
  progressBar.update(20);

  const parsedSource = JSON5.parse(sourceFile);
  progressBar.update(30);
  const parsedOutput = JSON5.parse(outputFile);
  progressBar.update(40);

  for (const key of Object.keys(parsedSource)) {
    parsedOutput[key] = parsedSource[key];
  }
  progressBar.update(80);
  fs.writeFileSync(pathToOutputFile,JSON5.stringify(parsedOutput,{ space:'\n  ', quote: '"' }), { encoding:'utf8' });

  progressBar.update(100);
  progressBar.stop();
}
