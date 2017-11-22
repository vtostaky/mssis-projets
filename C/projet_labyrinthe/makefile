CC=gcc
CFLAGS=-I.
DEPS= labyrinth.h
OBJ= labyrinth.o main.o

%.o: %.c $(DEPS)
	$(CC) -c -o $@ $< $(CFLAGS)
laby_exe: $(OBJ)
	$(CC) -o $@ $^ $(CFLAGS)

clean:
	rm -f *.o
