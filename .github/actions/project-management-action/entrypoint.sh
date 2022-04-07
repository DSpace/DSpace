#!/bin/sh -l

PROJECT_URL="$INPUT_PROJECT"
if [ -z "$PROJECT_URL" ]; then
  echo "Project input variable is not defined." >&2
  exit 1
fi

get_project_type() {
  _PROJECT_URL="$1"

  case "$_PROJECT_URL" in
    https://github.com/orgs/*)
      echo "org"
      ;;
    https://github.com/users/*)
      echo "user"
      ;;
    https://github.com/*/projects/*)
      echo "repo"
      ;;
    *)
      echo "Invalid Project URL: '$_PROJECT_URL' . Please pass a valid Project URL in the project input variable" >&2
      exit 1
      ;;
  esac

  unset _PROJECT_URL
}

get_next_url_from_headers() {
  _HEADERS_FILE=$1
  grep -i '^link' "$_HEADERS_FILE" | tr ',' '\n'| grep \"next\" | sed 's/.*<\(.*\)>.*/\1/'
}

find_project_id() {
  _PROJECT_TYPE="$1"
  _PROJECT_URL="$2"

  case "$_PROJECT_TYPE" in
    org)
      _ORG_NAME=$(echo "$_PROJECT_URL" | sed -e 's@https://github.com/orgs/\([^/]\+\)/projects/[0-9]\+@\1@')
      _ENDPOINT="https://api.github.com/orgs/$_ORG_NAME/projects?per_page=100"
      ;;
    user)
      _USER_NAME=$(echo "$_PROJECT_URL" | sed -e 's@https://github.com/users/\([^/]\+\)/projects/[0-9]\+@\1@')
      _ENDPOINT="https://api.github.com/users/$_USER_NAME/projects?per_page=100"
      ;;
    repo)
      _ENDPOINT="https://api.github.com/repos/$GITHUB_REPOSITORY/projects?per_page=100"
      ;;
  esac
  
  _NEXT_URL="$_ENDPOINT"

  while : ; do

    _PROJECTS=$(curl -s -X GET -u "$GITHUB_ACTOR:$TOKEN" --retry 3 \
            -H 'Accept: application/vnd.github.inertia-preview+json' \
            -D /tmp/headers \
            "$_NEXT_URL")

    _PROJECTID=$(echo "$_PROJECTS" | jq -r ".[] | select(.html_url == \"$_PROJECT_URL\").id")
    _NEXT_URL=$(get_next_url_from_headers '/tmp/headers')

    if [ "$_PROJECTID" != "" ]; then
      echo "$_PROJECTID"
    elif [ "$_NEXT_URL" == "" ]; then
      echo "No project was found." >&2
      exit 1
    fi
  done

  unset _PROJECT_TYPE _PROJECT_URL _ORG_NAME _USER_NAME _ENDPOINT _PROJECTS _PROJECTID _NEXT_URL
}

find_column_id() {
  _PROJECT_ID="$1"
  _INITIAL_COLUMN_NAME="$2"

  _COLUMNS=$(curl -s -X GET -u "$GITHUB_ACTOR:$TOKEN" --retry 3 \
          -H 'Accept: application/vnd.github.inertia-preview+json' \
          "https://api.github.com/projects/$_PROJECT_ID/columns")


  echo "$_COLUMNS" | jq -r ".[] | select(.name == \"$_INITIAL_COLUMN_NAME\").id"
  unset _PROJECT_ID _INITIAL_COLUMN_NAME _COLUMNS
}

PROJECT_TYPE=$(get_project_type "${PROJECT_URL:?<Error> required this environment variable}")

if [ "$PROJECT_TYPE" = org ] || [ "$PROJECT_TYPE" = user ]; then
  if [ -z "$MY_GITHUB_TOKEN" ]; then
    echo "MY_GITHUB_TOKEN not defined" >&2
    exit 1
  fi

  TOKEN="$MY_GITHUB_TOKEN" # It's User's personal access token. It should be secret.
else
  if [ -z "$GITHUB_TOKEN" ]; then
    echo "GITHUB_TOKEN not defined" >&2
    exit 1
  fi

  TOKEN="$GITHUB_TOKEN"    # GitHub sets. The scope in only the repository containing the workflow file.
fi

INITIAL_COLUMN_NAME="$INPUT_COLUMN_NAME"
if [ -z "$INITIAL_COLUMN_NAME" ]; then
  # assing the column name by default
  INITIAL_COLUMN_NAME='To do'
  if [ "$GITHUB_EVENT_NAME" == "pull_request" ] || [ "$GITHUB_EVENT_NAME" == "pull_request_target" ]; then
    echo "changing column name for PR event"
    INITIAL_COLUMN_NAME='In progress'
  fi
fi


PROJECT_ID=$(find_project_id "$PROJECT_TYPE" "$PROJECT_URL")
INITIAL_COLUMN_ID=$(find_column_id "$PROJECT_ID" "${INITIAL_COLUMN_NAME:?<Error> required this environment variable}")

if [ -z "$INITIAL_COLUMN_ID" ]; then
  echo "Column name '$INITIAL_COLUMN_ID' is not found." >&2
  exit 1
fi

case "$GITHUB_EVENT_NAME" in
  issues|issue_comment)
    ISSUE_ID=$(jq -r '.issue.id' < "$GITHUB_EVENT_PATH")

    # Add this issue to the project column
    curl -s -X POST -u "$GITHUB_ACTOR:$TOKEN" --retry 3 \
     -H 'Accept: application/vnd.github.inertia-preview+json' \
     -d "{\"content_type\": \"Issue\", \"content_id\": $ISSUE_ID}" \
     "https://api.github.com/projects/columns/$INITIAL_COLUMN_ID/cards"
    ;;
  pull_request|pull_request_target)
    PULL_REQUEST_ID=$(jq -r '.pull_request.id' < "$GITHUB_EVENT_PATH")

    # Add this pull_request to the project column
    curl -s -X POST -u "$GITHUB_ACTOR:$TOKEN" --retry 3 \
     -H 'Accept: application/vnd.github.inertia-preview+json' \
     -d "{\"content_type\": \"PullRequest\", \"content_id\": $PULL_REQUEST_ID}" \
     "https://api.github.com/projects/columns/$INITIAL_COLUMN_ID/cards"
    ;;
  *)
    echo "Nothing to be done on this action: '$GITHUB_EVENT_NAME'" >&2
    exit 1
    ;;
esac
