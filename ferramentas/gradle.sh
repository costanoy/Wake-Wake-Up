#!/usr/bin/env bash
# Runs the Gradle wrapper for the checkout (or git worktree) you are in, from Git Bash on Windows.
#
#   ferramentas/gradle.sh :app:testDebugUnitTest
#   WWU_SCREENSHOTS=1 ferramentas/gradle.sh :app:testDebugUnitTest --tests "com.wakewakeup.ui.ScreenshotTour*"
#
# Why: the project lives under "Área de Trabalho". Compiling works there, but Gradle's test workers
# fail with ClassNotFoundException when the path has non-ASCII characters. So when the path is not
# plain ASCII, this maps the checkout to a free drive letter (subst), runs Gradle from that drive,
# and removes the mapping afterwards. On an ASCII path it just runs the wrapper.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
export ANDROID_HOME="${ANDROID_HOME:-$(cygpath -w "$LOCALAPPDATA")\\Android\\Sdk}"
if [ -z "${JAVA_HOME:-}" ] && [ -d "/c/Program Files/Android/Android Studio/jbr" ]; then
  export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
fi

# Worktrees made from an older commit may lack the wrapper: borrow it from the main checkout.
if [ ! -f "$ROOT/gradlew.bat" ]; then
  MAIN="$(git worktree list --porcelain | sed -n '1s/^worktree //p')"
  cp "$MAIN/gradlew" "$MAIN/gradlew.bat" "$ROOT/" 2>/dev/null || true
  mkdir -p "$ROOT/gradle/wrapper" && cp "$MAIN/gradle/wrapper/gradle-wrapper.jar" "$ROOT/gradle/wrapper/" 2>/dev/null || true
fi

if ! printf '%s' "$ROOT" | LC_ALL=C grep -q '[^ -~]'; then
  cd "$ROOT" && exec ./gradlew.bat "$@" --console=plain
fi

WIN_ROOT="$(cygpath -w "$ROOT")"
DRIVE=""
for L in R S T U V W X Y Z Q P O N M L K J; do
  if MSYS_NO_PATHCONV=1 subst.exe "$L:" "$WIN_ROOT" >/dev/null 2>&1; then DRIVE="$L"; break; fi
done
[ -n "$DRIVE" ] || { echo "No free drive letter for subst" >&2; exit 1; }
trap 'MSYS_NO_PATHCONV=1 subst.exe "$DRIVE:" /D >/dev/null 2>&1 || true' EXIT

cd "/$(echo "$DRIVE" | tr 'A-Z' 'a-z')/"
./gradlew.bat "$@" --console=plain
