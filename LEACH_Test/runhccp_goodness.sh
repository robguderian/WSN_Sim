uname -a
time java -cp .:./src:SSJ/lava.jar:./jfreechart/jfreechart-1.0.13.jar:./jfreechart/jcommon-1.0.16.jar:./ssj.jar   -Xms1024M -Xmx8g hccp_test/HCCP_Multi_Goodness $1 $2 $3 $4 $5 $6 $7
echo "run $1 $2 $3 $4 $5 $6 $7 done" | mail -s "HCCP Goodness run complete" robg
echo "HCCP Goodness $1 $2 $3 $4 $5 $6 $7 done"
read -n 1 -s
