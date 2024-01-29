package cs451;

import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UniformReliableBroadcast{

    static private class UniformProcess{

        private final Map<Integer, Set<Byte>> acknowledgments = new HashMap<>();
        
        UniformProcess(){};

        public boolean addAck(Integer packetId, Byte senderId){
            if(!this.acknowledgments.containsKey(packetId)){
                acknowledgments.put(packetId, new HashSet<>(Arrays.asList(senderId)));
            } else{
                acknowledgments.get(packetId).add(senderId);
            }
            int tmp = acknowledgments.get(packetId).size();
            if(tmp == NetworkInterface.network.size()-1) acknowledgments.remove(packetId);
            return  tmp >= NetworkInterface.network.size()/2;
        }
    }

    private static final Map<Byte, UniformProcess> uniformProcesses = new HashMap<>();

    public static void create() throws SocketException{
        for(byte id : NetworkInterface.network.keySet()) uniformProcesses.put(id, new UniformProcess());
        BestEffortBroadcast.create();
    }

    public static void urbBroadcast(List<Integer> inputData, byte numberInput, byte producerId){
        BestEffortBroadcast.bebBroadcast(inputData, numberInput, producerId);
    }

    public static void urbDeliver(Packet packet){
        UniformProcess tmp = uniformProcesses.get(packet.getProducerId());
        if(!tmp.acknowledgments.containsKey(packet.getBroadcastId()))
            BestEffortBroadcast.bebBroadcast(packet);
        if(tmp.addAck(packet.getBroadcastId(), packet.getSenderId())){
            FifoBroadcast.fifoDeliver(packet);
        }
    }
}