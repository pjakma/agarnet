PROJECTHOME="/home/paul/code"

CPATH="$PROJECTHOME/MultiGraph/build/classes"
CPATH="$CPATH:$PROJECTHOME/agarnet/build/jar/agarnet.jar"
CPATH="$CPATH:/usr/share/java/gnu.getopt.jar"
CPATH="$CPATH:$PROJECTHOME/kcoresim/build/jar/kcoresim.jar"
#PROFOPTS="-Xprof"
#PROFOPTS="-Xrunhprof"
#PROFOPTS="-javaagent:/home/paul/Downloads/shiftone-jrat.jar"
GCOPTS="-XX:+UseConcMarkSweepGC"

java -Xverify:none ${PROFOPTS} ${GCOPTS} \
	-server -classpath $CPATH kcoresim.kcoresim $@
