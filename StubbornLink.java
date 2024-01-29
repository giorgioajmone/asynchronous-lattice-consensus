package cs451;

import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


public class StubbornLink {

    static public class StubbornProcess {

        private final AtomicLong timeout = new AtomicLong(1024);

        public final Set<Long> packetsAcked  = ConcurrentHashMap.newKeySet();

        public final Queue<TimeoutPacket> unAcked = new LinkedList<>();

        private int notAckedWindow = 100;

        public final ConcurrentLinkedDeque<Proposal> acksToSend = new ConcurrentLinkedDeque<>();

        public final ConcurrentLinkedDeque<Proposal> nacksToSend = new ConcurrentLinkedDeque<>();

        public StubbornProcess(){};

        public long getTimeout() {
            return timeout.get();
        }

        public void backOff(long packetTimeout) {
            long initial = timeout.get();
            if (packetTimeout < initial || initial > 16384) {
                return;
            }
            long newTimeout = 2 * initial + 10;
            timeout.set(newTimeout);   
        }
        
        public void updateReceived(long lastTime) {
            timeout.set(lastTime);
        }    

        public boolean containsAcked(Long packetId){
            return packetsAcked.contains(packetId);
        }

        public void addPacketToAcked(Long packetId){
            packetsAcked.add(packetId);
        }

        public Packet createAckPacket(){
            int length = Packet.HEADER;
            List<Proposal> list = new LinkedList<>();
            Proposal tmp;
            while (list.size() < 8 && (tmp = acksToSend.peek()) != null) {
                int bytes = tmp.getBytesLength();
                if (Packet.MAX_PACKET_SIZE - length >= bytes) {
                    length += bytes;
                    acksToSend.remove();
                    list.add(tmp);
                }
            }
            if (!list.isEmpty()) {
                return new Packet(list, packetNumber++, NetworkInterface.HOST_ID, NetworkInterface.HOST_ID, 0, length);
            }
            return null;
        }

        public Packet createNAckPacket(){
            int length = Packet.HEADER;
            List<Proposal> list = new LinkedList<>();
            Proposal tmp;
            while (list.size() < 8 && (tmp = nacksToSend.peek()) != null) {
                int bytes = tmp.getBytesLength();
                if (Packet.MAX_PACKET_SIZE - length >= bytes) {
                    length += bytes;
                    nacksToSend.remove();
                    list.add(tmp);
                }
            }
            if (!list.isEmpty()) {
                return new Packet(list, packetNumber++, NetworkInterface.HOST_ID, NetworkInterface.HOST_ID, 0, length);
            }
            return null;
        }
    }
    
    public static long packetNumber = 1;
    public static int ackNumber = -1;

    public static final Map<Byte, StubbornProcess> stubbornProcesses = new HashMap<>();

    public static final ConcurrentLinkedDeque<Proposal> proposalsToSend = new ConcurrentLinkedDeque<>();

    public static void create() throws SocketException {
        for(byte id : NetworkInterface.network.keySet()) stubbornProcesses.put(id, new StubbornProcess());
        FairLossLink.create();
        ExecutorService worker = Executors.newFixedThreadPool(1);
        worker.execute(StubbornLink::sendPackets);
    }

    public static void stubbornDeliver(Packet packet) {
        if (packet.isAck()) {
            StubbornProcess tmp = stubbornProcesses.get(packet.getSenderId());
            tmp.updateReceived(packet.getRoundTripTime());
            tmp.addPacketToAcked(packet.getPacketId());
        } else {
            FairLossLink.addPacket(packet.createAck(), packet.getSenderId());
            PerfectLink.perfectDeliver(packet);
        }
    }

    private static void sendPackets() {
        while (NetworkInterface.running.get()) {
            boolean sending = false;
            Packet packetToSend = null;
            if(resendPackets()){
                packetToSend = createPacket();
                sending = true;
            }
            for(Entry<Byte,StubbornProcess> e: stubbornProcesses.entrySet()){
                StubbornProcess tmp = e.getValue();
                if(e.getKey() != NetworkInterface.HOST_ID){
                    if(packetToSend != null) {
                        tmp.unAcked.add(new TimeoutPacket(tmp.getTimeout(), packetToSend, true));
                        tmp.notAckedWindow--;
                        FairLossLink.addPacket(packetToSend, e.getKey());
                    }
                    Packet packet;

                    packet = tmp.createAckPacket();
                    if(packet != null) {
                        tmp.unAcked.add(new TimeoutPacket(tmp.getTimeout(), packet, false));
                        FairLossLink.addPacket(packet, e.getKey());
                    }

                    if(sending){
                        packet = tmp.createNAckPacket();
                        if(packet != null) {
                            tmp.unAcked.add(new TimeoutPacket(tmp.getTimeout(), packet, true));
                            tmp.notAckedWindow--;
                            FairLossLink.addPacket(packet, e.getKey());
                        }
                    }
                }
            }
        }
    }

    private static boolean resendPackets() {
        int workingProcesses = 0;
        for(Entry<Byte,StubbornProcess> e: stubbornProcesses.entrySet()){
            TimeoutPacket timeoutPacket;
            StubbornProcess tmp = e.getValue();
            if((timeoutPacket = tmp.unAcked.poll()) != null){
                if (!tmp.containsAcked(timeoutPacket.getPacket().getPacketId())) {
                    if (timeoutPacket.isExpired()) {
                        tmp.backOff(timeoutPacket.getTimeout());
                        timeoutPacket.updateTimeout(tmp.getTimeout());
                        FairLossLink.addPacket(timeoutPacket.getPacket(), e.getKey());
                    }
                    tmp.unAcked.add(timeoutPacket);
                }
                else if(timeoutPacket.isProposal()){
                    tmp.notAckedWindow++;
                }
            }
            if(tmp.notAckedWindow > 0){
                workingProcesses++;
            }
        }
        return workingProcesses >= NetworkInterface.network.size()/2; 
    }

    public static Packet createPacket(){
        int length = Packet.HEADER;
        List<Proposal> list = new LinkedList<>();
        Proposal tmp;
        while (list.size() < 8 && (tmp = proposalsToSend.poll()) != null) {
            int bytes = tmp.getBytesLength();
            if (Packet.MAX_PACKET_SIZE - length >= bytes) {
                length += bytes;
                list.add(tmp);
            }
            else{
                proposalsToSend.addFirst(tmp);
            }
        }
        if (!list.isEmpty()) {
            return new Packet(list, packetNumber++, NetworkInterface.HOST_ID, NetworkInterface.HOST_ID, 0, length);
        }
        return null;
    }
}
