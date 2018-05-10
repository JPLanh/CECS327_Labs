import java.rmi.*;
import java.util.TreeMap;
import java.io.*;

public interface ChordMessageInterface extends Remote
{
    public ChordMessageInterface getPredecessor()  throws RemoteException;
    public ChordMessageInterface getSuccessor() throws RemoteException;
    ChordMessageInterface locateSuccessor(long key) throws RemoteException;
    ChordMessageInterface closestPrecedingNode(long key) throws RemoteException;
    public void joinRing(String Ip, int port)  throws RemoteException;
    public Boolean isKeyInOpenInterval(long key, long key1, long key2) throws RemoteException;
    public void notify(ChordMessageInterface j) throws RemoteException;
    public boolean isAlive() throws RemoteException;
    public long getId() throws RemoteException;

    public void emitMap(long key, String value) throws RemoteException;
    public void emitReduce(long key, String value) throws RemoteException;
    
    public void put(long guidObject, InputStream inputStream) throws IOException, RemoteException;
    public InputStream get(long guidObject) throws IOException, RemoteException;
    public void delete(long guidObject) throws IOException, RemoteException;

    public void setWorkingPeer(Long page) throws RemoteException;
    public void completePeer(Long page, int n) throws RemoteException;
    public boolean isPhaseCompleted() throws RemoteException;
    
//    public void reduceContext(Long source, MapReduceInterface reducer,
//            ChordMessageInterface context) throws RemoteException;
    public void reduceContext(MapReduceInterface reducer,
            ChordMessageInterface context) throws RemoteException;
    public void mapContext(Long page, MapReduceInterface mapper,
            ChordMessageInterface context) throws RemoteException;
    
    public void whois() throws RemoteException;
    public void printAllMap(ChordMessageInterface initial) throws RemoteException;
    public void printAllReduce(ChordMessageInterface initial) throws RemoteException;
    public TreeMap<Long, String> getReduceMap() throws RemoteException;
//    public TreeMap<Long, String> getSucReduce() throws RemoteException;
}
