uname -a
time java -cp .:./src:SSJ/lava.jar:./jfreechart/jfreechart-1.0.13.jar:./jfreechart/jcommon-1.0.16.jar:./ssj.jar -Xms1024M -Xmx8192M leach_test/LEACH_Test $1 $2 $3 $4
echo "leach run $1 $2 $3 $4 $5 done"

read -n 1 -s
