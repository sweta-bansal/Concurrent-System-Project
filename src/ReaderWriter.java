import java.util.LinkedList;
import java.util.Queue;



//import RWController.request;


public final class ReaderWriter {
	   public static void main (String args[]) {
		RWController c = new RWController(); 
		Reader r1 = new Reader(c,1);	Reader r2 = new Reader(c,2); Reader r3 = new Reader(c,3); //Reader r4 = new Reader(c,7);
		Writer w1 = new Writer(c,4);	Writer w2 = new Writer(c,5); Writer w3 = new Writer(c,6); //Writer w4 = new Writer(c,8);
		r1.start(); w1.start(); r2.start(); w2.start(); r3.start(); w3.start();//r4.start();w4.start();
		try {		r1.join(); w1.join(); r2.join(); w2.join();r3.join(); w3.join();//r4.join();w4.join();
		
		}
		catch (InterruptedException e) {}
	}
}
final class Reader extends TDThread {
	private RWController c;
	private int num;
	Reader (RWController c, int num) {this.c = c; this.num = num; }
	public void run () {
		////System.out.println("Reader"+num+" running");
		c.read(this,num);
		////System.out.println("Reader"+num+" finished");
	}
}
final class Writer extends TDThread {
	private RWController c;
	private int num;
	Writer (RWController c, int num) {this.c = c; this.num = num; }
	public void run () {		
		//System.out.println("Writer"+num+" running");
		c.write(this,num);
		//System.out.println("Writer"+num+" finished");
	}
}
final class RWController extends monitorSC {
// SC monitor for R<W.2: This strategy gives writers a higher priority than readers, 
// except that when a writer arrives, if it is a lead writer (i.e., no writer is 
// writing or waiting), it waits until all readers that arrived earlier have finished.

	/* decls here */
	int readerCount=0;
	boolean writing=false;
	int signalledReaders=0;
	int signalledWriters=0;
	conditionVariable readerQ=new conditionVariable();
	conditionVariable writerQ=new conditionVariable();
	Queue<request> readRequest=new LinkedList<request>();
	Queue<request> writeRequest=new LinkedList<request>();
	int noReader=0;
   
  	class request {
	/* requests from readers and writers are saved as request objects */
  		int ID;
  		int noOfWaitingReaders=0;
  		boolean leadWriter=false;
  		public request(int ID)
  		{
  			this.ID = ID;
  			//this.noOfWaitingReaders = readRequest.size()-signalledReaders;
  			
  			if(!writing && writeRequest.size()==0)
  			{
  				this.leadWriter=true;
  				this.noOfWaitingReaders = readRequest.size();
  			}
  				
  		}
  	}
   	
	public RWController() {   

	}
	public void read(TDThread caller,int ID) {
		request(caller,ID,true);
		startRead(caller,ID); 
		//System.out.println("Reader" + ID + " reading");
		//try{Thread.sleep(10);}catch(InterruptedException e){}; 
		endRead(caller,ID); 
	}
	public void write(TDThread caller, int ID) {
		request(caller,ID,false);
		startWrite(caller,ID); 
		//System.out.println("Writer" + ID + " writing");
		//try{Thread.sleep(10);}catch(InterruptedException e){}; 
		endWrite(caller,ID);
	}
	private void request(TDThread caller, int ID, boolean isReadRequest) {     
		enterMonitor("request");
		// save requests and use them in startRead and startWrite to force readers
		// and writers to read and write in the correct order. This method should have no waitC() statements.
		
		// Add your code - if reader do this else writers do this
 
 		// execute these statements for readers only
		//request r;
		if(isReadRequest)
		{
			request r = new request(ID);
			readRequest.add(r);
	 		ApplicationEvents.exerciseEvent("requestR"+ID);   // these events will be used for testing
			exerciseEvent("requestR"+ID);          				// these events will be used for testing
			
		}
		
		else{
	 		// execute these statements for writers only
			request r = new request(ID);
			writeRequest.add(r);
			ApplicationEvents.exerciseEvent("requestW"+ID);   // these events will be used for testing
			exerciseEvent("requestW"+ID);          				// these events will be used for testing
			
		}
		exitMonitor();
	}

	private void startRead(TDThread caller, int ID) {
		enterMonitor("startRead");

		// Add your code
		//removed || ID!=((request)readRequest.peek()).ID
		// removed|| signalledReaders>0
		
		while(writing || signalledWriters>0)
		
		{
			
				readerQ.waitC();
		}
		
		while(!writeRequest.isEmpty() && writeRequest.peek().leadWriter==true && writeRequest.peek().noOfWaitingReaders==0)
		{
			readerQ.waitC();
		}
		if(!writeRequest.isEmpty() && writeRequest.peek().noOfWaitingReaders>0 )
			writeRequest.peek().noOfWaitingReaders--;
		readRequest.remove();
		++readerCount;
		
		if(signalledReaders>0)
		{
			signalledReaders--;
		}
		
		
		
		ApplicationEvents.exerciseEvent("startRead"+ID);
		exerciseEvent("startRead"+ID);
		exitMonitor();
	}
	
	private void endRead(TDThread caller, int ID) {
		enterMonitor("endRead");
 
 		// Add your code
		--readerCount;
		
		if(!writeRequest.isEmpty() && writeRequest.peek().noOfWaitingReaders>0)
		{
			
				readerQ.signalC();
		}
			
		if(readerCount==0 && signalledReaders==0 )
			writerQ.signalCall();
		else
			readerQ.signalC();
		
			
 		ApplicationEvents.exerciseEvent("endRead"+ID);
		exerciseEvent("endRead"+ID);
		exitMonitor();
	}
	
	private void startWrite(TDThread caller, int ID) {
		enterMonitor("startWrite");
		//professors code
 		// Add your code
		while (writing ||                                   // writer writing 
			       readerCount>0 ||                               // reader(s) reading
			       signalledReaders>0 ||                  // readers temporarily have priority
			      ID != (writeRequest.peek()).ID   // ID not first in the FCFS write request queue
			     )
		{
			    writerQ.waitC();
		}
		if(writeRequest.peek().noOfWaitingReaders>0)
			writerQ.waitC();
		writeRequest.remove();
		writing=true;
		if(signalledWriters>0)
		{
			signalledWriters--;
		}
 		ApplicationEvents.exerciseEvent("startWrite"+ID);
		exerciseEvent("startWrite"+ID);
		exitMonitor();
	}
	//start writing
	private void endWrite(TDThread caller, int ID) {
		enterMonitor("endWrite");
  
  		// Add your code
		writing=false;
		//writeRequest.remove();
		/*if(signalledWriters>0)
		{
			writerQ.signalC();
		}*/
		
		if (!writerQ.empty())
		{
			signalledWriters=writerQ.length();
			writerQ.signalCall();
		}
		else if (writeRequest.isEmpty())
			{
			
				signalledReaders=readerQ.length();
				readerQ.signalCall();
			}
		
				
		ApplicationEvents.exerciseEvent("endWrite"+ID);  
		exerciseEvent("endWrite"+ID);
		exitMonitor();
	}
}

