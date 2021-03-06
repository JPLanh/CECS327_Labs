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
    //    public TreeMap<Long, String> PreBReduceTreeMap;
    public TreeMap<Long, String> BReduceTreeMap;
    public TreeMap<Long, List<String>> BMap;


    /** Make sure the key belong within the interval and exactly on the chord
     * @param key the guid to be compared between the two keys
     * @param key1 the beginning guid
     * @param key2 the end guid
     * @return if the guid belong between the two key
     */
    public Boolean isKeyInSemiCloseInterval(long key, long key1, long key2)
    {
        if (key1 < key2)
            return (key > key1 && key <= key2);
        else
            return (key > key1 || key <= key2);
    }

    /** Make sure the key belong within the interval but not exactly on the chord
     * @param key the guid to be compared between the two keys
     * @param key1 the beginning guid
     * @param key2 the end guid
     * @return if the guid belong between the two key
     */
    public Boolean isKeyInOpenInterval(long key, long key1, long key2)
    {
        if (key1 < key2)
            return (key > key1 && key < key2);
        else
            return (key > key1 || key < key2);
    }

    /** Put a file into the repository of the chord
     * @param guidObject the guid to put into the chord
     * @param stream the inputstream cooresponding to the guid of the file
     */
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

    /** Read the InputStream from a page or guid if you will
     * @param the guid of the page
     * @return InputStream of the page that has been read
     */
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

    /** Delete the file within the chord
     *  @param the guid of the file to be deleted
     */
    public void delete(long guidObject) throws RemoteException {
        File file = new File("./"+guid+"/repository/" + guidObject);
        file.delete();
    }

    /** Get the ID of the chord
     *  @return the ID of the chord
     */
    public long getId() throws RemoteException {
        return guid;
    }

    /** Just to make sure that the chord is being responsive, or acknowledging
     * 
     */
    public boolean isAlive() throws RemoteException {
        return true;
    }

    /** Get the predecessor for the current chord
     * @return the predecessor for the current cord
     */
    public ChordMessageInterface getPredecessor() throws RemoteException {
        return predecessor;
    }

    /** Get the successor for the current chord
     * @return the successor
     */
    public ChordMessageInterface getSuccessor() throws RemoteException{
        return successor;
    }

    /** Error here, there's inconsistencies with locating if the key is within a successor.
     * 
     */
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

    /** Find the closest preceding node
     * @param key find the closest node according to the key
     * @return the closest preceding node
     */
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

    /** Allows a chord to join the ring, or the distributed file system with the port given and IP
     * @Param ip The destination for the chord to join
     * @param port the port that the chord are to join in.
     */
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

    /** Find the next successors
     * 
     */
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

    /** Stablize it's successor so that way it can be a loop.
     * 
     */
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

    /** notifies predecessor and successor of the list of files
     * 
     */
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

    /** Supposibly fix the fingers for a node after a new joins so that way it knows whos in the ring
     * 
     */
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

    /** Check to see if the predecessor has already been determined
     * 
     */
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

    /** Constructor for the chord
     * 
     * @param port The port that the chord will be connecting to
     * @param guid The guid of the chord
     * @throws RemoteException
     */
    public Chord(int port, long guid) throws RemoteException {

        //        PreBReduceTreeMap = new  TreeMap<Long, String>();
        BReduceTreeMap = new  TreeMap<Long, String>();
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

    /** Print some information about the current chord
     * 
     */
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

    /** Increment the set by 1 when a peer is tasked to do some work.                                      
     * 
     */
    public void setWorkingPeer(Long page)  throws RemoteException{
        set.add(page);
    }

    /** Reduce the set by 1 when a peer is completed with it's page
     * @param page the page that the peer is working on
     * @param i the record of the page
     */
    public void completePeer(Long page, int i) throws RemoteException
    {
        this.numOfRecord += i;
        set.remove(page);
    }

    /** Check to see if the peer completed their process by seeing if the set is empty
     * @return if the set is empty
     */
    public boolean isPhaseCompleted() throws RemoteException{

        return set.isEmpty();
    }

    public void whois() throws RemoteException{
        System.out.println("Pre: " + getPredecessor().getId());
        System.out.println("Suc: " + getSuccessor().getId());
    }


	/**
	* the reduce context thread that runs the reduce in the background
	* @param reducer the map reducer object
	* @param context the context
	*
	*/
    public void reduceContext(MapReduceInterface reducer,
            ChordMessageInterface context) throws RemoteException{
        if (context.getId() != guid){
            successor.reduceContext(reducer, context);
        }
        context.setWorkingPeer(getId());
        Thread mappingThread = new Thread(){
            public void run(){
                Set<Long> setOfKeys = BMap.keySet();
                long keyApp = 0;
                try{
                    for (Long key : setOfKeys){
                        List<String> getList = BMap.get(key);
                        keyApp += key;
                        reducer.reduce(key, getList, context);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                try {      
                    context.completePeer(guid, 1);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
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

	/**
	* prints all the maps recursively 
	* @param initial the initial chord value
	*
	*/
    public void printAllMap(ChordMessageInterface initial) throws RemoteException{
        if (guid != initial.getId()){
            successor.printAllMap(initial);
        }
        System.out.println(BMap);        
    }
	/**
	* prints all the map reduce recursively 
	* @param initial the initial chord value
	*
	*/	
    public void printAllReduce(ChordMessageInterface initial) throws RemoteException{
        if (guid != initial.getId()){
            successor.printAllReduce(initial);
        }
        System.out.println(getId() + ": " + BReduceTreeMap);        
    }
    
    /** Check to see if the key lies either before or after the node, if not it will be passed to the next node
     * @param key the key to be compared between the two nodes
     * @param value the value of the key
     */
    public void emitMap(long key, String value)  throws RemoteException{
        if (isKeyInOpenInterval(key, getId(), successor.getId())){            
            List<String> tempList = BMap.get(key);
            if (tempList == null) tempList = new ArrayList<String>();
            tempList.add(value);
            BMap.put(key, tempList);      
        } else {
            successor.emitMap(key, value);
        }
    }

    /** The key will check to see if it falls before the guid or after the guid
     * @param key the key to be compared between the two nodes
     * @param value the value of the key
     */
    public void emitReduce(long key, String value) throws RemoteException{
      if(isKeyInOpenInterval(key, getId(), successor.getId())){
            BReduceTreeMap.put(key,  value);
        } else {
            successor.emitReduce(key, value);
        }
    }

    /** If the reducing process created two set of the distribution, it will give the predecessor's to the current node portion
     * @return the treemap containing the predecessor's portion.
     */
    public TreeMap<Long, String> getReduceMap()  throws RemoteException{
        return BReduceTreeMap;
    }

}
