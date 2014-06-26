A simplified version of the Gnutella Protocol Version 0.6, which we shall refer to as Simpella. Simpella is a  unix command line version of Gnutella Protocol to share, search and download files over the network. Simpella is entirely distributive in nature.

Four phases are covered in this project:
- Connecting to the network.
- Searching for files
- Downloading files
- Traffic Monitoring.

Brief overview:

Connecting to the network: Whenever any node joins the system it will send the "PING" message to the node to which it connects. Receiving this ping message the receiver node will forward this ping message to its adjacency and so on. When last node receives this message it will revert with PONG message that represent it gets the information of the incoming node.

Searching for files: Same rule follow while searching for file, the node sends the search query to one node in the network and that node will forward this search message to its adjacency plus search for this file on its own system. At last combine all the result and return back to the query client.

Downloading files: After receiving the file list client will also get the ip address and port number of the other client who has this file so it will connect directly(outside of simpella network) to download the file.

Traffic Monitoring: Hop count is taken as 7 which will decrease whenever any node forward a message to its adjacency to avoid the network flooding.

Project folder files:
1) Simpella - Main file includes all the functionalities.
2) PeerInfo - Maintaining information about peer and also when any node joins message will sent through "connectTo" function present in this.
3) ServentContaingFile: The client which has the file
4) ServentRequestingFile: The client requesting file
5) Tokenizer: Take input from command line and parse it as per the functionality.

Command:

1) java simpella <port1> <port2>  (To run the simplla)

2) info
info [cdhnqs] - Display list of current connections. The letters are:
•  c - Simpella network connections 
•	d - file transfer in progress (downloads only) 
•	h - number of hosts, number of files they are sharing, and total size of those shared files 
•	n - Simpella statistics: packets received and sent, number of unique packet IDs in memory (routing tables), total Simpella bytes received and sent so far. 
•	q - queries received and replies sent 
•	s - number and total size of shared files on this host
eg: simpella> info h

3) simpella> share this   (Share current directory)
   
   simpella> share -i     (Information about current directory shared)
   sharing /home/smathew2/Simpella/this
   
   simpella> scan         (Scan the current directory)
   scanning /home/smathew2/Simpella/this for files ...
   Scanned 2164 files and 3.96663e+07 bytes.


4) open <host:port> - open a connection to host at port
eg: open himank.cse.buffalo.edu:6346

5) update - send PINGs to all neighbors.

6) find <string> - start looking for files containing words in the string.
   simpella> find himank chaudhary
   searching Simpella network for `himank chaudhary'
   press enter to continue
   20 responses received
  
7) clear [file-no]  (clear particluar file no)

8) download <file-num>  (start downloading the file specified.)

9) monitor - display the queries people are searching for
   simpella> monitor
   MONITORING SIMPELLA NETWORK:
   Press enter to continue
