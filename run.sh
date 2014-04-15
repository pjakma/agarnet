PROJECTHOME="/home/paul/code"

CPATH="$PROJECTHOME/MultiGraph/build/jar/MultiGraph.jar"
CPATH="$CPATH:$PROJECTHOME/agarnet/build/jar/agarnet.jar"
CPATH="$CPATH:/usr/share/java/gnu.getopt.jar"
CPATH="$CPATH:$PROJECTHOME/kcoresim/build/jar/kcoresim.jar"
#PROFOPTS="-Xprof"
#PROFOPTS="-Xrunhprof"
#PROFOPTS="-javaagent:/home/paul/Downloads/shiftone-jrat.jar"
GCOPTS="-XX:+UseConcMarkSweepGC"
JAVAOPTS="${JAVAOPTS} -XX:+UseLargePages "
JAVAOPTS="${JAVAOPTS} -Xverify:none"

java ${JAVAOPTS} ${PROFOPTS} ${GCOPTS} \
	-server -classpath $CPATH kcoresim.kcoresim $@
