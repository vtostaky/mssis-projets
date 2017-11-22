#!/usr/local/bin/python2
import socket
import sys
import select
import Queue

def find_end_of_header(data, marker):
    position_eth_type = data.find(marker)

    # Should have MAC addresses before end of frame
    while position_eth_type < 12:
        position_eth_type = data.find(marker, position_eth_type)
        if position_eth_type < 0:
            break

    return position_eth_type


def get_macaddr_from_datagram(data):
    # Try and locate end of Ethernet frame / beginning of IPv4 data
    #0x0800 : IPv4
    position = find_end_of_header(data, '\x08\x00\x45')

    # No IPv4 : try IPv6
    #0x86DD : IPv6
    if position < 0:
        position = find_end_of_header(data, '\x86\xdd')
    
    # No IPv6 : try ARP
    #0x0806 : ARP
    if position < 0:
        position = find_end_of_header(data, '\x08\x06')

    if position  < 0:
        return None, None

    # Return origin & destination MAC addresses
    return data[position-12:position-6], data[position-6:position]


if __name__ == "__main__":
    if (len(sys.argv) < 2 or not sys.argv[1].isdigit()):
        print 'Usage: qemu_listener.py <port> [<mirror_port>]',
        exit()

    port = int(sys.argv[1])
   
    # If a mirror port has been specified, try and use it
    hasMirrorPort = False
    try:
        # Open a dedicated socket and connect to the mirror port listener
        tomirror = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        tomirror.connect(('',int(sys.argv[2])))
    except IndexError:
        pass
    except ValueError:
        print "Mirror port should be an integer - second parameter ignored"
    except socket.error as msg:
        print "Please run mirror listener before using it - second parameter ignored"
    except OverflowError:
        print 'Mirror port must be 0-65535'
    else:
        hasMirrorPort = True

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        server.bind(('', port))
    except OverflowError:
        print 'Port must be 0-65535',
        exit()
    server.listen(5)

    #List of ports that are writable.
    writeList = []
    #List of ports that are readable.
    #Server is readable as it receives connection requests.
    readList  = [server]

    #Dictionary of queues {connection: data} to store data for each connection
    data_queues = {}

    #Dictionary - correspondance table between MAC @ & connection handles (i.e. CAM table)
    macaddr_connection = {}

    while True:
        readPorts, writePorts, exceptPorts = select.select(readList, writeList, writeList)

        # Data received
        for s in readPorts:
            if s is server:
                (connection, addr) = s.accept()
                connection.setblocking(0)

                # Add connection to the list of inputs & outputs
                readList.append(connection)
                writeList.append(connection)
                
                print '%d: connection from %s connection %s' % (len(readList), addr, connection)
                # Give the connection a queue for data we want to send
                data_queues[connection] = Queue.Queue()

            else:
                # A connected qemu sent data
                # Ethernet frame can convey 1500 bytes of data max, use nearest higher power of 2.
                data = s.recv(2048)

                if data:
                    # Minimal size of Ethernet frame is 64.
                    # If smaller, drop packet.
                    if len(data) < 64:
                        continue

                    print "New data received from %s" % s.getpeername()[1]
                    
                    # Retrieve dest address, and add data to the corresponding queue(s)
                    macaddr_dest, macaddr_orig = get_macaddr_from_datagram(data)

                    # Problem with received data : drop data
                    if macaddr_orig is None:
                        continue

                    # Associate connection to MAC address in CAM table
                    macaddr_connection[macaddr_orig] = s

                    if macaddr_dest not in macaddr_connection:
                        print "BROADCAST transmission"
                        # In case of ff:ff:ff:ff:ff:ff, or unkown MAC address, broadcast
                        for connection in writeList:
                            if connection != s:
                                print "Put data to queue of connection %s" % connection.getpeername()[1]
                                data_queues[connection].put(data)
                    else:
                        # Known dest MAC address : send data in unicast
                        print "UNICAST to %r" % macaddr_dest
                        try:
                            data_queues[macaddr_connection[macaddr_dest]].put(data)
                        except KeyError:
                            print "No device connected with MAC address %r" % macaddr_dest
                            # Connection lost with given MAC address, remove it from CAM
                            del macaddr_connection[macaddr_dest]

                    if hasMirrorPort:
                        # Send received data to mirror port
                        print "Send data to mirror port"
                        tomirror.send(data)
                else:
                    print "No more data coming from port %s" % s.getpeername()[1] 
                    # Stop listening for input on the connection
                    if s in writeList:
                        writeList.remove(s)
                    readList.remove(s)
                    s.close()
                    # Remove data queue corresponding to the connection
                    del data_queues[s]

        # Data to write
        for s in writePorts:
            try:
                if data_queues[s]:
                    # Get data from queue without blocking in case queue is empty.
                    datagram = data_queues[s].get(False)
            except Queue.Empty:
                # Nothing to send to this port.
                pass
            except KeyError:
                # The queue associated to this connection has been deleted
                # The writePorts list has not yet been updated accordingly - ignore
                pass
            else:
                print "Send datagram to port %s" % s.getpeername()[1]
                # Send data to corresponding port / connection
                s.send(datagram)

