PROJECTHOME="/home/paul/code/"

CPATH="$PROJECTHOME/MultiGraph/build/classes/"
CPATH="$CPATH:$PROJECTHOME/agarnet/build/classes/"
CPATH="$CPATH:/usr/share/java/gnu.getopt.jar"
CPATH="$CPATH:$PROJECTHOME/kcoresim/bin/"

java -cp $CPATH kcoresim/kcoresim $@
