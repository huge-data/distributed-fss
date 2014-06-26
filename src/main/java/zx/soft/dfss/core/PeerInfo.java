package zx.soft.dfss.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.UUID;

class RoutingInfo {
	public int msgcame_port;
	public byte msgType;
	public Socket msgcame_socket;
	public UUID uniqueId;
	public String msgcame_ip;
	public long bytesReceive;
}

class PingMessage {
	public int msgFromPort;
	public String msgFromIp;
	public UUID uniqueId;
	public long bytesReceive;
	public Socket msgFromSocket;
}

class PongMessage {
	public int msgFromPort;
	public String msgFromIp;
	public UUID uniqueId;
	public long bytesReceive;
	public int filesCount;
	public long bytesShared;
}

class SearchMessage {
	public int msgFromPort;
	public String msgFromIp;
	public UUID uniqueId;
	public long bytesReceive;
	public String fileName;
	public Socket msgFromSocket;
}

class SearchHitMessage {
	public int msgFromPort;
	public String msgFromIp;
	public UUID uniqueId;
	public long bytesReceive;
	public int index;
	public String fileName;
	public int fileSize;
	public List<FileDetails> fDetails;
}

class FileDetails {
	public int index;
	public String fileName;
	public int fileSize;
	public int fileIndex;
}

class ResultSet {

	String msgtype;
	String filename[];
}

class PeerData {
	int connId;
	Socket p_socket;
	int port;
	DataInputStream din;
	DataOutputStream dout;
	String ip;
	int packRecv;
	int packSent;
	long bytesRecv;
	long bytesSent;
}

class PeerInfo extends Thread {

	public Socket peerSocket;
	public static Charset charset;
	public static int outgoingConnection = 0;
	Simpella S = new Simpella();

	public PeerInfo() {
		charset = Charset.forName("UTF-8");
	}

	//public void run()
	//{
	public void connectTo(String ipServer, String port) {
		try {

			if (ipServer.equals(Simpella.ipAddr)
					&& ((Simpella.fileDownloadingPort == Integer.parseInt(port)) || (Simpella.tcpPort == Integer
							.parseInt(port)))) {
				System.out.println("Error: May be you are trying self connection");
			} else if (outgoingConnection <= 3) {
				peerSocket = new Socket(ipServer, Integer.parseInt(port));
				String toSend = "SIMPELLA CONNECT/0.6\r\n";
				sendInitiatorString(toSend, peerSocket);
				S.maintain_list(peerSocket);
				sendPing();
				outgoingConnection++;

			} else {
				System.out.println("Error: Your Outgoing Connection Limit Exceeded");

			}
		} catch (java.net.UnknownHostException e) {
			System.out.println("Connection Not Established: Unknown IP Address or Port");
			System.out.println("Enter a valid Simpella listening port");
		} catch (Exception e) {
			System.out.println("Connection Not Established: Unknown IP Address or Port");
			System.out.println("Enter a valid Simpella listening port");
			// e.printStackTrace();
		}
	}

	public byte[] headerGenerate(String msgType) {

		UUID uniqueId = UUID.randomUUID();
		ByteBuffer bb = ByteBuffer.wrap(new byte[23]);
		bb.putLong(uniqueId.getLeastSignificantBits());
		bb.putLong(uniqueId.getMostSignificantBits());
		if (msgType == "PING")
			bb.put((byte) 0);
		else if (msgType == "Query")
			bb.put((byte) 80);
		bb.put((byte) 7);
		bb.put((byte) 0);
		return bb.array();

	}

	public void sendInitiatorString(String str, Socket initiator) {
		ByteBuffer bb = ByteBuffer.wrap(new byte[str.length() + 1]);
		CharsetEncoder encoder = charset.newEncoder();
		CharBuffer cbuf = CharBuffer.wrap(str);
		try {
			bb.put(encoder.encode(cbuf));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			int len = bb.array().length;
			DataOutputStream dout = new DataOutputStream(initiator.getOutputStream());
			dout.writeInt(len);
			if (len > 0)
				dout.write(bb.array());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void sendPing() {
		byte[] header = headerGenerate("PING");
		Simpella.pongMsgList.clear();
		S.sendPing(header);
	}

	@Override
	public void run() {
	}

}
