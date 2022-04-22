package utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class LockFreeDynamicArray<E> implements DynamicArray<E> {
    private interface Value<K> {}

    private static class Fixed<K> implements Value<K> {
        private final K value;
        public Fixed(K value) {
            this.value = value;
        }
    }

    private static class Just<K> implements Value<K> {
        private final K value;
        public Just(K value) {
            this.value = value;
        }
    }

    static class Shifted<E> implements Value<E> {}

    private static class Core<T> {
        final int capacity;
        final AtomicReferenceArray<T> array;
        final AtomicReference<Core<T>> next = new AtomicReference<>(null);

        public Core(int capacity) {
            this.capacity = capacity;
            this.array = new AtomicReferenceArray<>(capacity);
        }
    }

    private static final int INITIAL_CAPACITY = 1;

    private final AtomicReference<Core<Value<E>>> core = new AtomicReference<>(new Core<>(INITIAL_CAPACITY));
    private final AtomicInteger curSize = new AtomicInteger(0);

    @Override
    public E get(int index) {
        if (index >= getSize()) {
            throw new IllegalArgumentException("Array index out of range");
        }

        var curArr = core.get();

        while (true) {
            final var it = curArr.array.get(index);

            if (it instanceof Fixed<E>) {
                return ((Fixed<E>) it).value;
            } else if (it instanceof Just<E>) {
                return ((Just<E>) it).value;
            } else {
                curArr = curArr.next.get();
            }
        }
    }

    @Override
    public void put(int index, E element) {
        if (index >= getSize()) {
            throw new IllegalArgumentException("Array index out of range");
        }

        var curArr = core.get();

        while (true) {
            final var it = curArr.array.get(index);

            if (it instanceof Shifted<E>) {
                curArr = curArr.next.get();
            } else {
                if (it instanceof Fixed<E>) {
                    continue;
                }
                if (curArr.array.compareAndSet(index, it, new Just<>(element))) {
                    return;
                }
            }
        }
    }

    @Override
    public int pushBack(E element) {
        while (true) {
            var size = getSize();
            var curCore = core.get();
            var capacity = curCore.capacity;

            if (size != getSize()) {
                continue;
            }

            if (size < capacity) {
                if (curCore.array.compareAndSet(size, null, new Just<>(element))) {
                    return curSize.getAndIncrement();
                }
                continue;
            }

            final var newCore = new Core<Value<E>>(2 * capacity);

            if (curCore.next.compareAndSet(null, newCore)) {
                for (int i = 0; i < capacity; i++) {
                    E vr;
                    while (true) {
                        final var v = curCore.array.get(i);
                        assert v == null || v instanceof Just<E>;

                        final var to = v == null ?
                                                                    new Shifted<E>() :
                                                                    new Fixed<>(((Just<E>) v).value);
                        if (curCore.array.compareAndSet(i, v, to)) {
                            vr = v == null ? null : ((Just<E>) v).value;
                            break;
                        }
                    }

                    final var it = vr == null ? null : new Just<>(vr);
                    newCore.array.compareAndSet(i, null, it);
                    curCore.array.set(i, new Shifted<>());
                }

                core.compareAndSet(curCore, newCore);
            }
        }
    }

    @Override
    public int getSize() {
        return curSize.get();
    }
}
