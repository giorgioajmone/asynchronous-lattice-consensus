package cs451;

import java.net.SocketException;

public class BestEffortBroadcast {

    static int broadcastNumber = 1;

    public static void create() throws SocketException{
        PerfectLink.create();
    }

    public static void bebDeliver(Packet packet) {
        LatticeAgreement.latticeDeliver(packet);
    }

    public static void bebBroadcast(Proposal proposal, boolean priority){
        PerfectLink.perfectSend(proposal, priority);
    }
}
