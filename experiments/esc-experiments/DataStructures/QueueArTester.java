package DataStructures;

import java.util.*;

public class QueueArTester
{
  /*@ spec_public */ private QueueAr q = null;

  private QueueArTester()
  {
    q = new QueueAr(0);
  }

  public void doNew(int size)
  {
    q = new QueueAr(size);
    observe();
  }

  public Object createItem(int i)
  {
    return new MyInteger(i);
  }

  public void observe()
  {
    isEmpty();
    isFull();
    getFront();
  }
  public void isEmpty()
  { q.isEmpty(); }
  public void isFull()
  { q.isFull(); }
  public void getFront()
  { q.getFront(); }

  public void makeEmpty()
  { observe(); q.makeEmpty(); observe(); }
  public void dequeue()
  { observe(); q.dequeue(); observe(); }
  public void enqueue(int x)
  { observe(); try { q.enqueue(createItem(x)); } catch (Exception e) { } observe(); }

  public void repEnqueue(int n)
  {
    for (int i=0; i < n; i++) {
      int x = (int)(1000 * Math.random());
      enqueue(x);
    }
  }

  public void dequeueAll()
  {
    while( !q.isEmpty( ) ) {
      dequeue();
    }
  }

  public void fillAndEmpty(int n)
  {
    System.out.println("fae " + n);
    doNew(n);
    repEnqueue(n);
    dequeueAll();
  }

  public void walkAround(int n)
  {
    System.out.println("wa " + n);
    doNew(n);
    for (int i=0; i < n/2; i++) {
      int x = (int)(1000 * Math.random());
      enqueue(x);
    }
    for (int i=0; i < 5*n; i++) {
      int x = (int)(1000 * Math.random());
      enqueue(x);
      dequeue();
    }
  }

  public void run()
  {
    fillAndEmpty(0);
    fillAndEmpty(1);
    fillAndEmpty(2);
    fillAndEmpty(5);
    fillAndEmpty(10);
    fillAndEmpty(20);
    fillAndEmpty(30);
    fillAndEmpty(50);
    walkAround(5);
    walkAround(10);
    walkAround(20);
    walkAround(30);
  }

  public static void main(String[] args)
  {
    (new QueueArTester()).run();
  }
}
