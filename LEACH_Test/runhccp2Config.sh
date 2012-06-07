uname -a
time java -cp .:./src:SSJ/lava.jar:./jfreechart/jfreechart-1.0.13.jar:./jfreechart/jcommon-1.0.16.jar:./ssj.jar   -Xms1024M -Xmx8g hccp_test/HCCP_Multi_Goodness_2Configs $1 $2 $3 $4 $5 $6
echo HCCP $1 $2 $3  $4 $5 $6
echo "run $1 $2 $3 $4 $5 $6 done" | mail -s "HCCP A to B run complete" robg
read -n 1 -s
