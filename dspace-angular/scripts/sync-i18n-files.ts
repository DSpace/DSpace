import { projectRoot } from '../webpack/helpers';

const commander = require('commander');
const fs = require('fs');
const JSON5 = require('json5');
const _cliProgress = require('cli-progress');
const _ = require('lodash');

const program = new commander.Command();
program.version('1.0.0', '-v, --version');

const NEW_MESSAGE_TODO = '// TODO New key - Add a translation';
const MESSAGE_CHANGED_TODO = '// TODO Source message changed - Revise the translation';
const COMMENTS_CHANGED_TODO = '// TODO Source comments changed - Revise the translation';

const DEFAULT_SOURCE_FILE_LOCATION = 'src/assets/i18n/en.json5';
const LANGUAGE_FILES_LOCATION = 'src/assets/i18n';

parseCliInput();

/**
 * Parses the CLI input given by the user
 *    If no parameters are set (standard usage) -> source file is default (set to DEFAULT_SOURCE_FILE_LOCATION) and all
 *            other language files in the LANGUAGE_FILES_LOCATION are synced with this one in-place
 *            (replaced with newly synced file)
 *    If only target-file -t is set -> either -i in-place or -o output-file must be set
 *    Source file can be set with -s if it should be something else than DEFAULT_SOURCE_FILE_LOCATION
 *
 *    If any of the paths to files/dirs given by user are not valid, an error message is printed and script gets aborted
 */
function parseCliInput() {
  program
    .option('-d, --output-dir <output-dir>', 'output dir when running script on all language files; mutually exclusive with -o')
    .option('-t, --target-file <target>', 'target file we compare with and where completed output ends up if -o is not configured and -i is')
    .option('-i, --edit-in-place', 'edit-in-place; store output straight in target file; mutually exclusive with -o')
    .option('-s, --source-file <source>', 'source file to be parsed for translation', projectRoot(DEFAULT_SOURCE_FILE_LOCATION))
    .option('-o, --output-file <output>', 'where output of script ends up; mutually exclusive with -i')
    .usage('([-d <output-dir>] [-s <source-file>]) || (-t <target-file> (-i | -o <output>) [-s <source-file>])')
    .parse(process.argv);

  if (!program.targetFile) {
    fs.readdirSync(projectRoot(LANGUAGE_FILES_LOCATION)).forEach(file => {
      if (!program.sourceFile.toString().endsWith(file)) {
        const targetFileLocation = projectRoot(LANGUAGE_FILES_LOCATION + "/" + file);
        console.log('Syncing file at: ' + targetFileLocation + ' with source file at: ' + program.sourceFile);
        if (program.outputDir) {
          if (!fs.existsSync(program.outputDir)) {
            fs.mkdirSync(program.outputDir);
          }
          const outputFileLocation = program.outputDir + "/" + file;
          console.log('Output location: ' + outputFileLocation);
          syncFileWithSource(targetFileLocation, outputFileLocation);
        } else {
          console.log('Replacing in target location');
          syncFileWithSource(targetFileLocation, targetFileLocation);
        }
      }
    });
  } else {
    if (program.targetFile && !checkIfPathToFileIsValid(program.targetFile)) {
      console.error('Directory path of target file is not valid.');
      console.log(program.outputHelp());
      process.exit(1);
    }
    if (program.targetFile && checkIfFileExists(program.targetFile) && !(program.editInPlace || program.outputFile)) {
      console.error('This target file already exists, if you want to overwrite this add option -i, or add an -o output location');
      console.log(program.outputHelp());
      process.exit(1);
    }
    if (!checkIfFileExists(program.sourceFile)) {
      console.error('Path of source file is not valid.');
      console.log(program.outputHelp());
      process.exit(1);
    }
    if (program.outputFile && !checkIfPathToFileIsValid(program.outputFile)) {
      console.error('Directory path of output file is not valid.');
      console.log(program.outputHelp());
      process.exit(1);
    }

    syncFileWithSource(program.targetFile, getOutputFileLocationIfExistsElseTargetFileLocation(program.targetFile));
  }
}

/**
 * Creates chunk lists for both the source and the target files (for example en.json5 and nl.json5 respectively)
 *    > Creates output chunks by comparing the source chunk with corresponding target chunk (based on key of translation)
 *    > Writes the output chunks to a new valid lang.json5 file, either replacing the target file (-i in-place)
 *          or sending it to an output file specified by the user
 * @param pathToTargetFile    Valid path to target file to generate target chunks from
 * @param pathToOutputFile    Valid path to output file to write output chunks to
 */
