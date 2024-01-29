package cs451;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class LatticeAgreement{

    private static final Map<Integer, Integer> activePerProposal = new HashMap<>();

    private final static Map<Integer, Integer[]> messagesPerProposal = new HashMap<>();

    private static final Map<Integer, Set<Integer>> proposedValue = new HashMap<>();

    private static final Map<Integer, Set<Integer>> acceptedValue = new HashMap<>();

    private static final BlockingQueue <Packet> messagesReceived = new LinkedBlockingQueue<>();

    private static final Map<Integer, Set<Byte>> processesPerProposal = new HashMap<>();

    public static final LinkedList<Proposal> input = new LinkedList<>();

    private static final Map<Integer, Integer> cleanCount = new HashMap<>();

    private static int proposalId = 0;

    private static int finished = 0;

    private static int deliveredProposals = 0;

    public static int NUMBER_PROPOSALS = 0;

    public static int MAX_INPUT = 0;

    public static int MAX_PROPOSAL = 0;

    public static int BATCH_PROPOSALS = 100;

    public static void create(){
        try{
            String[] parameters = NetworkInterface.getConfig().split("\\s");

            NUMBER_PROPOSALS = Integer.valueOf(parameters[0]);
            MAX_INPUT = Integer.valueOf(parameters[1]);
            MAX_PROPOSAL = Integer.valueOf(parameters[2]);

            Packet.MAX_PACKET_SIZE = Packet.HEADER + 8 * (MAX_PROPOSAL + 4) * Integer.BYTES;

            try {
                BestEffortBroadcast.create();
            } catch (SocketException e) {
                e.printStackTrace();
            }
            
            if (readProposals(BATCH_PROPOSALS)) {
                Iterator<Proposal> iterator = input.listIterator();
                while (iterator.hasNext()) {
                    Proposal proposal = iterator.next();
                    iterator.remove();
                    broadcastProposal(proposal);
                }
            }

            while(NetworkInterface.running.get()){
                try{
                    Packet packet = messagesReceived.take();
                    packetToProposal(packet.getBytes(), packet.getNumberValues(), packet.getPacketId());
                }
                catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
            }
        }catch (IOException e) {
            System.err.println("Problem with the config file!");
        }
    }

    public static boolean readProposals(int value) {
        if (proposalId == NUMBER_PROPOSALS) {
            return false;
        }
        try {
            int p = 0;
            for (String line; p < value && (line = NetworkInterface.reader.readLine()) != null; p++) {
                readLine(line);
                if (proposalId == NUMBER_PROPOSALS) {
                    NetworkInterface.reader.close();
                    break;
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
            return !input.isEmpty();
        }
        return true;
    }

    private static void readLine(String line) {
        try {
            String[] splits = line.split("\\s");
            Set<Integer> values = new HashSet<>(MAX_INPUT);
            for (String split : splits) {
                values.add(Integer.parseInt(split));
            }

            input.add(new Proposal(proposalId++, (byte) 0, NetworkInterface.HOST_ID, values, 1));
            
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    public static void packetToProposal(byte[] payload, byte numberOfValues, long packetId) throws IOException{
        int offset = Packet.HEADER;
        for(int i = 0; i < numberOfValues; i++){
            
            Proposal proposal = new Proposal(payload, payload[Packet.SENDER_ID], offset);
            switch (proposal.type) {
                case 0:
                    deliverProposal(proposal);
                    break;
                case 1:
                    deliverAck(proposal);
                    break;
                case 2:
                    deliverNAck(proposal);
                    break;
                case 3:
                    clean(proposal.proposalNumber);
                    break;
                default:
                    throw new IOException("Exception message");
                    
            } 
            offset += proposal.getBytesLength();
        }
    }

    public static void latticeDeliver(Packet packet){
        try{
            messagesReceived.put(packet);
        }
        catch(Exception InterruptedException){
            Thread.currentThread().interrupt();
        }
    }

    private static void broadcastProposal(Proposal proposal) {
        int id = proposal.proposalNumber;
        
        activePerProposal.put(id, 1);
        
        proposedValue.put(id, new HashSet<>(proposal.proposedValues));

        if(!processesPerProposal.containsKey(id)){
            processesPerProposal.put(id, new HashSet<Byte>());
        }
        processesPerProposal.get(id).add(proposal.sender);

        if (!acceptedValue.containsKey(id) || proposal.proposedValues.containsAll(acceptedValue.get(id))) {
            acceptedValue.put(id, new HashSet<>(proposal.proposedValues));
            messagesPerProposal.put(id, new Integer[] {1, 0});
        } else {
            proposedValue.get(id).addAll(acceptedValue.get(id));
            acceptedValue.get(id).addAll(proposal.proposedValues);
            messagesPerProposal.put(id, new Integer[] {0, 1});
        }
        BestEffortBroadcast.bebBroadcast(proposal, false);
        checkAcknowledgements(id);
    }

    public static void deliverProposal(Proposal proposal) {
        int id = proposal.proposalNumber;

        if(!processesPerProposal.containsKey(id)){
            processesPerProposal.put(id, new HashSet<Byte>());
        }
        processesPerProposal.get(id).add(proposal.sender);

        if (!acceptedValue.containsKey(id) || proposal.proposedValues.containsAll(acceptedValue.get(id))) {
            acceptedValue.put(id, proposal.proposedValues);
            PerfectLink.perfectSend(new Proposal(id, (byte) 1, NetworkInterface.HOST_ID, null, proposal.activeProposalNumber), proposal.sender, true);
        } else {
            acceptedValue.get(id).addAll(proposal.proposedValues);
            PerfectLink.perfectSend(Proposal.createProposal(id, (byte) 2, NetworkInterface.HOST_ID, acceptedValue.get(id), proposal.activeProposalNumber), proposal.sender, false);
        }
        checkAcknowledgements(id);
    }

    public static void deliverNAck(Proposal proposal) {
        int id = proposal.proposalNumber, activeId = proposal.activeProposalNumber;

        if (activeId == activePerProposal.get(id)) {
            proposedValue.get(id).addAll(proposal.proposedValues);
            ++messagesPerProposal.get(id)[1];
            checkAcknowledgements(id);
        }
    }

    public static void deliverAck(Proposal proposal) {
        int id = proposal.proposalNumber, activeId = proposal.activeProposalNumber;

        if (activeId == activePerProposal.get(id)) {
            ++messagesPerProposal.get(id)[0];
            checkAcknowledgements(id);
        }
    }

    public static void terminateProposal(int id, Set<Integer> decided){
        activePerProposal.put(id, -1);
        messagesPerProposal.remove(id);
        processesPerProposal.remove(id);
        proposedValue.put(id, decided);
        
        if (++finished % 8 == 0) {
            if (readProposals(8)) {
                Iterator<Proposal> iterator = input.listIterator();
                while (iterator.hasNext()) {
                    Proposal proposal = iterator.next();
                    iterator.remove();
                    broadcastProposal(proposal);
                }
            }
        }

        while(deliveredProposals < NUMBER_PROPOSALS && activePerProposal.get(deliveredProposals) == -1){
            NetworkInterface.printOutput(proposedValue.remove(deliveredProposals));
            clean(deliveredProposals);
            BestEffortBroadcast.bebBroadcast(new Proposal(deliveredProposals, (byte) 3, NetworkInterface.HOST_ID, null, -2), false);
            deliveredProposals++;
        }        
    }

    public static void clean(int id) {
        int tmp = cleanCount.getOrDefault(id, 0) + 1;
        if (tmp == NetworkInterface.network.size()) {
            acceptedValue.remove(id);
            cleanCount.remove(id);
        }
        else{
            cleanCount.put(id, tmp);
        }   
    }

    public static void checkAcknowledgements(int id){
        if(activePerProposal.containsKey(id) && activePerProposal.get(id) != -1){
            if(((messagesPerProposal.get(id)[0] + messagesPerProposal.get(id)[1]) > NetworkInterface.network.size()/2)){ 
                if(messagesPerProposal.get(id)[1] > 0){
                    int activeId = activePerProposal.get(id) + 1;
                    activePerProposal.put(id, activeId);
                    if (proposedValue.get(id).containsAll(acceptedValue.get(id))) {
                        messagesPerProposal.get(id)[0] = 1; messagesPerProposal.get(id)[1] = 0;
                    } else {
                        proposedValue.get(id).addAll(acceptedValue.get(id));
                        messagesPerProposal.get(id)[0] = 0; messagesPerProposal.get(id)[1] = 1;
                    }
                    acceptedValue.get(id).addAll(proposedValue.get(id));
                    BestEffortBroadcast.bebBroadcast(Proposal.createProposal(id, (byte) 0, NetworkInterface.HOST_ID, proposedValue.get(id), activeId), true);
                }
                else{
                    terminateProposal(id, new HashSet<Integer>(proposedValue.get(id)));
                }
            }
            if(activePerProposal.get(id) != -1 && processesPerProposal.get(id).size() == NetworkInterface.network.size()){
                terminateProposal(id, new HashSet<Integer>(acceptedValue.get(id)));
            }
        }
    }
}