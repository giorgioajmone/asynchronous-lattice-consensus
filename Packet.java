package cs451;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Packet {

    private static final int MASK = 0Xff;

    private static final long LONG_MASK = 0xff;
	
	public static final int
                     PACKET_ID = 0,
                     IS_ACK = Long.BYTES + PACKET_ID,
                     SENDER_ID = 1 + IS_ACK,
                     PRODUCER_ID = 1 + SENDER_ID,
                     BROADCAST_ID = 1 + PRODUCER_ID,
                     TIMESTAMP = Integer.BYTES + BROADCAST_ID,
                     SIZE_VALUES = Long.BYTES + TIMESTAMP,
                     HEADER = 1 + SIZE_VALUES;

    public static int MAX_PACKET_SIZE;

    private byte[] data;

    private final long packetId;

    private final byte senderId;

    private final byte producerId;

    private final int broadcastId;

    private long timestamp;

    private final byte numberOfValues;

    private final boolean isAck;
	
	public static void IntegerToByteOffSet(int value, byte[] array, int offset) {
        array[offset] = (byte) (value >>> 24);
        array[offset + 1] = (byte) (value >>> 16);
        array[offset + 2] = (byte) (value >>> 8);
        array[offset + 3] = (byte)  value;
    }

    public static int ByteToIntegerOffSet(byte[] bytes, int offset) {
        return  ((bytes[offset] & MASK) << 24) |
                ((bytes[offset+1] & MASK) << 16) |
                ((bytes[offset+2] & MASK) << 8 ) |
                ((bytes[offset+3] & MASK));
    }

    public static void LongToByteOffSet(long value, byte[] array, int offset) {
        array[offset] = (byte) (value >>> 56);
        array[offset + 1] = (byte) (value >>> 48);
        array[offset + 2] = (byte) (value >>> 40);
        array[offset + 3] = (byte) (value >>> 32);
        array[offset + 4] = (byte) (value >>> 24);
        array[offset + 5] = (byte) (value >>> 16);
        array[offset + 6] = (byte) (value >>> 8);
        array[offset + 7] = (byte)  value;
    }

    public static long ByteToLongOffSet(byte[] bytes, int offset) {
        return  ((bytes[offset] & LONG_MASK) << 56) |
                ((bytes[offset+1] & LONG_MASK) << 48) |
                ((bytes[offset+2] & LONG_MASK) << 40) |
                ((bytes[offset+3] & LONG_MASK) << 32) |
                ((bytes[offset+4] & LONG_MASK) << 24) |
                ((bytes[offset+5] & LONG_MASK) << 16) |
                ((bytes[offset+6] & LONG_MASK) << 8 ) |
                ((bytes[offset+7] & LONG_MASK));
    }
	
	public Packet(List<Integer> inputData, long packetId, byte senderId, byte producerId, int broadcastId, byte numberOfValues){

        this.data = new byte[HEADER+inputData.size()*4];
        this.packetId = packetId;
        this.senderId = senderId;
        this.producerId = producerId;
        this.broadcastId = broadcastId;
        this.isAck = false;
        this.timestamp = System.currentTimeMillis();
        this.numberOfValues = numberOfValues;

        LongToByteOffSet(packetId, this.data, PACKET_ID);
        this.data[IS_ACK] = 0;
        this.data[SENDER_ID] = this.senderId;
        this.data[PRODUCER_ID] = this.producerId;
        IntegerToByteOffSet(this.broadcastId, this.data, BROADCAST_ID);
        LongToByteOffSet(this.timestamp, this.data, TIMESTAMP);
        this.data[SIZE_VALUES] = this.numberOfValues;

        int index = 0;
        for(Integer value: inputData){
            IntegerToByteOffSet(value.intValue(), this.data, HEADER + index);
            index += Integer.BYTES;
        }
    }

	public List<Integer> processPacket(){
        List<Integer> output = new ArrayList<>();
        for(int i = 0; i < this.numberOfValues; i++){
            output.add(ByteToIntegerOffSet(this.data, HEADER+i*Integer.BYTES));
        }
        data = new byte[0];
        return output;
    }
	
	public Packet createAck() {
        byte[] newData = Arrays.copyOf(data, HEADER);
        newData[IS_ACK] = 1;
        newData[SENDER_ID] = NetworkInterface.HOST_ID;
        return new Packet(newData, packetId, NetworkInterface.HOST_ID, producerId, broadcastId, true, timestamp, numberOfValues);
    }

    private Packet(byte[] data, long packetId, byte senderId, byte producerId, int broadcastId, boolean isAck, long timestamp, byte numberOfValues) {
        this.data = data;
        this.packetId = packetId;
        this.senderId = senderId;
        this.producerId = producerId;
        this.broadcastId = broadcastId;
        this.isAck = isAck;
        this.timestamp = timestamp;
        this.numberOfValues = numberOfValues;    
    }
	
	public Packet(byte[] data) {
        this.data = data;
        this.packetId = ByteToLongOffSet(data, PACKET_ID);
        this.senderId = data[SENDER_ID];
        this.producerId = data[PRODUCER_ID];
        this.broadcastId = ByteToIntegerOffSet(data, BROADCAST_ID);
        this.isAck = data[IS_ACK] == 1;
        this.timestamp = ByteToLongOffSet(data, TIMESTAMP);
        this.numberOfValues = data[SIZE_VALUES];
        
    }

    public Packet(byte[] data, long packetId, byte senderId, byte producerId, int broadcastId, byte numberOfValues) {
        this.data = Arrays.copyOf(data, data.length);
        this.packetId = packetId;
        this.senderId = senderId;
        this.producerId = producerId;
        this.broadcastId = broadcastId;
        this.isAck = false;
        this.timestamp = System.currentTimeMillis();
        this.numberOfValues = numberOfValues;

        LongToByteOffSet(packetId, this.data, PACKET_ID);
        this.data[IS_ACK] = 0;
        this.data[SENDER_ID] = this.senderId;
        this.data[PRODUCER_ID] = this.producerId;
        IntegerToByteOffSet(this.broadcastId, this.data, BROADCAST_ID);
        LongToByteOffSet(this.timestamp, this.data, TIMESTAMP);
        this.data[SIZE_VALUES] = this.numberOfValues;
    }

    public Packet(List<Proposal> proposals, long packetId, byte senderId, byte producerId, int broadcastId, int len) {
        this.data = new byte[len];
        this.packetId = packetId;
        this.senderId = senderId;
        this.producerId = producerId;
        this.broadcastId = broadcastId;
        this.isAck = false;
        this.timestamp = System.currentTimeMillis();
        this.numberOfValues = (byte)proposals.size();

        LongToByteOffSet(packetId, this.data, PACKET_ID);
        this.data[IS_ACK] = 0;
        this.data[SENDER_ID] = this.senderId;
        this.data[PRODUCER_ID] = this.producerId;
        IntegerToByteOffSet(this.broadcastId, this.data, BROADCAST_ID);
        LongToByteOffSet(this.timestamp, this.data, TIMESTAMP);
        this.data[SIZE_VALUES] = this.numberOfValues;

        int ptr = HEADER;
        for (Proposal proposal : proposals) {
            IntegerToByteOffSet(proposal.proposalNumber, this.data, ptr);
            ptr += Integer.BYTES;
            IntegerToByteOffSet(proposal.activeProposalNumber, this.data, ptr);
            ptr += Integer.BYTES;
            this.data[ptr++] = proposal.type;
            if (proposal.type != 1 && proposal.type != 3) {
                IntegerToByteOffSet(proposal.proposedValues.size(), this.data, ptr);
                ptr += Integer.BYTES;
                for (int x : proposal.proposedValues) {
                    IntegerToByteOffSet(x, this.data, ptr);
                    ptr += Integer.BYTES;
                }
            }
        }
    }

    public byte[] getBytes() {
        return data;
    }

    public byte getNumberValues(){
        return numberOfValues;
    }


    public long getPacketId() {
        return packetId;
    }

    public int getBroadcastId(){
        return broadcastId;
    }

	public int getFirst(){
        return ByteToIntegerOffSet(data, HEADER);
    }

	public void cleanMemory(){
        this.data = new byte[0];
    }

    public byte getSenderId() {
        return senderId;
    }

    public boolean isAck() {
        return isAck;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte getProducerId(){
        return producerId;
    }

    public long getRoundTripTime() {
        return System.currentTimeMillis() - timestamp;
    }
	
	public void updateTimestamp() {
        this.timestamp = System.currentTimeMillis();
        LongToByteOffSet(timestamp, data, TIMESTAMP);
    }
}