function syncFileWithSource(pathToTargetFile, pathToOutputFile) {
  const progressBar = new _cliProgress.SingleBar({}, _cliProgress.Presets.shades_classic);
  progressBar.start(100, 0);

  const sourceLines = [];
  const targetLines = [];
  const existingTargetFile = readFileIfExists(pathToTargetFile);
  existingTargetFile.toString().split("\n").forEach((function (line) {
    targetLines.push(line.trim());
  }));
  progressBar.update(10);
  const sourceFile = readFileIfExists(program.sourceFile);
  sourceFile.toString().split("\n").forEach((function (line) {
    sourceLines.push(line.trim());
  }));
  progressBar.update(20);
  const sourceChunks = createChunks(sourceLines, progressBar, false);
  const targetChunks = createChunks(targetLines, progressBar, true);

  const outputChunks = compareChunksAndCreateOutput(sourceChunks, targetChunks, progressBar);

  const file = fs.createWriteStream(pathToOutputFile);
  file.on('error', function (err) {
    console.error('Something went wrong writing to output file at: ' + pathToOutputFile + err)
  });
  file.on('open', function() {
    file.write("{\n");
    outputChunks.forEach(function (chunk) {
      progressBar.increment();
      chunk.split("\n").forEach(function (line) {
        file.write((line === '' ? '' : `  ${line}`) + "\n");
      });
    });
    file.write("\n}");
    file.end();
  });
  file.on('finish', function() {
    const osName = process.platform;
    if (osName.startsWith("win")) {
      replaceLineEndingsToCRLF(pathToOutputFile);
    }
  });

  progressBar.update(100);
  progressBar.stop();
}

/**
 * For each of the source chunks:
 *      - Determine if it's a new key-value => Add it to output, with source comments, source key-value commented, a message indicating it's new and the source-key value uncommented
 *      - If it's not new, compare it with the corresponding target chunk and log the differences, see createNewChunkComparingSourceAndTarget
 * @param sourceChunks      All the source chunks, split per key-value pair group
 * @param targetChunks      All the target chunks, split per key-value pair group
 * @param progressBar       The progressbar for the CLI
 * @return {Array}          All the output chunks, split per key-value pair group
 */
function compareChunksAndCreateOutput(sourceChunks, targetChunks, progressBar) {
  const outputChunks = [];
  sourceChunks.map((sourceChunk) => {
    progressBar.increment();
    if (sourceChunk.trim().length !== 0) {
      let newChunk = [];
      const sourceList = sourceChunk.split("\n");
      const keyValueSource = sourceList[sourceList.length - 1];
      const keySource = getSubStringBeforeLastString(keyValueSource, ":");
      const commentSource = getSubStringBeforeLastString(sourceChunk, keyValueSource);

      const correspondingTargetChunk = targetChunks.find((targetChunk) => {
        return targetChunk.includes(keySource);
      });

      // Create new chunk with: the source comments, the commented source key-value, the todos and either the old target key-value pair or if it's a new pair, the source key-value pair
      newChunk.push(removeWhiteLines(commentSource));
      newChunk.push("// " + keyValueSource);
      if (correspondingTargetChunk === undefined) {
        newChunk.push(NEW_MESSAGE_TODO);
        newChunk.push(keyValueSource);
      } else {
        createNewChunkComparingSourceAndTarget(correspondingTargetChunk, sourceChunk, commentSource, keyValueSource, newChunk);
      }

      outputChunks.push(newChunk.filter(Boolean).join("\n"));
    } else {
      outputChunks.push(sourceChunk);
    }
  });
  return outputChunks;
}

/**
 * If a corresponding target chunk is found:
 *      - If old key value is not found in comments > Assumed it is new key
 *      - If the target comments do not contain the source comments (because they have changed since last time) => Add comments changed message
 *      - If the key-value in the target comments is not the same as the source key-value (because it changes since last time) => Add message changed message
 *      - Add the old todos if they haven't been added already
 *      - End with the original target key-value
 */
function createNewChunkComparingSourceAndTarget(correspondingTargetChunk, sourceChunk, commentSource, keyValueSource, newChunk) {
  let commentsOfSourceHaveChanged = false;
  let messageOfSourceHasChanged = false;

  const targetList = correspondingTargetChunk.split("\n");
  const oldKeyValueInTargetComments = getSubStringWithRegex(correspondingTargetChunk, "\\s*\\/\\/\\s*\".*");
  let keyValueTarget = targetList[targetList.length - 1];
  if (!keyValueTarget.endsWith(",")) {
    keyValueTarget = keyValueTarget + ",";
  }

  if (oldKeyValueInTargetComments != null) {
    const oldKeyValueUncommented = getSubStringWithRegex(oldKeyValueInTargetComments[0], "\".*")[0];

    if (!(_.isEmpty(correspondingTargetChunk) && _.isEmpty(commentSource)) && !removeWhiteLines(correspondingTargetChunk).includes(removeWhiteLines(commentSource.trim()))) {
      commentsOfSourceHaveChanged = true;
      newChunk.push(COMMENTS_CHANGED_TODO);
    }
    const parsedOldKey = JSON5.stringify("{" + oldKeyValueUncommented + "}");
    const parsedSourceKey = JSON5.stringify("{" + keyValueSource + "}");
    if (!_.isEqual(parsedOldKey, parsedSourceKey)) {
      messageOfSourceHasChanged = true;
      newChunk.push(MESSAGE_CHANGED_TODO);
    }
    addOldTodosIfNeeded(targetList, newChunk, commentsOfSourceHaveChanged, messageOfSourceHasChanged);
  }
  newChunk.push(keyValueTarget);
}

