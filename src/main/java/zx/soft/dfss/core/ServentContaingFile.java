package zx.soft.dfss.core;

import java.io.BufferedReader;
//import java.net.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

//import java.util.Iterator;

class ServentContaingFile implements Runnable {
	public ServerSocket downloadSocket;
	public Socket listeningToDownload;
	public String ipaddr;
	public int listeningPort;

	ServentContaingFile(int listeningport) throws IOException {
		Socket testSocket = new Socket("8.8.8.8", 53);
		ipaddr = testSocket.getLocalAddress().getHostAddress();
		listeningPort = listeningport;
		testSocket.close();
		downloadSocket = new ServerSocket(listeningport);

	}

	public void run() {
		while (true) {
			try {
				listeningToDownload = downloadSocket.accept();
				System.out.println("Connection Done");
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(
						listeningToDownload.getInputStream()));
				DataOutputStream outToClient = new DataOutputStream(listeningToDownload.getOutputStream());
				String requestString = inFromClient.readLine();
				String header = requestString;
				while (inFromClient.ready()) {
					requestString = inFromClient.readLine();
					System.out.println(requestString);

				}
				String delims = "/";
				String[] tokens = header.split(delims);
				//System.out.println(tokens[2]);
				String[] againSplit = tokens[3].split(" HTTP");
				System.out.println("Downloading Request For File: " + againSplit[0]);
				sendFile(outToClient, againSplit[0], tokens[2]);
				inFromClient.close();
				outToClient.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	public void sendFile(DataOutputStream outToClient, String fileName, String index) {
		File file = new File(Simpella.searchDir + fileName);
		if (file.exists()) {
			long contentLength = file.length();
			try {

				outToClient.writeBytes("\r\n");
				outToClient.writeBytes("HTTP/1.1 200 OK\r\n");
				outToClient.writeBytes("User-Agent: Simpella\r\n");
				outToClient.writeBytes("Content-type: application/binary\r\n");
				outToClient.writeBytes("Content-length: " + String.valueOf(contentLength) + "\r\n");
				outToClient.writeBytes("Connection: close\r\n");
				outToClient.writeBytes("\r\n");
				FileInputStream fstream = new FileInputStream(Simpella.searchDir + fileName);
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = fstream.read(buffer)) != -1) {
					outToClient.write(buffer, 0, bytesRead);
				}
				fstream.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				outToClient.writeBytes("\r\n");
				outToClient.writeBytes("HTTP/1.1 503 File not found.\r\n");
				outToClient.writeBytes("\r\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}
