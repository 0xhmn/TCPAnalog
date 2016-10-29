import org.junit.Test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Stack;
import java.util.UUID;


public class Client {

    /**
     *
     * @param args: args[0]: host - args[1]: port - arg[2]: filename
     */
    public static void main(String[] args) throws Exception {

        if (args.length != 3) {
            System.out.println("> need to pass three args for\n\t- host\n\t- port\n\t- filename");
            return;
        }

        String host = args[0]; // Remote hostname. It can be changed to anything you desire.
        int port = Integer.parseInt(args[1]); // Port number.
        String filename = args[2];
        String data; // Store the server's feedback.

        Socket cSock = null;
        DataOutputStream sendOut = null;
        BufferedReader readFrom = null;

        try {
            cSock = new Socket(host, port); // Initialize the socket.
            sendOut = new DataOutputStream(cSock.getOutputStream()); // The output stream to server.
            readFrom = new BufferedReader(new InputStreamReader(cSock.getInputStream())); // The input stream from server.
        } catch (Exception e) {
            System.out.println("Error: cannot open socket");
            System.exit(1); // Handle exceptions.
        }


		/* Generating a Stack of msgs from input file */
        Stack<String> msgStack = stackGenerator(filename);

        while (!msgStack.isEmpty()) {
            String msg = msgStack.peek();
            sendOut.writeBytes(msg + "\n");

            cSock.setSoTimeout(5000); // Set the time out in milliseconds.

            try {
                data = readFrom.readLine();

                if (data.equals("END")) {
                    System.out.println("---------------------------");
                    System.out.println("SERVER GOT ALL THE MESSAGES");
                    System.out.println("---------------------------");
                    return;
                }

                if (data.length() > 0 && data.length() < 512) {
                    System.out.println("Got a corrupt ACK from Server - len: " + data.length());
                }


                // check if data is what we want
                for (String s : msgStack) {
                    if (s.equals(data)) {
                        System.out.println("Got a Valid Message by Server: " + data.substring(0, 5));
                        System.out.println("\t send back the acknowledgement for quid: " + data.substring(0, 5));
                        // now send back the guid as acknowledgement
                        String guid = data.substring(0, 36);
                        // send the guid back as acknowledgement
                        sendOut.writeBytes(guid + "\n");

                        // don't remove the object, but send it to the bottom of stack
                        String tmp = msgStack.pop();
                        Stack<String> tmpStack = new Stack<>();
                        tmpStack.addAll(msgStack);
                        msgStack.clear();
                        msgStack.push(tmp);
                        for(String tmpString : tmpStack) {
                            msgStack.push(tmpString);
                        }
                        break;
                    }
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Timeout! Retransmitting...");
            }
        }

        System.out.println("Message Stack is empty");
    }

    /**
     * Helper method
     * creates padding of length msgSize and value of msgChar by repeating msgChar
     */
    private static String createDataSize(int msgSize, char msgChar) {
        StringBuilder sb = new StringBuilder(msgSize);
        for (int i = 0; i < msgSize; i++) {
            sb.append(msgChar);
        }
        return sb.toString();
    }

    /**
     * Helper method
     * generates a unique, 36-char guid as our object signature
     */
    private static String guidGenerator() {
        UUID uuid = UUID.randomUUID();
        return  uuid.toString();
    }

    /**
     * Generates a Stack of padded Strings
     * padding : [32char of guid] + [3 digit of packet size] + [473 char of data]
     * @param path: path of file - passed as an argument
     */
    private static Stack<String> stackGenerator(String path) {
        Stack<String> stack = new Stack<>();
        String counterString;
        int counter = 1;
        String guid;
        String str = null;
        String tmp = null;


        File file = new File(path);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fileInputStream.read(data);
            fileInputStream.close();
            str = new String(data, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (str == null) {
            return null;
        }
        else {
            // replace all \n (because of networkLayer)
            str = str.replaceAll("\n", "");
        }

        counter = (int) Math.ceil((double)str.length() / 473.0);
        counterString = String.format("%03d", counter);

        // split the file
        while (str.length() >= 473) {
            tmp = str.substring(0, 473);
            guid = guidGenerator();

            stack.push(guid + counterString + tmp);

            str = str.substring(473);
        }

        // fill str with leading chars
        if (str.length() > 0) {
            int fillSize = 473 - str.length();
            guid = guidGenerator();
            str = str + createDataSize(fillSize, 'a');
            stack.push(guid + counterString + str);
        }

        return stack;
    }

    @Test
    public void test() {
        Stack<String> st = stackGenerator("test.txt");
        for (String s : st) {
            System.out.println(s.length());
        }
    }

    @Test
    public void test2() {
        Stack<String> st = new Stack<>();
        st.push("aa");
        st.push("bb");
        st.push("cc");
        st.push("dd");

        String tmp = st.pop();

        Stack<String> tmpStack = new Stack<>();
        tmpStack.addAll(st);
        st.clear();
        st.push(tmp);
        for(String tmpString : tmpStack) {
            st.push(tmpString);
        }

        tmp = st.peek();
        System.out.println("done");
    }
}