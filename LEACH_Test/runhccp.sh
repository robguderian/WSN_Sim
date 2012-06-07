uname -a
time java -cp .:./src:SSJ/lava.jar:./jfreechart/jfreechart-1.0.13.jar:./jfreechart/jcommon-1.0.16.jar:./ssj.jar   -Xms1024M -Xmx8g hccp_test/HCCP_Test $1 $2 $3 $4
echo HCCP $1 $2 $3  $4
read -n 1 -s
