#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOCKER_DIR="$PROJECT_ROOT/docker"
COMPOSE_FILE="compose.dev.yml"
JAVA_DIR="$PROJECT_ROOT/java"

usage() {
  cat <<EOF
Usage:
  ./server down
  ./server up
  ./server restart
  ./server restart --plugin
EOF
}

run_compose() {
  cd "$DOCKER_DIR"
  docker compose -f "$COMPOSE_FILE" "$@"
}

run_gradle() {
  cd "$JAVA_DIR"
  ./gradlew "$@"
}

require_no_extra_args() {
  if [[ "$#" -ne 0 ]]; then
    echo "Unexpected argument(s): $*" >&2
    usage
    exit 2
  fi
}

command="${1:-}"

case "$command" in
  down)
    shift
    require_no_extra_args "$@"
    run_compose down
    ;;

  up)
    shift
    require_no_extra_args "$@"
    run_compose up
    ;;

  restart)
    shift

    if [[ "${1:-}" == "--plugin" ]]; then
      shift
      require_no_extra_args "$@"

      run_compose down
      run_gradle restartPaperDocker
      run_compose up
    else
      require_no_extra_args "$@"
      run_compose restart
    fi
    ;;

  ""|-h|--help|help)
    usage
    ;;

  *)
    echo "Unknown command: $command" >&2
    usage
    exit 2
    ;;
esac
