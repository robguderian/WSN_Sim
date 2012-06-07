JFLAGS = -classpath .:./src/:SSJ/lava.jar:jfreechart/jfreechart-1.0.13.jar:jfreechart/jcommon-1.0.16.jar:./ssj.jar
JC = javac
.SUFFIXES: .java .class
EXE_FLAGS = -cp .:./src:SSJ/lava.jar:./jfreechart/jfreechart-1.0.13.jar:./jfreechart/jcommon-1.0.16.jar:./ssj.jar:./event-1.6.5.jar
EXE =  leach_test/LEACH_Test
.java.class:
	$(JC) $(JFLAGS) $*.java


CLASSES = src/hccp_test/HCCP_Test.java src/hccp_test/HCCP_Multi.java  src/leach_test/LEACH_Test.java src/leach_test/LEACH_Multi.java src/hccp_test/HCCP_Multi_Goodness.java src/leach_test/LEACH_Multi_OneConfig.java src/hccp_test/HCCP_Multi_One_Config.java src/leach_test/LEACH_Multi_OneConfig_Simple.java src/hccp_test/HCCP_Multi_One_Config_Simple.java src/hccp_test/HCCP_Multi_Goodness_2Configs.java src/hccp_test/HCCP_Multi_Goodness_2Configs_Ch.java 

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) -v src/*/*.class

cleanOutput:
	$(RM) -v output/*

vclean: clean
	$(RM) -v output/*
	$(RM) -v macOutput/*

run: classes
	java $(EXE_FLAGS) $(EXE)

leach: classes
	java $(EXE_FLAGS) leach_test/LEACH_Test

hccp: classes
	java $(EXE_FLAGS) hccp_test/HCCP_Test

up:
	svn up

cleanall: vclean
	~/cleanUp.sh
