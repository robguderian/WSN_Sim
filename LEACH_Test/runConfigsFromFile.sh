#!/usr/local/bin/bash


# usage: ./thisScript defaultsFile folder otherdefaultsFiles


#machines=(rookery  eagle  sparrow  robin  finch  duck  grouse  killdeer  heron  grebe  cormorant  crow  sandpiper  woodpecker  wren  loon  nuthatch  oriole  osprey  owl  pelican  grebe  falcon  heron  kingfisher hawk)
machines=(agouti beaver capybara chinchilla chipmunk degu gerbil gopher     groundhog guineapig hamster hedgehog lemming marmot mouse muskrat     nutria porcupine prairiedog rat squirrel vole woodchuck zokor)
index=0
count=0
#echo ${#machines[@]}
#echo ${machines[0]}
for file in `ls $2 | grep -iv base`
do
echo screen  ssh ${machines[$index]} \"cd projects/LEACH_Test \; ./runhccp.sh $1  $2/$file $3\"
#index=$index+1
let "index += 1"
if [[ $index == ${#machines[@]} ]]
then
index=0
fi
let "count += 1"
if [[ $count == 30 ]]
then
count=0
echo screen bash
fi

done

echo screen bash
