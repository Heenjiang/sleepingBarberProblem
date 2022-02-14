package threadImpl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SleepingBarberThreadPoolVersion {
	//default configuration
	static int allHaircut = 100;
	static int barberCount = 10;
	static int customerIntervalTime = 100;
	static int maxWaittingRoom = 20;
	static int barberHiarcutTime = 500;
	
	static AtomicInteger totoalHaricut = new AtomicInteger(0);
	static AtomicInteger avalibleBarber = new AtomicInteger(barberCount);
	
	static Queue<Integer> waittingRoom = new LinkedList<>();
	static MyQueue haircutOngoinQueue = new MyQueue(barberCount);
	
	final static Lock lock = new ReentrantLock(true);
	final static Condition condition = lock.newCondition();

	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		//user input configutation
		System.out.println("Pleas input the barber num:(end with enter)");
		barberCount = input.nextInt();
		System.out.println("Please input the interval time of every customer:"
				+ "(in milisec)");
		customerIntervalTime = input.nextInt();
		System.out.println("Please input the customer number:(end with enter)");
		allHaircut = input.nextInt();
		System.out.println("Please input the waitting room campacity:(end with enter)");
		maxWaittingRoom = input.nextInt();
		System.out.println("Please input the haricut time:(end with enter)");
		barberHiarcutTime = input.nextInt();
		input.close();
		totoalHaricut = new AtomicInteger(0);
		avalibleBarber = new AtomicInteger(barberCount);
		haircutOngoinQueue = new MyQueue(barberCount);
		
		Random r = new Random();
		
		List<Thread> ts = new ArrayList<Thread>();
		ExecutorService es = Executors.newFixedThreadPool(barberCount);
		for (int i = 0; i < barberCount; i++) {
			
			var barber = new Thread() {
				@Override
				public void run() {
					System.out.println("Baeber " + getName() + " initilazied and goes to sleep");
					while(true) {
						try {
							//get a customer from the blocking queue
							if(allHaircut <= totoalHaricut.get()) {
								System.out.println("All customer have got their hiarcut barber " + getName() + ""
										+ " goes off-line");
								return;
							}
							
							var customer = haircutOngoinQueue.get();
							
							//a barber goes to work
							avalibleBarber.decrementAndGet();
							System.out.println("Customer " + customer + " awakes Barber " + getName() +" up");
							System.out.println("Barber " + getName() + " is giving customer "+ customer + " haircut.... directly");
							//do haircut
							double devaation = r.nextDouble();
							var pauseTime = devaation > 0.5 ? (int) barberHiarcutTime*(1-devaation):(int) barberHiarcutTime*(1+devaation);
							Thread.sleep((long) pauseTime);
							System.out.println("Customer " + customer + " finished his/her haircut");
							if(totoalHaricut.addAndGet(1) >= allHaircut) {
								System.out.println("All customer have got their hiarcut barber " + getName() + ""
										+ " goes off-line");
								return;
							}
							System.out.println("Barber " + getName() + " goes to check the waitting room");
							//barber goes to the waitting room to check if any customer need a haircut
							int  waittingRoomCustomer;
							while(true) {
								if(lock.tryLock(100, TimeUnit.MILLISECONDS)) {
									try {
										if(waittingRoom.isEmpty()) {
											avalibleBarber.incrementAndGet();
											System.out.println("The waitting room is empty, barber " + getName() + "goes to sleep");
											break;
										}
										//awake a customer from the waitting room
										waittingRoomCustomer = waittingRoom.remove();
										System.out.println("Barber " + getName() + " awaked customer "+ waittingRoomCustomer + " in the waitting room");
									} finally {
										lock.unlock();
									}
									System.out.println("Barber " + getName() + " is giving customer "+ waittingRoomCustomer + " haircut....");								//do haircut
									devaation = r.nextDouble();
									pauseTime = devaation > 0.5 ? (int) barberHiarcutTime*(1-devaation):(int) barberHiarcutTime*(1+devaation);
									Thread.sleep((long) pauseTime);
									System.out.println("Customer " + waittingRoomCustomer + " finished his/her haircut");
									if(totoalHaricut.addAndGet(1) >= allHaircut) {
										System.out.println("All customer have got their hiarcut barber " + getName() + ""
												+ " goes off-line");
										return;
									}
									
								}
							}
						} catch (InterruptedException e) {
							return;
						}
					}
				}
			};
			
			es.submit(barber);
			ts.add(barber);
		}
		es.shutdown();
		var customerGenratorThread = new Thread() {
			@Override
			public void run() {
				//loop for generating customer and put them to the haircut queue or waitting room
				for (int i = 0; true; i++) {
					try {
						var devaation = r.nextDouble();
						var pauseTime = devaation > 0.5 ? (int) customerIntervalTime*(1-devaation):(int) customerIntervalTime*(1+devaation);
						Thread.sleep((long) pauseTime);
						if(lock.tryLock(100, TimeUnit.MILLISECONDS)) {
							try {
								if(avalibleBarber.get() <= 0 || totoalHaricut.get() >= allHaircut) {
									//waittiing room is full
									if(waittingRoom.size() >= maxWaittingRoom) {
										System.out.println("The waitting room is full! Customer "+ i+ " will leave");
										continue;
									}
									if(totoalHaricut.get() >= allHaircut){
										System.out.println("The Barber shop closed!");
										return;
									}
									else{
										//no avalible barber, customer should go to the waitting room
										waittingRoom.add(i);
										System.out.println("Customer "+ i+ " sits in the waitting room");
									}
									
								}
								else{
									//barber avaliable, so customer gets a haircut directly
									haircutOngoinQueue.add(i);
									System.out.println("Customer "+ i+ " find an avilable barber, and will get a haircut directly");
								}
								
							} finally {
								lock.unlock();
							} 
						}
//						System.out.println("waitting");
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		
		customerGenratorThread.start();
		try {
			customerGenratorThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
	}

}

class MyQueue {
	private int maxElementNum;
	//taskQueue
	private Queue<Integer> queue = new LinkedList<>();
	//reentrantLock: fair(means every thread accquire lock by order)
	private final Lock lock = new ReentrantLock(true);
	//condition instance
	private final Condition condition = lock.newCondition();
	
	public MyQueue(int maxElementNum) {
		this.maxElementNum = maxElementNum;
	}
	public boolean isEmp() {
		lock.lock();
		try {
			return this.queue.isEmpty();
		} finally {
			lock.unlock();
		}
		
	}
	public void add(Integer customer) {
		lock.lock();
		//because in java, the reentrantLock is not language level lock mechniashm 
		//like synchronized, so it maybe throw exception and when exception occured, we have to
		//release lock mannually
		try {
			if(queue.size()> maxElementNum) {
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