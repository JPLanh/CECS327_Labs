import java.rmi.*;
import java.util.Set;

public class Context implements ContextInterface{
	long n = 0;
	Set<Long> set;
	
	public void setWorkingPeer(Long page){
		set.add(page);
	}
	
	public void completePeer(Long page, Long n) throws RemoteException
	{
		this.n += n;
		set.remove(page);
	}
	
	public boolean isPhaseCompleted(){
		if (set.isEmpty()) return true;
		return false;
	}
	
	public void reduceContext(Long source, ReduceInterface reducer,
			Context context) throws RemoteException{
		//TODO
	}
	
	public void mapContext(Long source, MapReduceInterface reducer,
			Context context) throws RemoteException{
		//TODO
	}
}
