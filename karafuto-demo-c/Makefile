TARGET = client

CC = cc

CFLAGS = -Wall -g

INCLUDES = -I libmqtt

LIBS = libmqtt/*.c -lcurl -lpthread -lcrypt -lrt -lm

SOURCES = client.c

all : 
	${CC} ${CFLAGS} ${INCLUDES} ${LIBS} ${SOURCES} -o ${TARGET}
	rm -fr *.gdb

clean :
	rm -fr ${TARGET}
	rm -fr *.gdb

