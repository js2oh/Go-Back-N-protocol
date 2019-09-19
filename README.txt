CS456 Assignment2 README file

Name: Jun Sung Oh
Student ID: 20596770

Instructions to Run the Programs:
-make to compile the required java classes into executables
-To run nEmulator-linux386: ./nEmulator <emul_forward> <recv_addr> <recv_port> <emul_backward> <send_addr> <send_port> <max_delay> <probability> <mode>
-To run receiver: java receiver <emul_addr> <emul_backward> <recv_port> <output>
-To run sender: java sender <emul_addr> <emul_forward> <send_port> <input>
-Make sure to run the emulator 1st, receiver 2nd, and sender last in order!!!

Parameters used:
	-Emulator:
		<emul_forward> -emulator's receiving UDP port number in the forward (sender) direction
		<recv_addr> -receiver’s network address
		<recv_port> -receiver’s receiving UDP port number
		<emul_backward> -emulator's receiving UDP port number in the backward (receiver) direction
		<send_addr> -sender’s network address
		<send_port> -sender’s receiving UDP port number
		<max_delay> -maximum delay of the link in units of millisecond
		<probability> -packet discard probability
		<mode> -verbose-mode; Set to 1, the network emulator will output its internal processing
	-Receiver:
		<emul_addr> -hostname for the network emulator
		<emul_backward> -UDP port number used by the link emulator to receive ACKs from the						 receiver
		<recv_port> -UDP port number used by the receiver to receive data from the emulator
		<output> -name of the file into which the received data is written
	-Sender:
		<emul_addr> -host address of the network emulator
		<emul_forward> -UDP port number used by the emulator to receive data from the sender
		<send_port> -UDP port number used by the sender to receive ACKs from the emulator
		<input> -name of the file to be transferred

Undergrad Machines Tested on:
-Emulator: ubuntu1604-002
-Receiver: ubuntu1604-006
-Sender: ubuntu1604-008

Compilation:
	-make: make version GNU Make 4.1
	-javac: compiler version javac 1.8.0_191

