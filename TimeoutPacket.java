package cs451;

public class TimeoutPacket{

    private final Packet packet;

    private long timeout;

    private final boolean isProposal;
	
	private long timestamp;

    public TimeoutPacket(long timeout, Packet packet, boolean isProposal) {
        this.packet = packet;
        this.timeout = timeout;
        this.isProposal = isProposal;
        this.packet.updateTimestamp();
        this.timestamp = this.packet.getTimestamp();
    }

    public void updateTimeout(long timeout) {
        packet.updateTimestamp();
        timestamp = packet.getTimestamp();
        this.timeout = timeout;
    }

    public Packet getPacket() {
        return packet;
    }
	
	public long getTimeout(){
        return timeout;
    }

    public boolean isProposal(){
        return isProposal;
    }
	
    public boolean isExpired() {
        return timeout < (System.currentTimeMillis() - timestamp);
    }
}
