#!/usr/local/bin/python2
import socket
import sys
import select


if __name__ == "__main__":
    if (len(sys.argv) != 2 or not sys.argv[1].isdigit()):
        print 'Usage: mirror_port_listener.py <mirror_port>',
        exit()

    port = int(sys.argv[1])

    mirror_server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        mirror_server.bind(('', port))
    except OverflowError:
        print 'Port must be 0-65535',
        exit()
    mirror_server.listen(5)
    
    #List of ports that are readable.
    #Server is readable as it receives connection requests.
    readList  = [mirror_server]

    while True:
        readInput, writeInput, exceptInput = select.select(readList, readList, readList)

        # Data received
        for s in readInput:
            if s is mirror_server:
                (connection, addr) = s.accept()
                connection.setblocking(0)
                readList.append(connection)

            else:
                # Switch sent data
                data = s.recv(2048)

                if data:
                    # Print received data
                    print "New data %r received from switch" % data
                else:
                    print "No more data coming from port %s" % s.getpeername()[1] 
                    # Stop listening for input on the connection
                    readList.remove(s)
                    s.close()
