package zx.soft.dfss.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;

public class Simpella implements Runnable {
	private ServerSocket tcpSocket;

	public Socket server;
	public Socket peer;
	public static LinkedList<PeerData> connectionList;
	public static int cid;
	public static int tcpPort;
	public static int fileDownloadingPort;
	public static String ipAddr;
	public DataOutputStream dout;
	public DataInputStream din;
	public PeerInfo pinfo;
	public static LinkedList<RoutingInfo> incomingMsgList;
	public static List<FileDetails> fileDetailsList;
	public static List<PingMessage> pingMsgList;
	public static List<PongMessage> pongMsgList;
	public static List<SearchMessage> searchMsgList;
	public static List<SearchHitMessage> searchHitMsgList;
	public static List<SearchHitMessage> searchHitMsgListTemp;
	public static UUID serventId;
	public static Charset charset;
	public static int fileIndex = 10000;
	public static int index = 1;
	public static int incomingConnections = 0;
	public static int queryResponseCount = 0;
	public static long bytesSent = 0;
	public static int packsSent = 0;
	public static String searchDir = System.getProperty("user.dir") + "/";
	public long bytesAlloc;
	public long filesCount;
	public static List<String> mySearch;
	public boolean flag = false;

	//public DataInputStream din;
	public Simpella(int tcpport, int fileDownloadingport, int backlog) throws IOException {
		tcpPort = tcpport;
		fileDownloadingPort = fileDownloadingport;
		cid = 1;
		tcpSocket = new ServerSocket(tcpport, backlog);
		Socket testSocket = new Socket("8.8.8.8", 53);
		ipAddr = testSocket.getLocalAddress().getHostAddress();
		testSocket.close();
		serventId = UUID.randomUUID();
		charset = Charset.forName("UTF-8");

	}

	public Simpella(int backlog) throws IOException {

		tcpPort = 6346;
		int tcpport = tcpPort;
		fileDownloadingPort = 5635;
		cid = 1;
		tcpSocket = new ServerSocket(tcpport, backlog);
		Socket testSocket = new Socket("8.8.8.8", 53);
		ipAddr = testSocket.getLocalAddress().getHostAddress();
		testSocket.close();
		serventId = UUID.randomUUID();
		charset = Charset.forName("UTF-8");

	}

	public Simpella(Socket p) {
		peer = p;
	}

	public Simpella() {
		//May be needed;
	}

