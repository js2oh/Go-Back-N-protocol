
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

import java.io.FileWriter;
import java.io.BufferedWriter;

public class receiver {
	// useful constants from the packet class
	private static final int windowSize = 10;
	private static final int maxDataLength = 500;
	private static final int seqNumModulo = 32;

	// helper function to get UPD data and datagram packet and send the ack packet to the emulator
	private static void udtSend(packet packetToSend, String emulAddr, int emulPortNum, DatagramSocket socketReceiverRecv) throws Exception {
		byte[] byteToSend = packetToSend.getUDPdata();
		InetAddress emulIPAddr = InetAddress.getByName(emulAddr);
		DatagramPacket dpToSend = new DatagramPacket(byteToSend, byteToSend.length, emulIPAddr, emulPortNum);
		socketReceiverRecv.send(dpToSend);
	}

	// helper function to receive the datagram packet, parse the udp data, and return it
	private static packet udtRecv(DatagramSocket socketReceiverRecv) throws Exception {
		byte[] byteFromEmul = new byte[512];
		DatagramPacket dpRecv = new DatagramPacket(byteFromEmul, byteFromEmul.length);
		socketReceiverRecv.receive(dpRecv);
		packet packetRecv = packet.parseUDPdata(byteFromEmul);
		return packetRecv;
	}

	public static void main(String[] args) throws Exception {
		// check the number of parameters and define appropriate variables to store these parameters
		if (args.length != 4) {
			System.out.println("Invalid number of parameters. Must have four parameters.");
			System.exit(1);
		}

		String netEmulAddr = args[0];
		int portNumEmulRecv = Integer.parseInt(args[1]);
		int portNumReceiverRecv = Integer.parseInt(args[2]);
		String fileName = args[3];

		// use buffered writer write the required output file and log for packet data and arrival
		BufferedWriter outputWriter = new BufferedWriter(new FileWriter(fileName));
		BufferedWriter arvLogWriter = new BufferedWriter(new FileWriter("arrival.log"));

		// count the expected sequence number and use it to send ack sequence number
		int expectedSeqNum = 0;
		// store the seuqnce number of the packet received
		int receivedSeqNum = 0;
		// indicate when the EOT packet is received
		boolean isEOTReceived = false;

		// socket to communicate with the emulator
		DatagramSocket socketReceiverRecv = new DatagramSocket(portNumReceiverRecv);

		while(true) {
			packet packetRecv = udtRecv(socketReceiverRecv);

			receivedSeqNum = packetRecv.getSeqNum();

			arvLogWriter.write(Integer.toString(receivedSeqNum));
			arvLogWriter.newLine();

			// if the packet received is the EOT, indicate EOT is received
			if (packetRecv.getType() == 2) {
				isEOTReceived = true;
			}

			// if the expected/correct sequence number is encountered, send back the packet with appropriate ack
			if (receivedSeqNum == expectedSeqNum) {
				// if the EOT packet is recevied, send back the EOT packet and break the loop
				if (isEOTReceived) {
					packet packetToSend = packet.createEOT(expectedSeqNum);
					udtSend(packetToSend, netEmulAddr, portNumEmulRecv, socketReceiverRecv);
					// System.out.println("Safe Exit by EOT");
					break;
				}
				// otherwise, extract the data into output file 
				// and send back the packet with appropriate ack sequence number
				else {
					String strDataRecv = new String(packetRecv.getData());
					outputWriter.write(strDataRecv);
					packet packetToSend = packet.createACK(expectedSeqNum);
					udtSend(packetToSend, netEmulAddr, portNumEmulRecv, socketReceiverRecv);
					expectedSeqNum = (expectedSeqNum + 1) % seqNumModulo;
				}
			}
			// if incorrect sequence number received, send back the packet with the last acknowledged ack sequnence number
			else {
				int lastACKSeqNum = 0;
				if (expectedSeqNum == 0) {
					lastACKSeqNum = expectedSeqNum - 1 + seqNumModulo;
				}
				else {
					lastACKSeqNum = expectedSeqNum - 1;
				}
				packet packetToSend = packet.createACK(lastACKSeqNum);
				udtSend(packetToSend, netEmulAddr, portNumEmulRecv, socketReceiverRecv);
			}
		}

		// close
		socketReceiverRecv.close();
		outputWriter.close();
		arvLogWriter.close();
	}
}