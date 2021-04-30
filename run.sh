#!/bin/bash
# run as: run.sh {name of main class} <arguments>
#
# e.g.
#
# ./run.sh basicp2psim/simapp -g
# ./run.sh topoapp -g

# If dependencies like MultiGraph are sibling directories
#PROJECTHOME=$(realpath ..)
PROJECTHOME="$(realpath $(dirname "$0")/..)"
VERSION=0.2

CPATH="$PROJECTHOME/MultiGraph/build/jar/MultiGraph.jar"
#CPATH="$CPATH:$PROJECTHOME/agarnet/build/jar/agarnet.jar"

CPATH="$CPATH:$PROJECTHOME/agarnet/target/agarnet-${VERSION}.jar"
CPATH="$CPATH:$PROJECTHOME/agarnet/target/agarnet-${VERSION}-linux64.jar"

CPATH="$CPATH:/usr/lib/java/hawtjni/hawtjni-runtime.jar"
CPATH="$CPATH:/usr/share/java/gnu.getopt.jar"
#CPATH="$CPATH:$PROJECTHOME/kcoresim/build/jar/kcoresim.jar"
#PROFOPTS="(-Xprof)"
#PROFOPTS="(-Xrunhprof)"
GCOPTS=(-XX:+UseConcMarkSweepGC)
JAVAOPTS=("${JAVAOPTS[@]}" -XX:+UseLargePages)
JAVAOPTS=("${JAVAOPTS[@]}" -Xverify:none)
#  -Dsun.java2d.opengl=True needed for Java2D to not be super-slow,
# with anti-aliasing on Linux.
JAVAOPTS=("${JAVAOPTS[@]}" '-DuseAA=true' '-Dsun.java2d.opengl=True')
java "${JAVAOPTS[@]}" "${PROFOPTS[@]}" "${GCOPTS[@]}" \
        -server -classpath "$CPATH" "$@"
