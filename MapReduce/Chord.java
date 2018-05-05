import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.net.*;
import java.util.*;
import java.io.*;


public class Chord extends java.rmi.server.UnicastRemoteObject implements ChordMessageInterface
{
    public static final int M = 2;

    int numOfRecord;
    Registry registry;    // rmi registry for lookup the remote objects.
    ChordMessageInterface successor;
    ChordMessageInterface predecessor;
    ChordMessageInterface[] finger;
    int nextFinger;
    long guid;   		// GUID (i)
    Set<Long> set = new HashSet<Long>();
    public TreeMap<Long, String> PreBReduceTreeMap;
    public TreeMap<Long, String> SucBReduceTreeMap;
    public TreeMap<Long, List<String>> BMap;


    public Boolean isKeyInSemiCloseInterval(long key, long key1, long key2)
    {
        if (key1 < key2)
            return (key > key1 && key <= key2);
        else
            return (key > key1 || key <= key2);
    }

    public Boolean isKeyInOpenInterval(long key, long key1, long key2)
    {
        if (key1 < key2)
            return (key > key1 && key < key2);
        else
            return (key > key1 || key < key2);
    }

    public void put(long guidObject, InputStream stream) throws RemoteException {
        try {
            String fileName = "./"+guid+"/repository/" + guidObject;
            FileOutputStream output = new FileOutputStream(fileName);
            while (stream.available() > 0)
                output.write(stream.read());
            output.close();
        }
        catch (IOException e) {
            System.out.println(e);
        }
    }

    public InputStream get(long guidObject) throws RemoteException {
        FileStream file = null;
        try {
            file = new FileStream("./"+guid+"/repository/" + guidObject);
        } catch (IOException e)
        {
            throw(new RemoteException("File does not exists"));
        }
        return file;
    }

    public void delete(long guidObject) throws RemoteException {
        File file = new File("./"+guid+"/repository/" + guidObject);
        file.delete();
    }

    public long getId() throws RemoteException {
        return guid;
    }
    public boolean isAlive() throws RemoteException {
        return true;
    }

    public ChordMessageInterface getPredecessor() throws RemoteException {
        return predecessor;
    }

    public ChordMessageInterface getSuccessor() throws RemoteException{
        return successor;
    }
    public ChordMessageInterface locateSuccessor(long key) throws RemoteException {
        if (key == guid)
            throw new IllegalArgumentException("Key must be distinct that  " + guid);
        if (successor.getId() != guid)
        {
            if (isKeyInSemiCloseInterval(key, guid, successor.getId()))
                return successor;
            ChordMessageInterface j = closestPrecedingNode(key);

            if (j == null)
                return null;
            return j.locateSuccessor(key);
        }
        return successor;
    }

    public ChordMessageInterface closestPrecedingNode(long key) throws RemoteException {
        // todo
        if(key != guid) {
            int i = M - 1;
            while (i >= 0) {
                try{

                    if(isKeyInSemiCloseInterval(finger[i].getId(), guid, key)) {
                        if(finger[i].getId() != key)
                            return finger[i];
                        else {
                            return successor;
                        }
                    }
                }
                catch(Exception e)
                {
                    // Skip ;
                }
                i--;
            }
        }
        return successor;
    }

    public void joinRing(String ip, int port)  throws RemoteException {
        try{
            System.out.println("Get Registry to joining ring");
            Registry registry = LocateRegistry.getRegistry(ip, port);
            ChordMessageInterface chord = (ChordMessageInterface)(registry.lookup("Chord"));
            predecessor = null;
            successor = chord.locateSuccessor(this.getId());
            System.out.println("Joining ring");
        }
        catch(RemoteException | NotBoundException e){
            successor = this;
        }   
    }

    public void findingNextSuccessor()
    {
        int i;
        successor = this;
        for (i = 0;  i< M; i++)
        {
            try
            {
                if (finger[i].isAlive())
                {
                    successor = finger[i];
                }
            }
            catch(RemoteException | NullPointerException e)
            {
                finger[i] = null;
            }
        }
    }

    public void stabilize() {
        try {
            if (successor != null)
            {
                ChordMessageInterface x = successor.getPredecessor();

                if (x != null && x.getId() != this.getId() && isKeyInOpenInterval(x.getId(), this.getId(), successor.getId()))
                {
                    successor = x;
                }
                if (successor.getId() != getId())
                {
                    successor.notify(this);
                }
            }
        } catch(RemoteException | NullPointerException e1) {
            findingNextSuccessor();

        }
    }

