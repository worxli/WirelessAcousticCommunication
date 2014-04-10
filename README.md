WirelessAcousticCommunication (CSE 561 project spring quarter 2014 at UW)
======================
###Fahad Pervaiz (fahadp@cs) & Lukas Bischofberger (lukasbi@cs)

Designing a wireless acoustic communication	channel on an android phone

##Sender
Consists of MainThread (UI) and a worker thread which encodes the data and sends the signal.

*UI Input
*convertData(String message)
**slice message into chunks
**convert to bit array
**for each chunk: e.g. stuff bits, create header and checksum / CRC, add preamble
*modulate(bits, carrier signal, bitspersymbol)
*sendToSpeaker

##Receiver
Consists of a MainThread (UI), a service which listens to acoustic signals and a worker thread which processes the signal and decodes the data.

*Listen to micro and store data in buffer
*Process data and identify preamble
*Decode data back to string
*Display message