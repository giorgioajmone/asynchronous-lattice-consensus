package cs451;

import java.util.HashSet;
import java.util.Set;

public class Proposal implements Comparable<Proposal> {

    public static final int ACK_COUNT = 1 + 2 * Integer.BYTES;

    public final int proposalNumber;

    public int activeProposalNumber;

    public final byte type;

    public final byte sender;

    public Set<Integer> proposedValues;

    public int getBytesLength(){
        if (type == 1 || type == 3) {
            return ACK_COUNT;
        } else {
            return ACK_COUNT + (1 + proposedValues.size()) * Integer.BYTES;
        }
    }

    @Override
    public int compareTo(Proposal b){
        return Integer.valueOf(this.proposalNumber).compareTo(b.proposalNumber);
    }
    
    public Proposal(int proposalNumber, byte type, byte sender, Set<Integer> proposedValues, int activeProposalNumber) {
        this.proposalNumber = proposalNumber;
        this.type = type;
        this.sender = sender;
        this.proposedValues = proposedValues;
        this.activeProposalNumber = activeProposalNumber;
    }

    public static Proposal createProposal(int proposalNumber, byte type, byte sender, Set<Integer> values, int activeProposalNumber) {
        return new Proposal(proposalNumber, type, sender, new HashSet<>(values), activeProposalNumber);
    }

    public Proposal (byte[] data, byte senderId, int offset){
        int ptr = offset;
        this.proposalNumber = Packet.ByteToIntegerOffSet(data, ptr);
        ptr += Integer.BYTES;
        this.activeProposalNumber = Packet.ByteToIntegerOffSet(data, ptr);
        ptr += Integer.BYTES;
        this.type = data[ptr++];
        this.sender = senderId;
        if (type != 1 && type != 3) {
            int numValues = Packet.ByteToIntegerOffSet(data, ptr);
            ptr += Integer.BYTES;
            Set<Integer> proposedValues = new HashSet<>(numValues);
            for (int i = 0; i < numValues; i++) {
                proposedValues.add(Packet.ByteToIntegerOffSet(data, ptr));
                ptr += Integer.BYTES;
            }
            this.proposedValues = proposedValues;
        }
    }

    public Proposal(int proposalNumber) {
        this.sender = NetworkInterface.HOST_ID;
        this.proposalNumber = proposalNumber;
        this.type = 3;
    }
}