    public void notify(ChordMessageInterface j) throws RemoteException {
        if (predecessor == null || (predecessor != null
                && isKeyInOpenInterval(j.getId(), predecessor.getId(), guid)))
            predecessor = j;
        try {
            File folder = new File("./"+guid+"/repository/");
            File[] files = folder.listFiles();
            for (File file : files) {
                long guidObject = Long.valueOf(file.getName());
                if(guidObject < predecessor.getId() && predecessor.getId() < guid) {
                    predecessor.put(guidObject, new FileStream(file.getPath()));
                    file.delete();
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            //happens sometimes when a new file is added during foreach loop
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void fixFingers() {

        long id= guid;
        try {
            long nextId = this.getId() + 1<< (nextFinger+1);
            finger[nextFinger] = locateSuccessor(nextId);

            if (finger[nextFinger].getId() == guid)
                finger[nextFinger] = null;
            else
                nextFinger = (nextFinger + 1) % M;
        }
        catch(RemoteException | NullPointerException e){
            e.printStackTrace();
        }
    }

    public void checkPredecessor() { 	
        try {
            if (predecessor != null && !predecessor.isAlive())
                predecessor = null;
        } 
        catch(RemoteException e) 
        {
            predecessor = null;
            //           e.printStackTrace();
        }
    }

    public Chord(int port, long guid) throws RemoteException {

        PreBReduceTreeMap = new  TreeMap<Long, String>();
        SucBReduceTreeMap = new  TreeMap<Long, String>();
        BMap = new TreeMap<Long, List<String>>();
        int j;
        finger = new ChordMessageInterface[M];
        for (j=0;j<M; j++){
            finger[j] = null;
        }
        this.guid = guid;

        predecessor = null;
        successor = this;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                stabilize();
                fixFingers();
                checkPredecessor();
            }
        }, 500, 500);
        try{
            // create the registry and bind the name and object.
            System.out.println(guid + " is starting RMI at port="+port);
            registry = LocateRegistry.createRegistry( port );
            registry.rebind("Chord", this);
        }
        catch(RemoteException e){
            throw e;
        } 
    }

    void Print()
    {   
        int i;
        try {
            if (successor != null)
                System.out.println("successor "+ successor.getId());
            if (predecessor != null)
                System.out.println("predecessor "+ predecessor.getId());
            for (i=0; i<M; i++)
            {
                try {
                    if (finger != null)
                        System.out.println("Finger "+ i + " " + finger[i].getId());
                } catch(NullPointerException e)
                {
                    finger[i] = null;
                }
            }
        }
        catch(RemoteException e){
            System.out.println("Cannot retrive id");
        }
    }

    public void setWorkingPeer(Long page)  throws RemoteException{
        set.add(page);
    }

    public void completePeer(Long page, int i) throws RemoteException
    {
        this.numOfRecord += i;
        set.remove(page);
    }

    public boolean isPhaseCompleted() throws RemoteException{

        return set.isEmpty();
    }

    /** Goes through each node and have it runs the reduceContext, Reducing their BMap
     * @param Source Who started the recursion so it can stop when it reaches itself
     * @param reducer the reducer class to perform either map or reduce
     * @param the node performing the process. Provide flexability so anyone can make this done it's work properly
     */
    public void reduceContext(Long source, MapReduceInterface reducer,
            ChordMessageInterface context) throws RemoteException{
        if (source != guid){
            successor.reduceContext(source, reducer, context);
        }
        context.setWorkingPeer(source);
        Thread mappingThread = new Thread(){
            public void run(){
                Set<Long> setOfKeys = BMap.keySet();
                try{
                    for (Long key : setOfKeys){
                        List<String> getList = BMap.get(key);
                        reducer.reduce(key, getList, context);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                    try {
                        completePeer(source, 1);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
            }
        };        
        mappingThread.start();

    }

    /** Reads a page and send the content to the mapper so it can map the word to their right key
     * @param page The page to be read
     * @param reducer the reducer class to perform either map or reduce
     * @param the node performing the process. Provide flexability so anyone can make this done it's work properly
     */
    public void mapContext(Long page, MapReduceInterface reducer,
            ChordMessageInterface context) throws RemoteException{

        Thread mappingThread = new Thread(){
            public void run(){
                try {
                    context.setWorkingPeer(page);
                    InputStream is = context.get(page);
                    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                    int nRead = is.read();
                    while (nRead != 0){
                        byteBuffer.write(nRead);
                        nRead = is.read();
                    }
                    byteBuffer.flush();
                    is.close();
                    byte[] readByte = new byte[1024];
                    readByte = byteBuffer.toByteArray();
                    reducer.map(page, new String(readByte), context);   
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        mappingThread.start();
    }

    /** Check to see if the key lies either before or after the node, if not it will be passed to the next node
     * @param key the key to be compared between the two nodes
     * @param value the value of the key
     */
    public void emitMap(long key, String value)  throws RemoteException{
        if (isKeyInOpenInterval(key, predecessor.getId(), successor.getId())){
            List<String> tempList = BMap.get(key);
            if (tempList == null) tempList = new ArrayList<String>();
            tempList.add(value);
            BMap.put(key, tempList);      
        } else {
            ChordMessageInterface peer = locateSuccessor(key);
            peer.emitMap(key, value);
        }
    }

    /** The key will check to see if it falls before the guid or after the guid
     * @param key the key to be compared between the two nodes
     * @param value the value of the key
     */
    public void emitReduce(long key, String value) throws RemoteException{
        if(isKeyInOpenInterval(key, predecessor.getId(), successor.getId())){
            if (isKeyInOpenInterval(key, predecessor.getId(), guid)) {
                PreBReduceTreeMap.put(key, value);
            } else if (isKeyInOpenInterval(key, guid, successor.getId())) {
                SucBReduceTreeMap.put(key, value);
            }

        } else {
            ChordMessageInterface peer = locateSuccessor(key);
            peer.emitReduce(key, value);
        }
    }

    /** If the reducing process created two set of the distribution, it will give the predecessor's to the current node portion
     * @return the treemap containing the predecessor's portion.
     */
    public TreeMap<Long, String> getPreReduce()  throws RemoteException{
        return PreBReduceTreeMap;
    }

    /** If the reducing process created two set of the distribution, it will give the successor's to the current node portion
     * @return the treemap containing the successor's portion.
     */
    public TreeMap<Long, String> getSucReduce()  throws RemoteException{
        return SucBReduceTreeMap;
    }
}
