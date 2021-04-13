package scw.consistency;


public interface Compensator extends Runnable{
	String getGroup();
	
	String getId();
	
	boolean isCancelled();
	
	boolean cancel();
	
	boolean isDone();
}
