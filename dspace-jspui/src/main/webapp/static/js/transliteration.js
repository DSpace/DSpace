'use strict';

function cyrillicToTranslit(language) {
    const _firstLetterAssociations = {
        "а": "a",
        "б": "b",
        "в": "v",
        "ґ": "g",
        "г": "g",
        "д": "d",
        "е": "e",
        "ё": "e",
        "є": "ye",
        "ж": "zh",
        "з": "z",
        "и": "i",
        "і": "i",
        "ї": "yi",
        "й": "i",
        "к": "k",
        "л": "l",
        "м": "m",
        "н": "n",
        "о": "o",
        "п": "p",
        "р": "r",
        "с": "s",
        "т": "t",
        "у": "u",
        "ф": "f",
        "х": "h",
        "ц": "c",
        "ч": "ch",
        "ш": "sh",
        "щ": "sh'",
        "ъ": "",
        "ы": "i",
        "ь": "",
        "э": "e",
        "ю": "yu",
        "я": "ya",
    };

    if (language === "uk") {
        Object.assign(_firstLetterAssociations, {
            "г": "h",
            "и": "y",
            "й": "y",
            "х": "kh",
            "ц": "ts",
            "щ": "shch",
            "'": "",
            "’": "",
            "ʼ": "",
        });
    }

    const _associations = Object.assign({}, _firstLetterAssociations);

    if (language === "uk") {
        Object.assign(_associations, {
            "є": "ie",
            "ї": "i",
            "й": "i",
            "ю": "iu",
            "я": "ia",
        });
    }

    function transform(input, spaceReplacement) {
        if (!input) {
            return "";
        }

        var newStr = "";
        for (var i = 0; i < input.length; i++) {
            const isUpperCaseOrWhatever = input[i] === input[i].toUpperCase();
            var strLowerCase = input[i].toLowerCase();
            if (strLowerCase === " " && spaceReplacement) {
                newStr += spaceReplacement;
                continue;
            }
            var newLetter = language === "uk" && strLowerCase === "г" && i > 0 && input[i - 1].toLowerCase() === "з"
                ? "gh"
                : (i === 0 ? _firstLetterAssociations : _associations)[strLowerCase];
            if ("undefined" === typeof newLetter) {
                newStr += isUpperCaseOrWhatever ? strLowerCase.charAt(0).toUpperCase() + newLetter.slice(1) : strLowerCase;
            }
            else {
                newStr += isUpperCaseOrWhatever ? newLetter.charAt(0).toUpperCase() + newLetter.slice(1): newLetter;
            }
        }
        return newStr;
    }

    return {
        transform: transform
    }
}