// Adds old todos found in target comments if they've not been added already
function addOldTodosIfNeeded(targetList, newChunk, commentsOfSourceHaveChanged, messageOfSourceHasChanged) {
  targetList.map((targetLine) => {
    const foundTODO = getSubStringWithRegex(targetLine, "\\s*//\\s*TODO.*");
    if (foundTODO != null) {
      const todo = foundTODO[0];
      if (!((todo.includes(COMMENTS_CHANGED_TODO) && commentsOfSourceHaveChanged)
        || (todo.includes(MESSAGE_CHANGED_TODO) && messageOfSourceHasChanged))) {
        newChunk.push(todo);
      }
    }
  });
}

/**
 * Creates chunks from an array of lines, each chunk contains either an empty line or a grouping of comments with their corresponding key-value pair
 * @param lines             Array of lines, to be grouped into chunks
 * @param progressBar       Progressbar of the CLI
 * @return {Array}          Array of chunks, grouped by key-value and their corresponding comments or an empty line
 */
function createChunks(lines, progressBar, creatingTarget) {
  const chunks = [];
  let nextChunk = [];
  let onMultiLineComment = false;
  lines.map((line) => {
    progressBar.increment();
    if (line.length === 0) {
      chunks.push(line);
    }
    if (isOneLineCommentLine(line)) {
      nextChunk.push(line);
    }
    if (onMultiLineComment) {
      nextChunk.push(line);
      if (isEndOfMultiLineComment(line)) {
        onMultiLineComment = false;
      }
    }
    if (isStartOfMultiLineComment(line)) {
      nextChunk.push(line);
      onMultiLineComment = true;
    }
    if (isKeyValuePair(line)) {
      nextChunk.push(line);
      const newMessageLineIfExists = nextChunk.find((lineInChunk) => lineInChunk.trim().startsWith(NEW_MESSAGE_TODO));
      if (newMessageLineIfExists === undefined || !creatingTarget) {
        chunks.push(nextChunk.join("\n"));
      }
      nextChunk = [];
    }
  });
  return chunks;
}

function readFileIfExists(pathToFile) {
  if (checkIfFileExists(pathToFile)) {
    try {
      return fs.readFileSync(pathToFile, 'utf8');
    } catch (e) {
      console.error('Error:', e.stack);
    }
  }
  return null;
}

function isOneLineCommentLine(line) {
  return (line.startsWith("//"));
}

function isStartOfMultiLineComment(line) {
  return (line.startsWith("/*"));
}

function isEndOfMultiLineComment(line) {
  return (line.endsWith("*/"));
}

function isKeyValuePair(line) {
  return (line.startsWith("\""));
}


function getSubStringWithRegex(string, regex) {
  return string.match(regex);
}

function getSubStringBeforeLastString(string, char) {
  const lastCharIndex = string.lastIndexOf(char);
  return string.substr(0, lastCharIndex);
}


function getOutputFileLocationIfExistsElseTargetFileLocation(targetLocation) {
  if (program.outputFile) {
    return program.outputFile;
  }
  return targetLocation;
}

function checkIfPathToFileIsValid(pathToCheck) {
  if (!pathToCheck.includes("/")) {
    return true;
  }
  return checkIfFileExists(getPathOfDirectory(pathToCheck));
}

function checkIfFileExists(pathToCheck) {
  return fs.existsSync(pathToCheck);
}

function getPathOfDirectory(pathToCheck) {
  return getSubStringBeforeLastString(pathToCheck, "/");
}

function removeWhiteLines(string) {
  return string.replace(/^(?=\n)$|^\s*|\s*$|\n\n+/gm, "")
}

/**
 * Replaces UNIX \n LF line endings to windows \r\n CRLF line endings.
 * @param filePath  Path to file whose line endings are being converted
 */
function replaceLineEndingsToCRLF(filePath) {
  const data = readFileIfExists(filePath);
  const result = data.replace(/\n/g,"\r\n");
  fs.writeFileSync(filePath, result, 'utf8');
}
