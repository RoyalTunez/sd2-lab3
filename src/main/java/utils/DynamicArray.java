package utils;

public interface DynamicArray<E> {
    E get(int index);
    void put(int index, E element);
    int pushBack(E element);
    int getSize();
}
