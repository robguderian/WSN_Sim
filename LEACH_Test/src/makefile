JFLAGS = -classpath .:../SSJ/lava.jar:../jfreechart/jfreechart-1.0.13.jar:../jfreechart/jcommon-1.0.16.jar
JC = javac
.SUFFIXES: .java .class
EXE_FLAGS = -cp .:../SSJ/lava.jar:../jfreechart/jfreechart-1.0.13.jar:../jfreechart/jcommon-1.0.16.jar
EXE =  leach_test/LEACH_Test
.java.class:
	$(JC) $(JFLAGS) $*.java


CLASSES = hccp_test/HCCP_Test.java leach_test/LEACH_Test.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) -v */*.class

run: classes
	java $(EXE_FLAGS) $(EXE)

leach: classes
	java $(EXE_FLAGS) leach_test/LEACH_Test

hccp: classes
	java $(EXE_FLAGS) hccp_test/HCCP_Test

