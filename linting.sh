#! /usr/bin/env bash
if [[ -z "$CI" ]];then
  ktlint 'apps/*/src/main/**/*.kt' 'apps/*/src/test/**/*.kt' 'common/*/src/main/**/*.kt' --reporter=plain?group_by_file --color --experimental $@
else
  ktlint 'apps/*/src/main/**/*.kt' 'apps/*/src/test/**/*.kt' 'apps/*/src/main/**/*.kt' --reporter=plain?group_by_file --experimental
fi
