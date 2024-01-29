package cs451;

import java.net.SocketException;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PerfectLink {

    static public class PerfectProcess{

        public final Set<Long> packetsDelivered = new HashSet<>();

        PerfectProcess(){};

        public boolean addPacketToDelivered(Long packetId){
            return packetsDelivered.add(packetId);
        }
    }

    public static final Map<Byte, PerfectProcess> perfectProcesses = new HashMap<>(); 
    
    public static void create() throws SocketException {
        for(byte id : NetworkInterface.network.keySet()) perfectProcesses.put(id, new PerfectProcess());
        StubbornLink.create();
    }

    public static void perfectSend(Proposal proposal, boolean priority){
        if(priority){
            StubbornLink.proposalsToSend.addFirst(proposal);
        }
        else{
            StubbornLink.proposalsToSend.add(proposal);
        }   
    }

    public static void perfectSend(Proposal proposal, byte receiverId, boolean priority){
        if(priority){
            StubbornLink.stubbornProcesses.get(receiverId).acksToSend.add(proposal);
        }
        else{
            StubbornLink.stubbornProcesses.get(receiverId).nacksToSend.add(proposal);
        }
    }

    public static void perfectDeliver(Packet packet) {
        if (perfectProcesses.get(packet.getSenderId()).addPacketToDelivered(packet.getPacketId())) {
            BestEffortBroadcast.bebDeliver(packet);
        }
    }
}
