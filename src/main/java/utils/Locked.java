package utils;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class Locked<A> {
    protected final A object;
    protected ReentrantLock mutex = new ReentrantLock();

    public Locked(A object) {
        this.object = object;
    }

    public <R> R transaction(Function<A, R> fun) {
        mutex.lock();
        R result = fun.apply(object);
        mutex.unlock();

        return result;
    }
}
