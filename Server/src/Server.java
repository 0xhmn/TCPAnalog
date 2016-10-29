import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class Server {

    /**
     *
     * @param args - args[0]: port (optional)
     */
    public static void main(String[] args) throws Exception {
        // UUID regex pattern
        final Pattern pattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        int packetNumber = -1;

        HashMap<String, String> tmpMap = new HashMap<>();
        Set<String> finalMagSet = new HashSet<>();
        int port = 5001;    // default value
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        }

        ServerSocket welcomeSock = null;

        try {
            welcomeSock = new ServerSocket(port);
        } catch (Exception e) {
            System.out.println("Error: cannot open socket");
            System.exit(1); // Handle exceptions.
        }

        System.out.println("Server is listening ...");

        Socket sSock = welcomeSock.accept();
        try{
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(sSock.getInputStream()));
            PrintWriter sendOut = new PrintWriter(sSock.getOutputStream(), true);

            while(true){

                if (packetNumber != -1 && finalMagSet.size() == packetNumber) {
                    sendOut.println("END");
                }

                String data = inFromClient.readLine();

                // minimum requirement to store the msg in tmpMap
                if (data.length() == 512) {
                    // first 36 char is guid
                    String guid = data.substring(0, 36);
                    // check if we have a valid guid (in case of it being mangled)
                    if (!pattern.matcher(guid).matches()) {
                        System.out.println("Guid is mangled by server : " + guid);
                        continue;
                    }
                    // next three char is the number of our packets
                    if (packetNumber < 0) {
                        packetNumber = Integer.parseInt(data.substring(36, 39));
                        System.out.println("Number of Packets: " + packetNumber);
                    }

                    // the rest (473 char) , is our msg
                    String msg = data.substring(39);
                    tmpMap.put(guid, msg);

                    System.out.println("SYNACK is transmitted for guid#: " + guid);
                    sendOut.println(data);
                }
                // potentially a client acknowledgement
                else if (data.length() == 36) {
                    if (tmpMap.get(data) != null) {
                        // means it is an acknowledgement
                        finalMagSet.add(tmpMap.get(data));
                        tmpMap.remove(data);
                        System.out.printf("Msg for GUID# %s has been stored on server successfully!\n", data.substring(0, 5));
                    }
                }
                else {
                    System.out.println("Server has received a corrupted msg! : " + data.length());
                }
            }

        } catch(Exception e){
            // System.out.println("Server is down.");
        }

    }
}