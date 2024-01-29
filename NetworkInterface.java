package cs451;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import cs451.parser.Host;
import cs451.parser.Parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

public class NetworkInterface {
    
    public static BufferedWriter writer;

    public static BufferedReader reader;

    public static byte HOST_ID;

    public static final AtomicBoolean running = new AtomicBoolean(true);

    public static final Map<Byte, Host> network = new HashMap<>();

    public static void create(Parser parser) {
        for(Host host : parser.hosts()){
            network.put((byte)(host.getId()-1), host);
        }
        HOST_ID = (byte)(parser.myId()-1);
        try {
            writer = new BufferedWriter(new FileWriter(parser.output()), 32768);
            reader = new BufferedReader(new FileReader(parser.config()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void closeOutput() {
        System.out.println("Immediately stopping network packet processing.");
        System.out.println("Writing output.");
        running.set(false);
        try {
            synchronized (writer) {
                writer.close();
                reader.close();
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
        
    }

    public static String getConfig() throws IOException{
        if (!running.get()) {
            return null;
        }
        synchronized (reader) {
            String data = reader.readLine();
            return data;
        }
    }

    public static Set<Integer> readInput(){
        String data;
        if (!running.get()) {
            return null;
        }
        synchronized (reader) {
            try{
                if((data = reader.readLine()) == null){
                    reader.close();
                    return null;
                } 
                return Arrays.stream(data.split(" ")).map(Integer::valueOf).collect(Collectors.toSet());
            }
            catch(IOException e){
                return null;
            }
        }
    }

    public static void printOutput(List<Integer> output, byte senderId) {
        if (!running.get()) return;
        try {
            synchronized (writer) {
                for(Integer data : output){
                        writer.write(String.format("d %d %d\n", Integer.valueOf(senderId)+1, data));
                }
                writer.flush();
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public static void printOutput(Set<Integer> decided) {
        if (!running.get()) return;
        try {
            synchronized (writer) {
                int i = 0;
                for(Integer data : decided){
                    if(i != 0) writer.write(" ");
                    writer.write(String.format("%d", data));
                    i++;
                }
                writer.write("\n");
                writer.flush();
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public static void printOutput(List<Integer> output, int size) {
        if (!running.get()) return;
        try {
            synchronized (writer) {
                for(int i = 0; i < size; i++){
                    writer.write(String.format("b %d\n", output.get(i)));
                }
                writer.flush();
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }
}
