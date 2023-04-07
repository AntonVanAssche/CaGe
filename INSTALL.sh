#!/usr/bin/env bash

# CaGe install script, version 1.1
# Sebastian Lisken
# requires standard shell with 'expr' command.

set -o errexit  # Abort on nonzero exit code.
set -o nounset  # Abort on unbound variable.
set -o pipefail # Don't hide errors within pipes.

# Whenever an error occurs, print the error message and exit
# with a non-zero exit code.
error_exit() {
    printf '\n%s\n' "${1}"
    exit 1
}

# Wrapper for 'command -v' to avoid spamming '> /dev/null'.
# It also protects against user aliasses and functions.
find_cmd() {
    cmd=$(command -v "${1}") 2> /dev/null
    [[ -x "${cmd}" ]] && printf '%s' "${cmd}"
}

# Since we want to find multiple commands,
# we can call find_cmd() in a loop. This is especially useful
# when we want to check whether all dependencies are installed.
find_cmds() {
    value=0
    for cmd in "${@}"; do
        if ! find_cmd "${cmd}" 2> /dev/null; then
            echo "${cmd}"
            value=1
        fi
    done
    return "${value}"
}

parent() {
    if parent="${1##*/}"; then
        printf '%s' "${parent}"
    elif [[ "${1}" =~ ^/ ]]; then
        printf '/'
    else
        printf '.'
    fi
}

is_java_dir() {
    local java_dir
    java_dir="$(parent "${1}")"
    [[ -x "${java_dir}/bin/${java_cmd}" ]] && \
        [[ ! -d "${java_dir}/bin/${java_cmd}" ]] && \
        [[ -r "${java_dir}/include/jni.h" ]]
}

printf '\033c%s' "
C a G e  --  Chemical & abstract Graph environment
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
"


# We will use 'read -e' to read user input. This provides TAB completion.
# However, not all versions of 'read' support this option. So when we
# can't get read -e to work, we will use 'read' without completion.
with_completion=""
printf . | eval "read -e test" 2>/dev/null && \
    with_completion="-e"


# Since most of the code is written in C, we need a C compiler.
# We will try to find 'gcc' or 'cc' in the path.
# If we can't find one, we will abort the installation.
printf '\n*  Looking for a C compiler ...\n'
if CC="$(find_cmd gcc cc)"; then
    printf '   Ok, using %s.\n' "${CC}"
else
    printf '   None found. Make sure a C compiler (cc or gcc) is in your path.\n'
    error_exit "-  Installation aborted."
fi
printf '\n'
export CC

# We will need 'unzip', 'make', 'mkdir', 'chmod', 'find' and 'java',
# in order to compile and install CaGe. If we can't find one of them,
# we will abort the installation.
printf '\n*  Looking for commands: unzip, make, mkdir, chmod, find ...\n'
if missing="$(find_cmds unzip make mkdir chmod find)"; then
    printf '   All found.\n'
else
    printf '   Command(s) not found in your path: %s\n' "${missing}"
    error_exit "-  Installation aborted."
fi
printf '\n'


### find the "bin" directory of a Java installation ##################

printf '\n*  Looking for a Java installation ...\n'

java_cmd="java"
java_dirs=0
nl="
"

java_dir_list=""

java_dir_list_get() {
    local num="${1}"
    expr "${nl}${java_dir_list}" : ".*${nl}[ ]*${num}:  \([^${nl}]*\)"
}

add_to_java_dir_list() {
    local dir_list="${1}"
    local dir_sep="${2}"
    local get_parents="${3}"

    # Loop through each directory in the list separated by ':'.
    # '${dir_list%%:*}' will extract the first directory in the
    # list until the first ':' character is encountered.
    # The extracted directory is stored in the 'dir' variable.
    while dir="$(expr \"${dir_list}\" : \"^\([^${dir_sep}]*\)\" 2>&-)"; do
        [[ "${get_parents}" == "true" ]] && dir="$(parent "${dir}")"
        [[ -z "${dir}" ]] && dir=.
        if is_java_dir "${dir}"; then
            java_dirs="$(expr "${java_dirs}" + 1)"
            java_dir_list="${java_dir_list}${nl}  ${java_dirs}:  ${dir}"
        fi
        dir_list="$(expr "${dir_list}" : "^[^${dir_sep}]*${dir_sep}\(.*\)$" 2>&-)"
    done
}

# When we can't find the 'java' command, the user will have to
# enter the path to a Java installation manually.
# We will try to find the 'java' command in the path.
# If we can't find it, we will ask the user to enter the path
prepare_java_dir_list_prompt() {
    printf '\n'
    if [[ -z "${java_dir_list}" ]]; then
        printf '   None found. Enter a directory that contains the '\''%s'\'' command.\n' \
            "${java_cmd}"
        prompt="directory: "
    else
        if [[ "${java_dirs}" -eq 1 ]]; then
            directories_seem="directory seems"
            choose_one="Choose it (enter 1)"
        else
            directories_seem="directories seem"
            choose_one="Choose one (enter 1-${java_dirs})"
        fi

        printf '   The following %s to be part of a Java installation:\n' \
            "${directories_seem}"
        printf '   %s or enter the path of another such directory.\n' \
            "${choose_one}"

        prompt="directory [1-${java_dirs}]: "
    fi

    printf '   Enter '\''?'\'' for a full search, or '\''-'\'' to exit.\n'
}