	// Code for listening continously for any incoming connections
	public void server() {
		try {
			System.out.println("Local IP: " + ipAddr);
			System.out.println("Simpella Net Port: " + tcpPort);
			System.out.println("Downloading Port: " + fileDownloadingPort);
			System.out.println("Simpella Version 0.6 (c) 2012-2013");
			System.out.println();
			while (true) {
				try {
					server = tcpSocket.accept();
					maintain_list(server);
					incomingConnections++;
				} catch (SocketTimeoutException s) {
					System.out.println("Socket timed out!");

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Thread to read incoming connection
	public void run() {

		while (true) {

			try {
				DataInputStream din = new DataInputStream(peer.getInputStream());
				int len = din.readInt();
				byte[] data = new byte[len];
				if (len > 0)
					din.readFully(data);
				if (len == 0)
					din.close();
				recvMsgHandler(data, peer);
			} catch (EOFException e) {
				try {
					peer.close();
					ListIterator<PeerData> li = connectionList.listIterator();
					while (li.hasNext()) {
						PeerData p = li.next();
						if (p.p_socket == peer)
							li.remove();
					}//System.out.println("Connection Closed");
				} catch (IOException ignored) {

				}
				break;
			} catch (IOException e) {
				try {
					peer.close();
					ListIterator<PeerData> li = connectionList.listIterator();
					while (li.hasNext()) {
						PeerData p = li.next();
						if (p.p_socket == peer)
							li.remove();
					}
				} catch (IOException ignored) {

				}
				break;
			}

		}

	}

	public void disconnectServer() {
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Maintain incoming connection list
	public void maintain_list(Socket peer) {
		try {
			PeerData pData = new PeerData();
			dout = new DataOutputStream(peer.getOutputStream());
			InetAddress addr = peer.getInetAddress();
			//pInfo.hostname=addr.getHostName();
			pData.connId = cid;
			cid++;
			pData.ip = addr.getHostAddress();
			pData.port = peer.getPort();
			pData.dout = dout;
			pData.p_socket = peer;
			connectionList.add(pData);
			Runnable r1 = new Simpella(peer);
			Thread t = new Thread(r1);
			t.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// If disconnect remove it from the list
	public void disconnect(int connId) {
		ListIterator<PeerData> li = connectionList.listIterator();
		while (li.hasNext()) {
			PeerData pData = li.next();
			if (pData.connId == connId) {
				try {
					pData.p_socket.close();
					li.remove();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

	// Send Ping Message
	public void sendPing(byte[] packet) {

		ListIterator<PeerData> li = connectionList.listIterator();
		while (li.hasNext()) {
			PeerData p = li.next();
			try {
				int len = packet.length;
				p.packSent = p.packSent + 1;
				p.bytesSent = p.bytesSent + len;
				packsSent = packsSent + 1;
				bytesSent = bytesSent + len;
				p.dout.writeInt(len);
				if (len > 0)
					p.dout.write(packet);

			} catch (NumberFormatException e) {
				System.out.println("Connection Id in Numeric form only");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	// Check byte 17 in the packet to check which type of message arrived 
	public void recvMsgHandler(byte[] packet, Socket peer) {
		if (packet.length > 16) {
			if (packet[16] == (byte) 0)
				recvPingMsg(packet, peer);
			else if (packet[16] == (byte) 1)
				recvPongMsg(packet, peer);
			else if (packet[16] == (byte) 80)
				recvSearchMsg(packet, peer);
			else if (packet[16] == (byte) 81)
				recvSearchHitMsg(packet, peer);
			else
				recvOtherMsg(packet, peer);

		} else {
			recvOtherMsg(packet, peer);
		}
	}

	public void recvPingMsg(byte[] packet, Socket peer) {
		int flag = 0;
		ListIterator<PingMessage> it = pingMsgList.listIterator();
		byte[] guidByte = new byte[16];
		for (int i = 0; i < 16; i++)
			guidByte[i] = packet[i];
		while (it.hasNext()) {
			PingMessage pm = it.next();
			if ((pm.uniqueId.compareTo(UUID.nameUUIDFromBytes(guidByte)) == 0))
				flag = 1;

		}
		if (flag == 0) {
			PingMessage pim = new PingMessage();
			//pim.msgType = (byte)0;
			pim.uniqueId = UUID.nameUUIDFromBytes(guidByte);
			pim.msgFromPort = peer.getPort();
			pim.msgFromSocket = peer;
			InetAddress addr = peer.getInetAddress();
			pim.msgFromIp = addr.getHostAddress();
			pim.bytesReceive = packet.length;
			pingMsgList.add(pim);
			ListIterator<PeerData> temp = connectionList.listIterator();
			while (temp.hasNext()) {
				PeerData p = temp.next();
				if (p.p_socket == peer) {
					p.packRecv = p.packRecv + 1;
					p.bytesRecv = p.bytesRecv + packet.length;
				}
			}

			int ttl = packet[17];
			int hop = packet[18];
			ttl = ttl - 1;
			hop = hop + 1;
			packet[17] = (byte) ttl;
			packet[18] = (byte) hop;
			ListIterator<PeerData> li = connectionList.listIterator();
			while (li.hasNext()) {
				PeerData pi = li.next();
				if (pi.p_socket != peer && ttl > 0) {
					try {
						int len = packet.length;
						pi.packSent = pi.packSent + 1;
						pi.bytesSent = pi.bytesSent + len;
						packsSent = packsSent + 1;
						bytesSent = bytesSent + len;
						pi.dout.writeInt(len);
						if (len > 0)
							pi.dout.write(packet, 0, len);

					} catch (NumberFormatException e) {
						System.out.println("Connection Id in Numeric form only");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			packet[16] = (byte) 1;
			packet[17] = (byte) 7;
			packet[18] = (byte) 0;
			byte[] header = new byte[23];
			for (int i = 0; i < 23; i++)
				header[i] = packet[i];
			Simpella.pongMsgList.clear();
			sendPong(header, peer);
		}

	}

	public void recvPongMsg(byte[] packet, Socket peer) {

		byte[] guidByte = new byte[16];
		for (int i = 0; i < 16; i++)
			guidByte[i] = packet[i];
		PongMessage pom = new PongMessage();
		pom.uniqueId = UUID.nameUUIDFromBytes(guidByte);
		pom.bytesReceive = packet.length;
		byte[] tempArry = new byte[4];
		int j = 25;
		for (int i = 0; i < 4; i++) {
			tempArry[i] = packet[j];
			j++;
		}
		ByteBuffer bufferPort = ByteBuffer.wrap(packet, 23, 2);
		int port = bufferPort.getShort();
		pom.msgFromPort = port;
		try {
			InetAddress addre = InetAddress.getByAddress(tempArry);
			pom.msgFromIp = addre.toString();
			//System.out.println(addre.toString());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		ByteBuffer bufferStats = ByteBuffer.wrap(packet, 29, 8);
		int fcount = bufferStats.getInt();
		int bsize = bufferStats.getInt();
		pom.filesCount = fcount;
		pom.bytesShared = bsize;
		pongMsgList.add(pom);
		ListIterator<PeerData> temp = connectionList.listIterator();
		while (temp.hasNext()) {
			PeerData p = temp.next();
			if (p.p_socket == peer) {
				p.packRecv = p.packRecv + 1;
				p.bytesRecv = p.bytesRecv + packet.length;
			}
		}
		int ttl = packet[17];
		int hop = packet[18];
		ttl = ttl - 1;
		hop = hop + 1;
		packet[17] = (byte) ttl;
		packet[18] = (byte) hop;
		ListIterator<PingMessage> it = pingMsgList.listIterator();
		while (it.hasNext()) {
			PingMessage pm = it.next();
			if ((pm.uniqueId.compareTo(UUID.nameUUIDFromBytes(guidByte)) == 0)) {
				ListIterator<PeerData> li = connectionList.listIterator();
				while (li.hasNext()) {
					PeerData p = li.next();
					if (p.p_socket == pm.msgFromSocket && ttl > 0) {
						try {
							int len = packet.length;
							p.packSent = p.packSent + 1;
							p.bytesSent = p.bytesSent + len;
							packsSent = packsSent + 1;
							bytesSent = bytesSent + len;
							p.dout.writeInt(len);
							if (len > 0)
								p.dout.write(packet, 0, len);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

	}

	public void recvSearchMsg(byte[] packet, Socket peer) {
		int flag = 0;
		ListIterator<SearchMessage> it = searchMsgList.listIterator();
		byte[] guidByte = new byte[16];
		for (int i = 0; i < 16; i++)
			guidByte[i] = packet[i];
		while (it.hasNext()) {
			SearchMessage sm = it.next();
			if ((sm.uniqueId.compareTo(UUID.nameUUIDFromBytes(guidByte)) == 0))
				flag = 1;

		}
		if (flag == 0) {
			SearchMessage sm = new SearchMessage();
			sm.uniqueId = UUID.nameUUIDFromBytes(guidByte);
			sm.msgFromPort = peer.getPort();
			InetAddress addr = peer.getInetAddress();
			sm.msgFromIp = addr.getHostAddress();
			sm.bytesReceive = packet.length;
			sm.msgFromSocket = peer;
			int j = 0;
			byte[] fname = new byte[256];
			for (int i = 25; i < packet.length; i++) {
				fname[j] = packet[i];
				j++;
			}
			try {
				ByteBuffer str = ByteBuffer.wrap(fname);
				CharsetDecoder decoder = charset.newDecoder();
				CharBuffer cbuf = decoder.decode(str);
				//fileNames[i] = cbuf.toString();
				String fnm = cbuf.toString();
				sm.fileName = fnm;
				//System.out.println("Searching for File: "+fnm);
			} catch (Exception e) {
				e.printStackTrace();
			}
			searchMsgList.add(sm);
			ListIterator<PeerData> temp = connectionList.listIterator();
			while (temp.hasNext()) {
				PeerData p = temp.next();
				if (p.p_socket == peer) {
					p.packRecv = p.packRecv + 1;
					p.bytesRecv = p.bytesRecv + packet.length;
				}
			}
			int ttl = packet[17];
			int hop = packet[18];
			ttl = ttl - 1;
			hop = hop + 1;
			packet[17] = (byte) ttl;
			packet[18] = (byte) hop;
			ListIterator<PeerData> li = connectionList.listIterator();
			while (li.hasNext()) {
				PeerData pi = li.next();
				if (pi.p_socket != peer && ttl > 0) {
					try {
						int len = packet.length;
						pi.packSent = pi.packSent + 1;
						pi.bytesSent = pi.bytesSent + len;
						packsSent = packsSent + 1;
						bytesSent = bytesSent + len;
						pi.dout.writeInt(len);
						if (len > 0)
							pi.dout.write(packet, 0, len);
					} catch (NumberFormatException e) {
						System.out.println("Connection Id in Numeric form only");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
			packet[16] = (byte) 81;
			packet[17] = (byte) 7;
			packet[18] = (byte) 0;
			queryHit(packet, peer);
		}
	}

	public void recvSearchHitMsg(byte[] packet, Socket peer) {
		byte[] guidByte = new byte[16];
		for (int i = 0; i < 16; i++)
			guidByte[i] = packet[i];
		SearchHitMessage shm = new SearchHitMessage();
		shm.uniqueId = UUID.nameUUIDFromBytes(guidByte);
		shm.bytesReceive = packet.length;
		byte[] payload = new byte[4096];
		int j = 23;
		for (int i = 0; i < 4096; i++) {
			payload[i] = packet[j];
			j++;
		}
		byte[] ipArry = new byte[4];
		j = 3;
		for (int i = 0; i < 4; i++) {
			ipArry[i] = payload[j];
			j++;
		}

		try {
			InetAddress ad = InetAddress.getByAddress(ipArry);
			String s = ad.toString();
			shm.msgFromIp = s.substring(1);
			//System.out.println("Connection IP " +ad.toString());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		//System.out.println(UUID.nameUUIDFromBytes(guidByte));
		ByteBuffer bufferQueryHit = ByteBuffer.wrap(payload, 0, payload.length);
		short filesHit = bufferQueryHit.get();
		short downloadingPort = bufferQueryHit.getShort();
		shm.msgFromPort = downloadingPort;
		//int speed = bufferQueryHit.getInt(7);
		int fileIndex, fileSize;
		String filename;
		int pos = 11;
		shm.fDetails = new ArrayList<FileDetails>();
		//System.out.print("Press Enter to Continue.... ");
		for (int i = 0; i < filesHit; i++) {
			FileDetails fd = new FileDetails();
			fd.index = index;
			fileIndex = bufferQueryHit.getInt(pos);
			fd.fileIndex = fileIndex;
			pos = pos + 4;
			fileSize = bufferQueryHit.getInt(pos);
			fd.fileSize = fileSize;
			pos = pos + 4;
			try {
				int tmp = pos, size = 0;
				while (payload[tmp] != (byte) 0) {
					size++;
					tmp++;
				}
				ByteBuffer str = ByteBuffer.wrap(payload, pos, size);
				CharsetDecoder decoder = charset.newDecoder();
				CharBuffer cbuf = decoder.decode(str);
				filename = cbuf.toString();
				fd.fileName = filename;
				pos = pos + size + 1;
				index++;
				shm.fDetails.add(fd);
				//  System.out.println(fd.index +")  "+shm.msgFromIp+":"+downloadingPort+"   Size: "+(float)fd.fileSize/1024+"kb");
				// System.out.println("File Name: "+fd.fileName);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//		long lsb = bufferQueryHit.getLong(pos);
		pos = pos + 8;
		//		long msb = bufferQueryHit.getLong(pos);
		//		UUID serventID = new UUID(msb, lsb);
		// System.out.println("Servents id :"+ serventID);
		searchHitMsgList.add(shm);
		searchHitMsgListTemp.add(shm);
		ListIterator<PeerData> temp = connectionList.listIterator();
		while (temp.hasNext()) {
			PeerData p = temp.next();
			if (p.p_socket == peer) {
				p.packRecv = p.packRecv + 1;
				p.bytesRecv = p.bytesRecv + packet.length;
			}
		}
		int ttl = packet[17];
		int hop = packet[18];
		ttl = ttl - 1;
		hop = hop + 1;
		packet[17] = (byte) ttl;
		packet[18] = (byte) hop;
		ListIterator<SearchMessage> it = searchMsgList.listIterator();
		while (it.hasNext()) {
			SearchMessage sm = it.next();
			if ((sm.uniqueId.compareTo(UUID.nameUUIDFromBytes(guidByte)) == 0)) {
				ListIterator<PeerData> li = connectionList.listIterator();
				while (li.hasNext()) {
					PeerData p = li.next();
					if (p.p_socket == sm.msgFromSocket && ttl > 0) {
						try {
							int len = packet.length;
							p.packSent = p.packSent + 1;
							p.bytesSent = p.bytesSent + len;
							packsSent = packsSent + 1;
							bytesSent = bytesSent + len;
							p.dout.writeInt(len);
							if (len > 0)
								p.dout.write(packet, 0, len);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

	}

	public void recvOtherMsg(byte[] packet, Socket peer) {
		ByteBuffer str = ByteBuffer.wrap(packet);
		CharsetDecoder decoder = charset.newDecoder();
		try {
			PeerInfo pTemp = new PeerInfo();
			CharBuffer cbuf = decoder.decode(str);
			String msg = cbuf.toString();
			if (incomingConnections > 3) {

				pTemp.sendInitiatorString("SIMPELLA/0.6 503 Maximum number of connections reached. Sorry!\r\n", peer);

				ListIterator<PeerData> li = connectionList.listIterator();
				while (li.hasNext()) {
					PeerData p = li.next();
					if (p.p_socket == peer) {
						//p.p_socket.close();
						peer.close();
						li.remove();
					}
				}

				incomingConnections--;
			} else if (msg.contains("503")) {
				System.out.println(msg);
				ListIterator<PeerData> li = connectionList.listIterator();
				while (li.hasNext()) {
					PeerData p = li.next();
					if (p.p_socket == peer) {
						//p.p_socket.close();
						peer.close();
						li.remove();
					}
				}

				PeerInfo.outgoingConnection--;
			} else if (msg.contains("200")) {
				System.out.println(msg);
				pTemp.sendInitiatorString("SIMPELLA/0.6 I appreciate the connection!\r\n", peer);
			} else if (msg.contains("CONNECT")) {
				pTemp.sendInitiatorString("SIMPELLA/0.6 200 Connection Established!\r\n", peer);
			} else {
				System.out.println(msg);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void sendPong(byte[] header, Socket peer) {

		int len = header.length;
		ByteBuffer bb = ByteBuffer.wrap(new byte[14 + len]);
		bb.put(header);
		bb.putShort((short) peer.getLocalPort());
		bb.put(peer.getLocalAddress().getAddress());
		calculateDirStats();
		bb.putInt((int) filesCount);
		bb.putInt((int) bytesAlloc);
		byte[] packet = bb.array();
		ListIterator<PeerData> li = connectionList.listIterator();
		while (li.hasNext()) {
			PeerData p = li.next();
			if (p.p_socket == peer) {
				try {
					len = packet.length;
					p.packSent = p.packSent + 1;
					p.bytesSent = p.bytesSent + len;
					packsSent = packsSent + 1;
					bytesSent = bytesSent + len;
					p.dout.writeInt(len);
					if (len > 0)
						p.dout.write(packet, 0, len);

				} catch (NumberFormatException e) {
					System.out.println("Connection Id in Numeric form only");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	// This function is helper for search in the directory
	public void query(String filename) {
		searchHitMsgListTemp.clear();
		String fileName = filename.substring(0, filename.length() - 1);
		mySearch.add(fileName);
		System.out.println("Searching Simpella Network for file: " + filename);
		byte[] header = new PeerInfo().headerGenerate("Query");
		int len = header.length;
		ByteBuffer bb = ByteBuffer.wrap(new byte[len + 3 + fileName.length()]);
		bb.put(header);
		bb.putShort((short) 0);
		CharsetEncoder encoder = charset.newEncoder();
		CharBuffer cbuf = CharBuffer.wrap(fileName);
		try {
			bb.put(encoder.encode(cbuf));
		} catch (Exception e) {
			e.printStackTrace();
		}
		bb.put((byte) 0);
		byte[] packet = bb.array();
		sendPing(packet);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		ListIterator<SearchHitMessage> it = searchHitMsgListTemp.listIterator();
		while (it.hasNext()) {
			SearchHitMessage shm = it.next();
			ListIterator<FileDetails> lit = shm.fDetails.listIterator();
			while (lit.hasNext()) {
				FileDetails fd = lit.next();
				System.out.println(fd.index + ")  " + shm.msgFromIp + ":" + shm.msgFromPort + "   Size: "
						+ (float) fd.fileSize / 1024 + "kb");
				System.out.println("File Name: " + fd.fileName);

			}
		}
		System.out.println();
	}

	// This function is search helper
	public void queryFind(String filename) {
		searchHitMsgListTemp.clear();
		System.out.println("Searching Simpella Network for file: " + filename);
		byte[] header = new PeerInfo().headerGenerate("Query");
		int len = header.length;
		ByteBuffer bb = ByteBuffer.wrap(new byte[len + 3 + 0]);
		bb.put(header);
		bb.putShort((short) 0);
		CharsetEncoder encoder = charset.newEncoder();
		CharBuffer cbuf = CharBuffer.wrap("");
		try {
			bb.put(encoder.encode(cbuf));
		} catch (Exception e) {
			e.printStackTrace();
		}
		bb.put((byte) 0);
		byte[] packet = bb.array();
		packet[17] = (byte) 1;
		packet[18] = (byte) 0;
		sendPing(packet);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		ListIterator<SearchHitMessage> it = searchHitMsgListTemp.listIterator();
		while (it.hasNext()) {
			SearchHitMessage shm = it.next();
			ListIterator<FileDetails> lit = shm.fDetails.listIterator();
			while (lit.hasNext()) {
				FileDetails fd = lit.next();
				System.out.println(fd.index + ")  " + shm.msgFromIp + ":" + shm.msgFromPort + "   Size: "
						+ (float) fd.fileSize / 1024 + "kb");
				System.out.println("File Name: " + fd.fileName);

			}
		}
		System.out.println();
	}

	// This function will search in the directory
	public ArrayList<FileDetails> searchString(String message) {

		if (message.length() > 0) {
			String delims = "[ ]+";
			String[] keyWords = message.split(delims);
			File directory = new File(searchDir);
			ArrayList<FileDetails> ftemp = new ArrayList<FileDetails>();
			for (int j = 0; j < keyWords.length; j++) {
				String[] myFiles;
				final String searchquery = keyWords[j];
				FilenameFilter filter = new FilenameFilter() {
					public boolean accept(File directory, String fileName) {
						String[] fileNameSplit = fileName.split("[ ]+");
						int i = 0;
						while (i < fileNameSplit.length) {

							if (fileNameSplit[i].contains(".") && !searchquery.contains(".")) {
								String str = fileNameSplit[i].substring(0, fileNameSplit[i].indexOf("."));
								if (str.toLowerCase().equals(searchquery.toLowerCase()))
									return true;
								else
									i++;
							} else if (fileNameSplit[i].contains(".") && searchquery.contains(".")) {
								if (fileNameSplit[i].toLowerCase().equals(searchquery.toLowerCase()))
									return true;
								else
									i++;
							} else if (!fileNameSplit[i].contains(".") && searchquery.contains(".")) {
								if (fileNameSplit[i].toLowerCase().equals(
										searchquery.substring(0, searchquery.indexOf(".")).toLowerCase()))
									return true;
								else
									i++;
							} else {
								if (fileNameSplit[i].toLowerCase().equals(searchquery.toLowerCase()))
									return true;
								else
									i++;
							}

						}
						return false;
					}
				};

				myFiles = directory.list(filter);

				for (int i = 0; i < myFiles.length; i++) {

					File file = new File(searchDir + myFiles[i]);
					if (file.exists()) {
						int size = (int) file.length();
						FileDetails f = new FileDetails();
						f.index = fileIndex;
						f.fileSize = size;
						f.fileName = myFiles[i];
						fileIndex++;
						ftemp.add(f);
					}

				}
			}
			return ftemp;
		} else {
			File directory = new File(searchDir);
			File[] listOfFiles = directory.listFiles();
			ArrayList<FileDetails> ftemp = new ArrayList<FileDetails>();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					int size = (int) listOfFiles[i].length();
					FileDetails f = new FileDetails();
					f.index = fileIndex;
					f.fileSize = size;
					f.fileName = listOfFiles[i].getName();
					fileIndex++;
					ftemp.add(f);

				}
			}
			return ftemp;
		}

	}

	public void queryHit(byte[] packet, Socket peer) {
		int j = 0, i = 25;
		byte[] fname = new byte[256];
		while (packet[i] != (byte) 0) {
			fname[j] = packet[i];
			j++;
			i++;
		}
		String fnm = "";
		try {
			ByteBuffer fBuffer = ByteBuffer.wrap(fname, 0, j);
			CharsetDecoder decoder = charset.newDecoder();
			CharBuffer tmpbuf = decoder.decode(fBuffer);
			fnm = tmpbuf.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ArrayList<FileDetails> ftemp = searchString(fnm);
		if (ftemp.size() > 0) {
			byte[] header = new byte[23];
			for (i = 0; i < 23; i++)
				header[i] = packet[i];

			ByteBuffer bb = ByteBuffer.wrap(new byte[5020]);
			bb.put(header);
			bb.put((byte) ftemp.size());
			bb.putShort((short) fileDownloadingPort);
			bb.put(peer.getLocalAddress().getAddress());
			bb.putInt(32000);
			ListIterator<FileDetails> fd = ftemp.listIterator();
			while (fd.hasNext()) {
				FileDetails f = fd.next();
				bb.putInt(f.index);
				bb.putInt(f.fileSize);
				CharsetEncoder encoder = charset.newEncoder();
				CharBuffer cbuf = CharBuffer.wrap(f.fileName);
				try {
					bb.put(encoder.encode(cbuf));
					bb.put((byte) 0);
				} catch (Exception e) {
					e.printStackTrace();
				}

				fileDetailsList.add(f);

			}
			queryResponseCount++;
			bb.putLong(serventId.getLeastSignificantBits());
			bb.putLong(serventId.getMostSignificantBits());
			packet = bb.array();
			ListIterator<PeerData> li = connectionList.listIterator();
			while (li.hasNext()) {
				PeerData p = li.next();
				if (p.p_socket == peer) {
					try {
						int len = packet.length;
						p.packSent = p.packSent + 1;
						p.bytesSent = p.bytesSent + len;
						packsSent = packsSent + 1;
						bytesSent = bytesSent + len;
						p.dout.writeInt(len);
						if (len > 0)
							p.dout.write(packet, 0, len);

					} catch (NumberFormatException e) {
						System.out.println("Connection Id in Numeric form only");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public byte[] createPayload(byte[] header, Socket peer) {
		int len = header.length;
		ByteBuffer bb = ByteBuffer.wrap(new byte[14 + len]);
		bb.put(header);
		if (header[16] == (byte) 1) {
			bb.putShort((short) peer.getLocalPort());
			bb.put(peer.getLocalAddress().getAddress());

		}
		return bb.array();

	}

	public static List<FileDetails> getList() {
		return fileDetailsList;
	}

	public void setDirectory(String str) {
		if (str.equals("-i")) {
			System.out.println("Sharing:" + searchDir);
		} else if (str.equalsIgnoreCase("this")) {
			final String dir = System.getProperty("user.dir");
			searchDir = dir + "/";

		} else if (str.charAt(0) == '/') {
			if (str.charAt(str.length() - 1) != '/')
				searchDir = str + '/';
			else
				searchDir = str;
		} else if (str.charAt(0) != '/') {
			if (str.charAt(str.length() - 1) != '/')
				searchDir = searchDir + str + '/';
			else
				searchDir = searchDir + str;

		}

	}

	public void calculateDirStats() {
		try {
			File file = new File(searchDir);
			bytesAlloc = 0;
			filesCount = file.list().length;
			for (String s : file.list()) {
				File temp = new File(searchDir + s);
				if (temp.exists()) {
					bytesAlloc = bytesAlloc + temp.length();
				}
			}
		} catch (NullPointerException e) {
			System.out.println("Invalid Directory. Please specify a valid directory.");
		}
	}

	public void printScan() {
		calculateDirStats();
		System.out.println("Scanned " + filesCount + " Files and " + bytesAlloc + " bytes used..!");
		System.out.println();
	}

	public void info(String str) {
		if (str.equalsIgnoreCase("q")) {
			System.out.println("QUERY STATS:");
			System.out.println("---------------------");
			System.out.println("Queries: " + searchMsgList.size() + "		" + "Responses Sent: " + queryResponseCount);
			System.out.println();

		} else if (str.equalsIgnoreCase("s")) {
			calculateDirStats();
			System.out.println("SHARE STATS:");
			System.out.println("---------------------");
			System.out.println("Num of Files Shared: " + filesCount + "		" + "Size Shared: " + bytesAlloc);
			System.out.println();

		} else if (str.equalsIgnoreCase("n")) {
			System.out.println("NET STATS:");
			System.out.println("-----------------");
			Set<UUID> nodups = new HashSet<UUID>();
			long bytesRecv = 0;
			ListIterator<PingMessage> pi_it = pingMsgList.listIterator();
			while (pi_it.hasNext()) {
				PingMessage pm = pi_it.next();
				bytesRecv = bytesRecv + pm.bytesReceive;
				nodups.add(pm.uniqueId);
				//System.out.println(r.msgcame_port+" "+r.msgType+ " "+r.uniqueId+" "+r.bytesReceive);
			}
			ListIterator<PongMessage> pom_it = pongMsgList.listIterator();
			while (pom_it.hasNext()) {
				PongMessage pom = pom_it.next();
				bytesRecv = bytesRecv + pom.bytesReceive;
				nodups.add(pom.uniqueId);
				//System.out.println(r.msgcame_port+" "+r.msgType+ " "+r.uniqueId+" "+r.bytesReceive);
			}
			ListIterator<SearchMessage> sm_it = searchMsgList.listIterator();
			while (sm_it.hasNext()) {
				SearchMessage sm = sm_it.next();
				bytesRecv = bytesRecv + sm.bytesReceive;
				nodups.add(sm.uniqueId);
				//System.out.println(r.msgcame_port+" "+r.msgType+ " "+r.uniqueId+" "+r.bytesReceive);
			}
			ListIterator<SearchHitMessage> shm_it = searchHitMsgList.listIterator();
			while (shm_it.hasNext()) {
				SearchHitMessage shm = shm_it.next();
				bytesRecv = bytesRecv + shm.bytesReceive;
				nodups.add(shm.uniqueId);
				//System.out.println(r.msgcame_port+" "+r.msgType+ " "+r.uniqueId+" "+r.bytesReceive);
			}
			int msgRecv = pingMsgList.size() + pongMsgList.size() + searchMsgList.size() + searchHitMsgList.size();
			System.out.println("Message Recevied: " + msgRecv + "  Message Sent: " + packsSent);
			System.out.println("Unique GUIDs in memory: " + nodups.size());
			System.out.println("Bytes Recevied: " + (float) bytesRecv / 1024 + "kb  Bytes Sent: " + (float) bytesSent
					/ 1024 + "kb");
			System.out.println();
		} else if (str.equalsIgnoreCase("h")) {
			calculateDirStats();
			long totFiles = 0;
			long totSize = 0;
			int hosts = 0;
			ListIterator<PongMessage> li = pongMsgList.listIterator();
			while (li.hasNext()) {
				PongMessage pm = li.next();
				totFiles = totFiles + pm.filesCount;
				totSize = totSize + pm.bytesShared;
				++hosts;
			}

			totFiles = totFiles + filesCount;
			totSize = totSize + bytesAlloc;
			hosts = hosts + 1;
			System.out.println("HOSTS STATS:");
			System.out.println("-------------");
			System.out.println("Total No of Hosts: " + hosts);
			System.out.println("Total No of Files: " + totFiles);
			System.out.println("Total Size: " + (double) totSize / (1024 * 1024) + "Mb");
			System.out.println();
		} else if (str.equalsIgnoreCase("c")) {
			System.out.println("CONNECTION STATS:");
			System.out.println("-------------------");
			ListIterator<PeerData> li = connectionList.listIterator();
			while (li.hasNext()) {
				int i = 1;
				PeerData p = li.next();
				System.out.println(i + ") " + p.ip + ":" + p.port + "  Packs: " + p.packSent + ":" + p.packRecv
						+ "  Bytes: " + (float) p.bytesSent / 1024 + "kb:" + (float) p.bytesRecv / 1024 + "kb");
			}
			System.out.println();
		}

	}

	public void clear(String index, int check) {
		if (check == 2) {
			int indx = Integer.parseInt(index);
			System.out.println(indx);
			ListIterator<SearchHitMessage> it = searchHitMsgList.listIterator();
			int flag = 0;
			while (it.hasNext()) {
				if (flag == 1)
					break;
				SearchHitMessage shm = it.next();
				ListIterator<FileDetails> lit = shm.fDetails.listIterator();
				while (lit.hasNext()) {
					FileDetails fd = lit.next();
					if (fd.index == indx) {
						lit.remove();
						flag = 1;
						break;
					}
				}
			}
		} else if (check == 1) {
			searchHitMsgList.clear();
		}
		System.out.println("length " + searchHitMsgList.size());
	}

	public void monitor() {
		ListIterator<SearchMessage> it = searchMsgList.listIterator();
		System.out.println("MONITORING SIMPELLA NETWORK: ");
		System.out.println("-----------------------------");
		while (it.hasNext()) {
			SearchMessage sm = it.next();
			System.out.println("Search: " + "'" + sm.fileName + "'");
		}
		ListIterator<String> lit = mySearch.listIterator();
		while (lit.hasNext()) {
			String str = lit.next();
			System.out.println("Search: '" + str + "'");
		}
	}

	public static void main(String[] args) {
		try {
			if (args.length == 2) {
				if ((Integer.parseInt(args[0]) < 60000) && (Integer.parseInt(args[0]) > 0)
						&& (Integer.parseInt(args[1]) > 0) && (Integer.parseInt(args[1]) < 60000)) {
					Simpella tcp = new Simpella(Integer.parseInt(args[0]), Integer.parseInt(args[1]), 15);
					connectionList = new LinkedList<PeerData>();
					fileDetailsList = new ArrayList<FileDetails>();
					pingMsgList = new ArrayList<PingMessage>();
					searchMsgList = new ArrayList<SearchMessage>();
					searchHitMsgList = new ArrayList<SearchHitMessage>();
					searchHitMsgListTemp = new ArrayList<SearchHitMessage>();
					pongMsgList = new ArrayList<PongMessage>();
					mySearch = new ArrayList<String>();
					Runnable rtokenizer = new Tokenizer();
					Thread t_tokenizer = new Thread(rtokenizer);
					t_tokenizer.start();
					Runnable r_serverContainingFile = new ServentContaingFile(Integer.parseInt(args[1]));
					Thread t_serverContainingFile = new Thread(r_serverContainingFile);
					t_serverContainingFile.start();
					tcp.server();

				} else {
					System.out.println("Exiting: Port Range out of Scope");
					System.exit(0);
				}
			} else {
				Simpella tcp = new Simpella(15);
				connectionList = new LinkedList<PeerData>();
				fileDetailsList = new ArrayList<FileDetails>();
				pingMsgList = new ArrayList<PingMessage>();
				searchMsgList = new ArrayList<SearchMessage>();
				searchHitMsgList = new ArrayList<SearchHitMessage>();
				searchHitMsgListTemp = new ArrayList<SearchHitMessage>();
				pongMsgList = new ArrayList<PongMessage>();
				mySearch = new ArrayList<String>();
				Runnable rtokenizer = new Tokenizer();
				Thread t_tokenizer = new Thread(rtokenizer);
				t_tokenizer.start();
				Runnable r_serverContainingFile = new ServentContaingFile(5635);
				Thread t_serverContainingFile = new Thread(r_serverContainingFile);
				t_serverContainingFile.start();
				tcp.server();

			}

		} catch (NumberFormatException e) {
			System.out.println("Exiting: Port only take Number as an argument");
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // End Of Main

} // End of Class

