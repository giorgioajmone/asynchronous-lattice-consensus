package cs451;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import cs451.parser.Host;


public class FairLossLink {

    private static DatagramSocket socket;

    public static final BlockingQueue<DatagramPacket> datagramsToSend = new LinkedBlockingQueue<>();

    public static void create() throws SocketException {
        socket = new DatagramSocket(NetworkInterface.network.get(NetworkInterface.HOST_ID).getPort());
        ExecutorService workers = Executors.newFixedThreadPool(2);
        workers.execute(FairLossLink::sendPackets);
        workers.execute(FairLossLink::receivePackets);
    }

    public static void receivePackets() {
        while (NetworkInterface.running.get()) {
            try {
                DatagramPacket datagramPacket  = new DatagramPacket(new byte[Packet.MAX_PACKET_SIZE], Packet.MAX_PACKET_SIZE);
                socket.receive(datagramPacket);
                StubbornLink.stubbornDeliver(new Packet(datagramPacket.getData()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } 
    
    public static void addPacket(Packet packet, Byte targetID) {
        Host targetHost = NetworkInterface.network.get(targetID);
        InetSocketAddress targetAddress = new InetSocketAddress(targetHost.getIp(), targetHost.getPort());
        byte[] buffer = packet.getBytes();
        try {
            datagramsToSend.put(new DatagramPacket(Arrays.copyOf(buffer, buffer.length), buffer.length, targetAddress));
        } catch (InterruptedException e) {
            //e.printStackTrace();
            Thread.currentThread().interrupt();
        } 
    }

    public static void sendPackets() {
        while (NetworkInterface.running.get()) {
            try {
                socket.send(datagramsToSend.take());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                //e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }
}
