
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;

class sender {
	// helper function to check if the window is full or not
	private static boolean isWindowFull(int baseC, int nextSeqNumC) {
		final int windowSize = 10;
		final int seqNumModuloF = 32;
		// case where the base + N is greater than the maximum sequence number allowed
		if ((baseC + windowSize) >= seqNumModuloF) {
			if ((baseC <= nextSeqNumC) && (nextSeqNumC < seqNumModuloF)) {
				return false;
			}
			else if ((nextSeqNumC >= 0) && 
				(((baseC + windowSize) % seqNumModuloF) > nextSeqNumC)) {
				return false;
			}
			else {
				return true;
			}
		}
		// case where the base + N is within the range of possible sequence number
		else {
			if ((nextSeqNumC >= baseC) && (baseC + windowSize > nextSeqNumC)) {
				return false;
			}
			else {
				return true;
			}
		}
	}

	// helper function to get UPD data and datagram packet and send it to the emulator
	private static void udtSend(packet packetToSend, String emulAddr, int emulPortNum, DatagramSocket socketSenderRecv) throws Exception {
		byte[] byteToSend = packetToSend.getUDPdata();
		InetAddress emulIPAddr = InetAddress.getByName(emulAddr);
		DatagramPacket dpToSend = new DatagramPacket(byteToSend, byteToSend.length, emulIPAddr, emulPortNum);
		socketSenderRecv.send(dpToSend);
	}

	// helper function to receive the datagram packet, parse the udp data, and return the ack packet
	// if time out, throws SocketTimeoutException for resending the unacknowledged packets
	private static packet udtRecv(DatagramSocket socketSenderRecv) throws Exception, SocketTimeoutException {
		try {
			byte[] byteFromEmul = new byte[512];
			DatagramPacket dpRecv = new DatagramPacket(byteFromEmul, byteFromEmul.length);
			socketSenderRecv.receive(dpRecv);
			packet packetRecv = packet.parseUDPdata(byteFromEmul);
			return packetRecv;
		} catch (SocketTimeoutException e) {
			throw e;
		}
	}

	public static void main(String args[]) throws Exception {
		// check the number of parameters and define appropriate variables to store these parameters
		if (args.length != 4) {
			System.out.println("Invalid number of parameters. Must have four parameters.");
			System.exit(1);
		}

		String netEmulAddr = args[0];
		int portNumEmulRecv = Integer.parseInt(args[1]);
		int portNumSenderRecv = Integer.parseInt(args[2]);
		String fileName = args[3];

		// use buffered reader and writer to read from an input file 
		// and write the required logs for sequence number sent and ack received
		BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
		BufferedWriter seqNumLogWriter = new BufferedWriter(new FileWriter("seqnum.log"));
		BufferedWriter ackLogWriter = new BufferedWriter(new FileWriter("ack.log"));

		// count the base and next sequence number
		int base = 0;
		int nextSeqNum = 0;
		// indicate when all the packets are sent
		boolean isEOTReady = false;

		// two seconds delay for timer
		final int delay = 2000;
		// maximum data length defined from the packet class
		final int maxDataLength = 500;
		// maximum possible sequence number (not inclusive) defined from the packet class
		final int seqNumModulo = 32;

		// store the not yet acknowledged packets in collection 
		ArrayList<packet> unACKedPackets = new ArrayList<packet>();

		// socket to communicate with the emulator
		DatagramSocket socketSenderRecv = new DatagramSocket(portNumSenderRecv);

		while(true) {
			// send packet when it is not the end-of-transmission and not full window
			if (!isWindowFull(base, nextSeqNum) && !isEOTReady){
				/*
				if (base == 28){
					System.out.println("base: " + base);
					System.out.println("nsn: " + nextSeqNum);
				}
				*/
				// read from a file using character array with specific size
				char[] dataReadFromFile = new char[maxDataLength];
				int dataLength = fileReader.read(dataReadFromFile, 0, maxDataLength);
				// nothing to read from, then return -1 for datalength
				// indicate the end of transmission
				if (dataLength == -1) {
					isEOTReady = true;
				}
				else {
					// create packet and send the packet to the emulator
					// and then add it to the unacknowledged packet collection
					String dataToSend = new String(dataReadFromFile, 0, dataLength);
					packet packetToSend = packet.createPacket(nextSeqNum, dataToSend);
					udtSend(packetToSend, netEmulAddr, portNumEmulRecv, socketSenderRecv);
					unACKedPackets.add(packetToSend);
					// System.out.println("Add packet: " + packetToSend.getSeqNum());
					seqNumLogWriter.write(Integer.toString(nextSeqNum));
					seqNumLogWriter.newLine();
					if (base == nextSeqNum) {
						// timer starts
						socketSenderRecv.setSoTimeout(delay);
					}
					nextSeqNum = (nextSeqNum + 1) % seqNumModulo;
				}
			}
			// otherwise try to receive from the emulator
			else {
				try{
					packet packetRecv = udtRecv(socketSenderRecv);
					int seqNumRecv = packetRecv.getSeqNum();
					ackLogWriter.write(Integer.toString(seqNumRecv));
					ackLogWriter.newLine();
					// update base with the collective ack received from the emulator
					base = (seqNumRecv + 1) % seqNumModulo;
					// remove the acknowledged packets from the collection
					Iterator<packet> iter = unACKedPackets.iterator();
					while (iter.hasNext()) {
						packet unACKedPacket = iter.next();
						if (unACKedPacket.getSeqNum() == base) {
							break;							
						}
						else {
							// System.out.println("Delete packet: " + unACKedPacket.getSeqNum());
							iter.remove();
						}
					}
					// timer should stop here but not really required
					if (base == nextSeqNum) {
						// if it is the end-of-transmission, break the loop and get ready to send EOT packet
						if (isEOTReady) {
							break;
						}
					}
					else {
						//timer restarts
						socketSenderRecv.setSoTimeout(delay);
					}
				} catch (SocketTimeoutException e) {
					// when SocketTimoutException thrown, it means timeout, so resend all the not yet acknowledged packets and reset timer
					for (packet packetResend : unACKedPackets) {
						udtSend(packetResend, netEmulAddr, portNumEmulRecv, socketSenderRecv);
						int resendSeqNum = packetResend.getSeqNum();
						seqNumLogWriter.write(Integer.toString(resendSeqNum));
						seqNumLogWriter.newLine();
						socketSenderRecv.setSoTimeout(delay);
					}
				}
			}
		}
		// send EOT packet
		packet lastPacketToSend = packet.createEOT(nextSeqNum);
		udtSend(lastPacketToSend, netEmulAddr, portNumEmulRecv, socketSenderRecv);
		seqNumLogWriter.write(Integer.toString(nextSeqNum));
		seqNumLogWriter.newLine();
		packet packetRecv = udtRecv(socketSenderRecv);
		int seqNumRecv = packetRecv.getSeqNum();
		ackLogWriter.write(Integer.toString(seqNumRecv));
		ackLogWriter.newLine();

		// close
		socketSenderRecv.close();
		fileReader.close();
		seqNumLogWriter.close();
		ackLogWriter.close();
    }
}