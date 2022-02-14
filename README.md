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



