package zx.soft.dfss.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class Tokenizer implements Runnable {

	public int flag = 0;

	public void run() {
		try {

			while (flag == 0) {
				String argsCmd = null;
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
				argsCmd = bufferedReader.readLine();
				tokenize(argsCmd);

			}
		} catch (IOException e) {
		}
	}

	public void tokenize(String argsCmd) {
		String delims = "[ ]+";
		String[] tokens = argsCmd.split(delims);
		Simpella P = new Simpella();
		PeerInfo pinf = new PeerInfo();
		try {

			if (tokens[0].equalsIgnoreCase("open")) {
				String[] openToken = tokens[1].split(":");
				if ((Integer.parseInt(openToken[1]) < 60000) && (Integer.parseInt(openToken[1]) > 1))
					pinf.connectTo(openToken[0], openToken[1]);
				else
					System.out.println("Port Error: Port Number mentioned incorrectly");

			} else if (tokens[0].equalsIgnoreCase("Share")) {
				P.setDirectory(tokens[1]);
			} else if (tokens[0].equalsIgnoreCase("Disconnect")) {
				P.disconnect(Integer.parseInt(tokens[1]));
			} else if (tokens[0].equalsIgnoreCase("find")) {
				String fileName = "";
				if (tokens.length > 1) {
					for (int i = 1; i < tokens.length; i++) {
						fileName = fileName + tokens[i];
						fileName = fileName + " ";
					}
					P.query(fileName);
				} else {
					P.queryFind(fileName);
				}
			}//else if(tokens[0].equalsIgnoreCase("find")) {
				// P.queryFind("");
				//}
			else if (tokens[0].equalsIgnoreCase("Info")) {
				if (tokens[1].equalsIgnoreCase("d"))
					ServentRequestingFile.status = true;
				else
					P.info(tokens[1]);
			} else if (tokens[0].equalsIgnoreCase("monitor")) {
				P.monitor();
			} else if (tokens[0].equalsIgnoreCase("Scan")) {
				P.printScan();
			} else if (tokens[0].equalsIgnoreCase("clear")) {
				if (tokens.length == 2)
					P.clear(tokens[1], 2);
				else if (tokens.length == 1)
					P.clear("Clear", 1);
			} else if (tokens[0].equalsIgnoreCase("Downloa")) {

				Runnable r_serventRequestingFile = new ServentRequestingFile(tokens[1]);
				Thread t_serventRequestingFile = new Thread(r_serventRequestingFile);
				t_serventRequestingFile.start();

			} else if (tokens[0].equalsIgnoreCase("Exit") || tokens[0].equalsIgnoreCase("Quit")) {
				//t_serventRequestingFile
				System.exit(0);
				flag = 0;
			} else if (tokens[0].equalsIgnoreCase("update")) {
				pinf.sendPing();
			} else if (tokens[0].length() == 0) {

			} else {
				System.out.println("No command Match Existing Commands ::");
			}
		} catch (java.lang.ArrayIndexOutOfBoundsException e) {
			System.out.println("No command Match Existing Commands :");
		} catch (Exception e) {
			System.out.println("No command Match Existing Commands :");
		}
	}

}
