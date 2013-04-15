PROJECTHOME="/home/paul/code"

CPATH="$PROJECTHOME/MultiGraph/build/classes"
CPATH="$CPATH:$PROJECTHOME/agarnet/build/jar/agarnet.jar"
CPATH="$CPATH:/usr/share/java/gnu.getopt.jar"
CPATH="$CPATH:$PROJECTHOME/kcoresim/build/jar/kcoresim.jar"

java -server -classpath $CPATH kcoresim.kcoresim $@
