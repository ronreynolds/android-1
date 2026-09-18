package com.ronreynolds.android.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * ArrayList using WeakReference<T> to prevent the list being the ONLY reason it's keeping its
 * contents from being GCed.
 *
 * @param <T> the type stored within an instance of this List
 * @author copilot (honestly i just copy-pasted this one)
 */
public class WeakArrayList<T> implements List<T> {

    private final List<WeakReference<T>> backing = new ArrayList<>();

    // --- internal helper ---
    private void clean() {
        Iterator<WeakReference<T>> it = backing.iterator();
        while (it.hasNext()) {
            if (it.next().get() == null) {
                it.remove();
            }
        }
    }

    private T unwrap(int index) {
        WeakReference<T> ref = backing.get(index);
        T val = ref.get();
        if (val == null) {
            backing.remove(index);
        }
        return val;
    }

    // --- List<T> implementation ---

    @Override
    public int size() {
        clean();
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        clean();
        return backing.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        clean();
        for (WeakReference<T> ref : backing) {
            T val = ref.get();
            if (val != null && val.equals(o)) return true;
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        clean();
        Iterator<WeakReference<T>> it = backing.iterator();
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                WeakReference<T> ref = it.next();
                T val = ref.get();
                if (val == null) {
                    it.remove();
                    return next();
                }
                return val;
            }
        };
    }

    @Override
    public Object[] toArray() {
        clean();
        List<T> list = new ArrayList<>();
        for (WeakReference<T> ref : backing) {
            T val = ref.get();
            if (val != null) list.add(val);
        }
        return list.toArray();
    }

    @Override
    public <U> U[] toArray(U[] a) {
        clean();
        List<T> list = new ArrayList<>();
        for (WeakReference<T> ref : backing) {
            T val = ref.get();
            if (val != null) list.add(val);
        }
        return list.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return backing.add(new WeakReference<>(t));
    }

    @Override
    public boolean remove(Object o) {
        Iterator<WeakReference<T>> it = backing.iterator();
        while (it.hasNext()) {
            T val = it.next().get();
            if (val == null || val.equals(o)) {
                it.remove();
                if (val != null) return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        clean();
        for (Object o : c) {
            if (!contains(o)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean changed = false;
        for (T t : c) {
            changed |= add(t);
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        int i = index;
        for (T t : c) {
            backing.add(i++, new WeakReference<>(t));
        }
        return !c.isEmpty();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            changed |= remove(o);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        Iterator<WeakReference<T>> it = backing.iterator();
        while (it.hasNext()) {
            T val = it.next().get();
            if (val == null || !c.contains(val)) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        backing.clear();
    }

    @Override
    public T get(int index) {
        return unwrap(index);
    }

    @Override
    public T set(int index, T element) {
        T old = unwrap(index);
        backing.set(index, new WeakReference<>(element));
        return old;
    }

    @Override
    public void add(int index, T element) {
        backing.add(index, new WeakReference<>(element));
    }

    @Override
    public T remove(int index) {
        T old = unwrap(index);
        backing.remove(index);
        return old;
    }

    @Override
    public int indexOf(Object o) {
        clean();
        for (int i = 0; i < backing.size(); i++) {
            T val = backing.get(i).get();
            if (val != null && val.equals(o)) return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        clean();
        for (int i = backing.size() - 1; i >= 0; i--) {
            T val = backing.get(i).get();
            if (val != null && val.equals(o)) return i;
        }
        return -1;
    }

    @Override
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        clean();
        List<T> snapshot = new ArrayList<>();
        for (WeakReference<T> ref : backing) {
            T val = ref.get();
            if (val != null) snapshot.add(val);
        }
        return snapshot.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        clean();
        List<T> snapshot = new ArrayList<>();
        for (WeakReference<T> ref : backing) {
            T val = ref.get();
            if (val != null) snapshot.add(val);
        }
        return snapshot.subList(fromIndex, toIndex);
    }
}