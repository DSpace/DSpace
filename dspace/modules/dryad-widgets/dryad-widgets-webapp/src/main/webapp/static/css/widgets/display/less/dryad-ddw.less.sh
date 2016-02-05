#!/usr/bin/env bash
#

LESS_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

DRYAD_DDW="$LESS_DIR/../dryad-ddw.css"
DRYAD_DDW_MIN="$LESS_DIR/../dryad-ddw.min.css"
DRYAD_DDW_LESS="$LESS_DIR/dryad-ddw.less"

lessc "$DRYAD_DDW_LESS" "$DRYAD_DDW" 
lessc --compress "$DRYAD_DDW_LESS" "$DRYAD_DDW_MIN" 
