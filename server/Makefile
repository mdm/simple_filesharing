INCLUDEDIRS=-I.
LIBS=
LIBRARYDIRS=-L/usr/X11R6/lib -L.


sf_server: sf_server.o
	gcc -ggdb -o sf_server $(LIBRARYDIRS) $(LIBS) sf_server.o

%.o: %.cpp
	gcc -ggdb -c $(INCLUDEDIRS) -o $@ $< 
