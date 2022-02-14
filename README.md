# Sleeping Barber Problem

## Description of problem

The sleeping barber problem was originally invented by **Edsger Dijkstra**, and the description of the problem is here:https://en.wikipedia.org/wiki/Sleeping_barber_problem#cite_note-6

## Solution

First start with multiple threads solution. The original problem only has 1 barber, but instead of only one, here we have M barbers. So, the complexity of problem grows.

### what need to solve?

When I first read the description of this problem, I kind of naturally relate it to the consumer-producer problem. But they are different! 

The first problem is **race condition**, where both barber and customer are waiting each other, and this might cause dead lock. To solve this one, we need to first identify critical code section and then use **lock** to show that only one thread could get access to the critical code section.

The second one is **fairness or starvation problem**, under concurrent condition. We have no control of certain thread's scheduling, cause its the OS job. So for example, when a customer comes, there maybe multiple barber are sleeping, it is possible that every time the OS will pick up one barber, and never let other barber to do it. To avoid this, we could use **FIFO queue**.

### Implementation Design

There are 2 actors in the sleeping barber problem, the customer and the barber. Below pics show working flow of barber and customer accordingly. 

#### Barber Thread



<img src="https://github.com/Heenjiang/sleepingBarberProblem/blob/master/SleepingBarberProblem/barber_working_flowchart.png?raw=true" alt="barber_working_flowchart.png" style="zoom:50%;" />

A barber will go to sleep after initialized, and wait any customer comes to give her/him haircut.(here when a customer comes, if there are any available barbers, the customer will get to the workingQueue which is blocking access implemented by ReentrantLock and LinkedList). After giving a haircut to a customer, barber will go to check the waittingRoom queue, if there are any waiting customer, wake him/her up and give her/him haircut, otherwise barber goes to sleep.

#### Customer Thread

<img src="https://github.com/Heenjiang/sleepingBarberProblem/blob/master/SleepingBarberProblem/customer_workin_flowchart.png?raw=true" alt="customer_workin_flowchart.png" style="zoom:50%;" />

Compare to the barber thread, the customer one is much simpler. When a customer comes（simply an integer number generated stands for a customer）,first he/she check if any barber available, then decide go to the workingQueue or continue  to check if the waittingRoom queue is full to decide leave or join the waittingRoom queue.



### Java Implementation

#### Solve the race condition and fairness problem.

From above flow chart, we know in program we need 2 queues, one for abstraction of waiting room, another for working Queue(means where a customer could get a haircut directly). The feature of the queue data structure is FIFO, so this means we could use queue to solve the **fairness problem**. And in Java, there is only a queue interface, but we could use **LinkedList** implement the queue structure.

And the ***java.util.concurrent*** package provide different kind of locks for solving the race condition problem. The most important mechanism for avoiding dead lock is **wait/notify**. For efficacy, we will use the **ReentrantLock**. Compare to the **synchronized** keyword, ReentrantLock can have several advantages:

1. power to create **fair lock**;
2. ability to **timeout while waiting for lock;**
3. ability to lock **interruptedly**

```java
	final static Lock lock = new ReentrantLock(true);
	final static Condition condition = lock.newCondition();
```

and also we need the ***Condition*** serve as signal to perform ***await and notify*** functions . To avoid code complexity, we can implement our own blocking queue.

```java
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
		//like synchronized, so it maybe throw exception and when exception occured,
        //we have to release lock mannually
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
```

#### When to exits?

We will declare a variable to indicate whether end the program.

```java
static AtomicInteger totoalHaricut = new AtomicInteger(0);
```

When the totoalHaircut hits the limitation, all thread will ends itself. And program ends.

#### Running program on the multiple cores 

Java provides us the ***java.util.concurrent.ExecutorService*** interface for executing tasks on multiple cores, and we don't have to worry about the low level interaction with OS. And the ***java.util.concurrent.Executors*** implemented the  ***java.util.concurrent.ExecutorService*** interface in 3 ways. we will use ***newFixedThreadPool***.		

```java
ExecutorService es = Executors.newFixedThreadPool(barberCount);
		
		for (int i = 0; i < barberCount; i++) {		
			var barber = new Thread() {
			@Override
			public void run() {};
            es.submit(barber);
            ts.add(barber);
		}
            
es.shutdown();
```

#### Configuration parameters

```java
	//default parameters
	static int allHaircut = 100;
	static int barberCount = 10;
	static int customerIntervalTime = 100;
	static int maxWaittingRoom = 20;
	static int barberHiarcutTime = 500;
	//when the amount of haircut reach this limitation, program ends
	static AtomicInteger totoalHaricut = new AtomicInteger(0);
	static AtomicInteger avalibleBarber = new AtomicInteger(barberCount);
	
	//2 queues 
	//notice the waitting room queue we use the general non-blocking queue
	static Queue<Integer> waittingRoom = new LinkedList<>();
	static MyQueue haircutOngoinQueue = new MyQueue(barberCount);
	
	//lock object and condition object for solving contention problem
	final static Lock lock = new ReentrantLock(true);
	final static Condition condition = lock.newCondition();
```

## Test Java program

