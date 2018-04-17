import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.*;
import java.util.HashSet;
import java.util.Set;

public class Context implements ContextInterface{
	long n = 0;
	Set<Long> set = new HashSet<Long>();
	Chord chord;
	
	public Context(Chord chord){
		this.chord = chord;
	}
	
	public void setWorkingPeer(long page){
		set.add(page);
	}
	
	public void completePeer(long page, long n) throws RemoteException
	{
		this.n += n;
		set.remove(page);
	}
	
	public boolean isPhaseCompleted(){
		if (set.isEmpty()) return true;
		return false;
	}
	
	public void reduceContext(long source, ReduceInterface reducer,
			Context context) throws RemoteException{
		if (source != chord.guid){
			context.setWorkingPeer(chord.guid);
			reduceContext(source, reducer, context);
		}
		
	}
	
	public void mapContext(long page, MapReduceInterface reducer,
			Context context) throws IOException{
    	InputStream is = chord.get(page);

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
	}
	
	
}
