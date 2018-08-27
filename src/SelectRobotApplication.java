import java.util.Objects;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;


public class SelectRobotApplication {
    private static Scanner inputSelect = new Scanner(System.in);

    public static DatagramSocket mySocket;
    public static boolean running;

    public static void main(String[] args){
        /** UPD-Verbindungsaufbau zum Triggern des Programmes **/
        mySocket = null;
        try{
            mySocket = new DatagramSocket(30333);
            //robot address (please adjust IP!)
            InetSocketAddress robotAddress = new InetSocketAddress("172.31.1.147", 30300);

            // get robot state
            byte[] msg = String.format("%d;0;Get_State;true", System.currentTimeMillis()).getBytes("UTF-8");

            mySocket.send(new DatagramPacket(msg, msg.length, robotAddress));

            // receive answer state message
            byte[] receiveData = new byte[508];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            mySocket.receive(receivePacket);

            // extract counter
            String[] stateMessage = new String(receivePacket.getData()).split(";");
            long counter = Long.parseLong(stateMessage[2]);

            // start application by sending a rising edge
            // (false->true) for App_Start
            msg = String.format("%d;%d;App_Start;false", System.currentTimeMillis(), ++counter).getBytes("UTF-8");
            mySocket.send(new DatagramPacket(msg, msg.length, robotAddress));
            msg = String.format("%d;%d;App_Start;true", System.currentTimeMillis(), ++counter).getBytes("UTF-8");
            mySocket.send(new DatagramPacket(msg, msg.length, robotAddress));
        }catch(IOException e1){
            e1.printStackTrace();
        }

        try{
            Thread.sleep(6000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }


        /** TCP-Verbindungsaufbau zum Anwählen der Roboter Application **/
        String sentence;
        String modifiedSentence;

        //BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));



        try(Socket clientSocket = new Socket("172.31.1.147", 30005)){
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            /** Schleife zum Auswählen der Roboterprogramme **/
            boolean active = true;
            running = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    InetSocketAddress robotAddress = new InetSocketAddress("172.31.1.147", 30300);
                    String oldstate = "old";
                    while(running) {

                        byte[] receiveData = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        try {
                            //System.out.println("waiting for udp");
                            mySocket.receive(receivePacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        String[] stateMessage = new String(receivePacket.getData()).split(";");
                        System.out.println("received state is: " + stateMessage[8]);

//                        for(String s : stateMessage){
//                            System.out.println(s);
//                        }

                        System.out.println("new state: " + stateMessage[8] + "  old state: " + oldstate + "\n");

                        if(stateMessage[8].equals("MOTIONPAUSED") && !oldstate.equals("MOTIONPAUSED")){
                            System.out.println("MOTIONPAUSED Handling:\n");
                            System.out.println("Continue? ([y]es )\n" );
                            String sentence = inputSelect.nextLine();

                            if(sentence.equals("y")){
                                // extract counter
                                long counter = Long.parseLong(stateMessage[2]);
                                try{
                                    // start application by sending a rising edge
                                    // (false->true) for App_Start
                                    byte[] msg = String.format("%d;%d;App_Start;false", System.currentTimeMillis(), ++counter).getBytes("UTF-8");
                                    mySocket.send(new DatagramPacket(msg, msg.length, robotAddress));
                                    msg = String.format("%d;%d;App_Start;true", System.currentTimeMillis(), ++counter).getBytes("UTF-8");
                                    mySocket.send(new DatagramPacket(msg, msg.length, robotAddress));
                                } catch(IOException e){
                                    e.printStackTrace();
                                }
                            }

                        }
                        oldstate = stateMessage[8];
                    }
                }
            }).start();


            while(active) {

                System.out.println("Select Roboter Application: ");
                System.out.println("1: test1: ");
                System.out.println("2: test2: ");
                System.out.println("exit: Exit Programm \n");

                sentence = inputSelect.nextLine();

                if(sentence.equals("exit")) {
                    outToServer.writeBytes("0" + "\n");
                    running = false;
                    active = false;
                }else {
                    outToServer.writeBytes(sentence + "\n");
                    modifiedSentence = inFromServer.readLine();

                    System.out.println(modifiedSentence);
                }
            }

        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