# We will try to find the the javadir automatically on macOS.
# If we can't find it, we will ask the user to enter the path
homedir_mac=""
[[ -f /usr/libexec/java_home ]] && [[ -x /usr/libexec/java_home ]] && \
    homedir_mac="$(/usr/libexec/java_home)/bin:"

add_to_java_dir_list "${PATH}:${homedir_mac}" ":" false

prepare_java_dir_list_prompt

REPLY=""
while [[ -z "${REPLY}" ]]; do
    printf '\n'
    [[ -n "${java_dir_list}" ]] && printf '%s\n' "${java_dir_list}"
    printf '\n%s' "${prompt}"
    read -r ${with_completion?}

    if [[ "${REPLY}" == "-" ]]; then
        error_exit "-  Installation aborted."
    elif [[ "${REPLY}" == "?" ]]; then
        REPLY=""
        cat << EOF
   Enter a list of directories to start searching from (space-separated)."
   An empty response cancels the search. You may use '/' to start a full"
   search. Some good start points are:"

        /usr/lib/jvm        (Linux)"
        /System /Libraries  (Mac OS X)"

        Start point: \c
EOF
        read -r ${with_completion?} points
        points="${points##* }"   # Remove leading whitespace.
        points="${points%%* }"   # Remove trailing whitespace.

        [[ -z "${points}" ]] && continue

        additional=""
        [[ "${java_dirs}" -gt 0 ]] && additional=" additional"
        printf 'Searching for%s Java directories ...\n' "${additional}"
        add_to_javadirlist \
            "$(find "${points}" \( -type f -o -type l \) \
                 -name "${java_cmd}" \
                 -perm -ugo=x \
                 -print 2>/dev/null)" \
            "$nl" \
            true

        prepare_javadirlist_prompt
    elif [[ "${REPLY}" =~ ^[1-9][0-9]*$ ]]; then
        java_dir="$(java_dir_list_get \""${REPLY}"\")"
        java_dir="$(parent "${java_dir}")"
    elif is_java_dir "${REPLY}"; then
        java_dir="${REPLY}"
    else
        REPLY=""
        printf '   Invalid response. Enter a number, a directory, or '\''?'\'' for a full search.\n'
        if [[ "${java_dirs}" -eq 0 ]]; then
            printf '   The directory must contain a %s' "${java_cmd}"
            printf '   command and a "sibling" directory called '\''include'\'' containing '\''jni.h'\''.\n'
        fi
    fi
done

java_dir="$(cd "${java_dir}" && pwd)"
printf '   Ok, using %s.\n' "${java_dir}"

java_dir_file="javadir"
printf 'echo %s\n' "${java_dir}" > "${java_dir_file}"

# We need the current OS name for comiling the code with the
# 'Native' directory. We can use 'uname -s' for this.
# The command will return 'Linux' on Linux systems, 'Darwin' on macOS.
system_name="$(uname -s)"
[[ -z "${system_name}" ]] && \
    error_exit "-  Can't determine system name ('uname -s') - aborted."

# Since 'uname -s' returns 'Darwin' on macOS systems, and our Makefile
# expects 'Mac', we will have to do some renaming here.
[[ "${system_name}" == "Darwin" ]] && system_name="Mac"

include_other_dir=""
other_include="jni_md.h"
if [[ ! -r "${java_dir}/include/${other_include}" ]]; then
    other_found="$(echo "${java_dir}/include"/*/"${other_include}")"
    if [[ "${other_found}" != "${java_dir}/include/*/${other_include}" ]]; then
        include_other_dir="$(dirname "${other_found}")"
    fi
else
    printf '   Warning: can'\''t find include file '\''\${other_include}'\''.'
    printf '   Making of native libraries might fail. Press Return.\n'
fi

printf '\n*  extracting C sources ...\n'
unzip -q CaGe-C.zip || error_exit "-  'unzip' failure, aborting."
printf '\n   Ok.\n'

printf '\n*  Precomputing data in the background ...\n'
(
    cd PreCompute
    make
    make compute &
) || error_exit "-  'make' failure, aborting."
printf '\n   Ok.\n'

printf '\n*  Making generators and embedders ...\n'
(
    cd Generators
    make
) || error_exit "-  'make' failure, aborting."
printf '\n   Ok.\n'

printf '\n*  Making native libraries for %s ...\n' "${system_name}"
(
    cd Native/src
    mkdir -p "../${system_name}"
    CPPFLAGS="$CPPFLAGS -I${java_dir}/include$include_other_dir -w" make "${system_name}"
) || error_exit "-  'make' failure, aborting."
printf '\n   Ok.\n'

chmod u+x cage.sh
cat <<EOF

+  Installation successful - congratulations!
   CaGe is started by the 'cage.sh' command.
   The file 'CaGe.ini' contains some options and comments.

EOF
