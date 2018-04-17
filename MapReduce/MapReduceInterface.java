import java.io.IOException;

public interface MapReduceInterface {
<<<<<<< HEAD
	public void map(Long key, String value, Context context) throws IOException;
	public void reduce(Long key, String[] value, Context context) throws IOException;
=======
	public void map(Long key, String value) throws IOException;
	public void reduce(Long key, String value[]) throws IOException;
>>>>>>> 18c4b399a49b912c49feca840020a63b47f7404a
}
