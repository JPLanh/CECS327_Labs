import java.rmi.RemoteException;

public interface ReduceInterface {
	public void reduce(Long key, String values[], Context context) throws RemoteException;
}
