package cs451;

import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FifoBroadcast {

    static private class FifoProcess{
        private final Map<Integer, Packet> packets = new ConcurrentHashMap<>();
        public int packetNumber = 1;
        
        FifoProcess(){};

        public boolean hasNextPacket(){
            return packets.containsKey(packetNumber);
        }

        public Packet getNextPacket(){
            Packet tmp = packets.remove(packetNumber);
            packetNumber++;
            return tmp;
        }

        public void addPacket(Packet packet){
            packets.put(packet.getBroadcastId(), packet);
        }


    }

    private static final Map<Byte, FifoProcess> fifoProcesses = new HashMap<>();

    public static void create() throws SocketException{
        for(byte id : NetworkInterface.network.keySet()) fifoProcesses.put(id, new FifoProcess());
        UniformReliableBroadcast.create();
        ExecutorService worker = Executors.newFixedThreadPool(1);
        worker.execute(FifoBroadcast::dequeueToPrint);
    }

    public static void fifoDeliver(Packet packet){
        fifoProcesses.get(packet.getProducerId()).addPacket(packet);
    }

    public static void dequeueToPrint(){
        FifoProcess tmp;
        while (NetworkInterface.running.get()) {
            for(Entry<Byte,FifoProcess> e: fifoProcesses.entrySet()){
                tmp = e.getValue();
                if(tmp.hasNextPacket()){
                    NetworkInterface.printOutput(tmp.getNextPacket().processPacket(), e.getKey());
                }
            }
        }
    }

    public static void fifoBroadcast(List<Integer> inputData, byte numberInput, byte producerId){
        UniformReliableBroadcast.urbBroadcast(inputData, numberInput, producerId);
    }
}
