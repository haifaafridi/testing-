#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mode="${1:-run}"
mkdir -p out
javac --release 17 -encoding UTF-8 -d out -sourcepath src src/taskflow/TaskFlow.java
case "$mode" in
    run) exec java -cp out taskflow.TaskFlow ;;
    test|ui-test)
        javac --release 17 -encoding UTF-8 -cp out -d out test/taskflow/TaskChecks.java test/taskflow/DesktopChecks.java
        if [ "$mode" = test ]; then exec java -cp out taskflow.TaskChecks; fi
        shift
        exec java -cp out taskflow.DesktopChecks "$@"
        ;;
    docs) exec javadoc -quiet -Werror -Xdoclint:all -notimestamp --no-platform-links -author -package -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -d docs/api -sourcepath src taskflow ;;
    *) echo "Usage: ./taskflow.sh [run|test|ui-test [image-directory]|docs]" >&2; exit 2 ;;
esac
