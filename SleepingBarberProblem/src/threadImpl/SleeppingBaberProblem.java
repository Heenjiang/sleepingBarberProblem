package threadImpl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;



public class SleeppingBaberProblem {
	//running parameters
	final static int MAXWAITTING = 20;
	final static int BARBER = 5;
	final static int CUSTOMER = 30;
	//waitting room stimulation
	static WaittingRoomQueue waittingQueue = new WaittingRoomQueue(MAXWAITTING);

	public static void main(String[] args) {
		var barberThreadList = new ArrayList<Thread>();
		//simulate barbers 
		for (int i = 0; i < BARBER; i++) {
			var t = new Thread() {
				@Override
				public void run() {
					//a barber spends his life to give customer haircut
					while(true) {
						try {
							int customerNum = waittingQueue.get();
							System.out.println("Barber " + getName() + 
									" is givng haircut for customer "+customerNum + "...'");
							Thread.sleep(100);
						} catch (InterruptedException e) {
							System.out.println(getName() + "is interrupted when giving haircut to customer");
							return;
						}
					}
				}
			};
			t.start();
			barberThreadList.add(t); 
		}
		
		var addCustomer = new Thread() {
			@Override
			public void run() {
				//stimulate customer comes in m time interval
				for (int i = 0; i < CUSTOMER; i++) {
					System.out.println("A customer getting to the waitting room");
					waittingQueue.add(i);
					try {
						Thread.sleep((int)(Math.random()*100));
					} catch (InterruptedException e) {
						System.out.println("addCustmer thread is interrupted");
					}
				}
			}
		};
		addCustomer.start();
		try {
			addCustomer.join();
			Thread.sleep(10000);
		} catch (Exception e) {
			System.out.println("main thread is interrupted");
		}
		
		//
		for (Thread thread : barberThreadList) {
			thread.interrupt();
		}
	}
}

class WaittingRoomQueue {
	private int maxElementNum;
	//taskQueue
	private Queue<Integer> queue = new LinkedList<>();
	//reentrantLock: fair(means every thread accquire lock by order)
	private final Lock lock = new ReentrantLock(true);
	//condition instance
	private final Condition condition = lock.newCondition();
	
	public WaittingRoomQueue(int maxElementNum) {
		this.maxElementNum = maxElementNum;
	}
	
	public void add(Integer customer) {
		lock.lock();
		//because in java, the reentrantLock is not language level lock mechniashm 
		//like synchronized, so it maybe throw exception and when exception occured, we have to
		//release lock mannually
		try {
			if(queue.size()>= maxElementNum) {
				throw new RuntimeException();
			}
			queue.add(customer);
			//every time add a customer to quue, signal all sleeping barber threads
			condition.signalAll();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}finally {
			lock.unlock();
		}
	}
	
	public int get() throws InterruptedException {
	    lock.lock();
	    //when get an elemnt from the queue, first check wheather the queue is empty
	    //if it is empty, current thread goes to sleep and release lock
	    //if is not empty, return 1st element of the queue.
        try {
            while (queue.isEmpty()) {
                condition.await();
            }
            return queue.remove();
        } finally {
			//no matter what happen, we have to release the lock.
            lock.unlock();
        }
	}
	
}
