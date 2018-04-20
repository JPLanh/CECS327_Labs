
import java.io.IOException;
import java.rmi.*;

public interface ContextInterface extends Remote {
	public void setWorkingPeer(long page);
	public void completePeer(long page, long n) throws RemoteException;
	public boolean isPhaseCompleted();
	
	public void reduceContext(long source, ReduceInterface reducer,
			Context context) throws RemoteException;
	public void mapContext(long page, MapReduceInterface mapper,
			Context context) throws RemoteException, IOException;

}
