package zx.soft.dfss.core;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ListIterator;

public class ServentRequestingFile implements Runnable {

	static boolean status = false;
	private final int index;

	ServentRequestingFile(String i) {
		index = Integer.parseInt(i);
	}

	public void run() {

		clientHttp();

	}

	public void clientHttp() {
		BufferedReader in = null;
		try {

			ListIterator<SearchHitMessage> it = Simpella.searchHitMsgList.listIterator();
			String ipToConnect = "";
			String portToConnect = "";
			String fileName = "";
			String fileIndex = "";
			int flag = 0;
			while (it.hasNext()) {
				SearchHitMessage shm = it.next();
				ListIterator<FileDetails> lit = shm.fDetails.listIterator();
				while (lit.hasNext()) {
					FileDetails fd = lit.next();
					if (fd.index == index) {
						flag = 1;
						ipToConnect = shm.msgFromIp;
						portToConnect = String.valueOf(shm.msgFromPort);
						fileName = fd.fileName;
						fileIndex = String.valueOf(fd.fileIndex);

					}
				}
			}
			if (flag == 1) {
				URL url = new URL("http://" + ipToConnect + ":" + portToConnect + "/get/" + fileIndex + "/" + fileName);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setRequestProperty("User-Agent", " Simpella 0.6");
				connection.setRequestProperty("Range", " bytes=0-");
				in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String s = in.readLine();
				String res = s;
				//System.out.println(s);
				while (in.ready()) {
					s = in.readLine();
					res = res + s;
					//System.out.println(s);
					if (s.trim().isEmpty())
						break;
				}

				String temp = res.substring(res.indexOf("Content-length"), res.indexOf("Connection"));
				//System.out.println("temp is "+temp);
				String contentLength = temp.substring(temp.indexOf(": ") + 2);
				//System.out.println("content Length is "+contentLength);
				int conLength = Integer.parseInt(contentLength);
				if (!res.contains("503 File not found"))
					httpProtocol(connection, fileName, ipToConnect, portToConnect, conLength);
				else if (res.contains("503 File not found"))
					System.out.println("File not Found: Try Again");
				in.close();
			} else {
				System.out.println("Error: Index Incorrect");
			}

		} catch (IOException e) {
			System.out.println("Connection not possible at this point..try again");
		}

	}

	public void httpProtocol(HttpURLConnection connection, String fileName, String ipToConnect, String portToConnect,
			int contentLength) {

		BufferedInputStream in = null;
		FileOutputStream fout = null;
		try {
			in = new BufferedInputStream(connection.getInputStream());
			fout = new FileOutputStream(Simpella.searchDir + fileName);
			byte data[] = new byte[1024];
			int count;
			int total = 0;
			System.out.println("Connection Established ");
			System.out.println("Downloading: " + fileName);
			System.out.println("--------------------");
			while ((count = in.read(data, 0, 1024)) != -1) {
				total = total + count;
				fout.write(data, 0, count);
				if (status) {
					float per = ((float) total / (float) contentLength) * 100;
					System.out.println("DOWNLOAD STATS:");
					System.out.println("-----------------");
					System.out.print(ipToConnect + ":" + portToConnect + "		");
					System.out.printf("%.2f", per);
					System.out.println("%		" + (float) total / 1024 + "kb/" + (float) contentLength / 1024 + "kb");
					System.out.println("File Name: " + fileName);
					System.out.println();
					status = false;
				}
			}
			System.out.println("Download Complete for file: " + fileName);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				if (fout != null)
					fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

}