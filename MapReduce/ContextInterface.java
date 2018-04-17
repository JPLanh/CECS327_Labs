<<<<<<< HEAD
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
=======
import java.rmi.*;

public interface ContextInterface extends Remote {
	public void setWorkingPeer(Long page);
	public void completePeer(Long page, Long n) throws RemoteException;
	public boolean isPhaseCompleted();
	
	public void reduceContext(Long source, ReduceInterface reducer,
			Context context) throws RemoteException;
	public void mapContext(Long page, MapReduceInterface mapper,
			Context context) throws RemoteException;
>>>>>>> 18c4b399a49b912c49feca840020a63b47f7404a
}
