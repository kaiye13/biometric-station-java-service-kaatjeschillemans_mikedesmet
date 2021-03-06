
package javaservicebiometricstation.SerialPort;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.util.Arrays;

/**
 *
 * @author kaatje
 */
public class SerialLineReciever  implements SerialPortDataListener{
    private com.fazecast.jSerialComm.SerialPort comPort;
    private final static int MAX_BYTES = 256;
    private byte[] dataBuffer = new byte[MAX_BYTES];
    private int iNextData = 0;
    private byte delimiter = '\n';
    private boolean enableDebugMessages = false;
    
    private SerialPortListener listener = null;
    
    public SerialLineReciever() {
        this(0, 115200, false);
    }
    
    public SerialLineReciever(int comIndex, int baudrate, boolean enableDebugging) {
        this.enableDebugMessages = enableDebugging;
        comPort = com.fazecast.jSerialComm.SerialPort.getCommPorts()[comIndex];
        if (comPort.openPort()) {
            if (enableDebugMessages) {
                System.out.println("Opening port with baudrate of " + baudrate + " baud");
            }
            setBaudRate(baudrate);
            comPort.addDataListener(this);
        } else {
            System.out.println("Could not open port. Is another program using it?");
        }
    }
    
    public void setBaudRate(int baudrate) {
        comPort.setBaudRate(baudrate);
    }
    
    public void setDelimiter(byte character) {
        delimiter = character;
    }
    
    public void enableDebugging() {
        this.enableDebugMessages = true;
    }
    
    public void close() {
        comPort.closePort();
    }
    
    public void setLineListener(SerialPortListener listener) {
        this.listener = listener;
    }
    
    @Override
    public int getListeningEvents() {
        return com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.getEventType() != com.fazecast.jSerialComm.SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
            return;
        }

        byte[] newData = new byte[comPort.bytesAvailable()];
        int numRead = comPort.readBytes(newData, newData.length);
        if (enableDebugMessages) {
            System.out.println("Read " + numRead + " bytes");
        }
        processReceivedData(newData, numRead);
    }
    
    private void processReceivedData(byte[] data, int numberOfBytes){
        for (int i = 0; i < numberOfBytes; i++) {
            if (iNextData < MAX_BYTES) {
                if ((char)data[i] == delimiter) {
                    if (enableDebugMessages) {
                        System.out.println("Found newline, sending buffer");
                    }
                    sendDataToListener();
                    iNextData = 0;
                } else {
                    dataBuffer[iNextData++] = data[i];
                }
            } else {
                if (enableDebugMessages) {
                    System.out.println("Buffer full, sending result without newline");
                }
                sendDataToListener();
                iNextData = 0;
            }
        }
    }
    
    private void sendDataToListener() {
        if (listener != null) {
            if (enableDebugMessages) {
                System.out.println("Data = " + new String(Arrays.copyOf(dataBuffer, iNextData)));
            }
            listener.serialLineEvent(new SerialData(Arrays.copyOf(dataBuffer, iNextData)));
        } else if (enableDebugMessages) {
            System.out.println("No listener registered. Cannot deliver data.");
        }
}
}
