package ru.ifmo.ctddev.shatrov.arrayset;

import java.util.*;

/**
 * Created by vi34 on 25.02.15.
 */
public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private List<T> array;
    private Comparator<T> comparator;
    private boolean naturalOrdering = false;
    private boolean reversed = false;

    public ArraySet() {
        array = new ArrayList<T>();
        naturalOrdering = true;
        comparator = (Comparator<T>) Comparator.naturalOrder();
    }

    public ArraySet(Collection<T> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<T> collection, Comparator<? super T> comp) {
        if (collection != null) {
            if (comp != null) {
                this.comparator = (Comparator<T>) comp;
            } else {
                comparator = (Comparator<T>) Comparator.naturalOrder();
                naturalOrdering = true;
            }
            if (collection.size() == 0) {
                array = new ArrayList<T>();
                return;
            }
            ArrayList<T> tmp = new ArrayList<>(collection);
            int same = 0;
            Collections.sort(tmp,comparator);
            for (int i = 0; i < tmp.size() - 1; ++i) {
                if (comparator.compare(tmp.get(i + 1), tmp.get(i)) == 0) {
                    same++;
                }
            }
            array = new ArrayList<T>(tmp.size() - same);
            for (int i = 0; i < tmp.size() - 1; ++i) {
                if (comparator.compare(tmp.get(i + 1), tmp.get(i)) != 0) {
                    array.add(tmp.get(i));
                }
            }
            if (tmp.size() == 1 || !(comparator.compare(tmp.get(tmp.size() - 1),tmp.get(tmp.size() - 2)) == 0)) {
                array.add(tmp.get(tmp.size() - 1));
            }
            if (array.size() == 0) {
                array.add(tmp.get(0));
            }
        }
    }

    private ArraySet(List<T> array, Comparator<T> comp, boolean naturalOrdering, boolean reversed) {
        this.array = array;
        this.comparator = comp;
        this.naturalOrdering = naturalOrdering;
        this.reversed = reversed;

    }

    private Integer lowerAndHigherInd(T t, boolean lower) {
        Comparator comp = comparator;
        if (reversed) {
            comp = comparator.reversed();
            lower = !lower;
        }
        int correction = 2;
        if (lower) {
            correction = 0;
        }
        int ind = Collections.binarySearch(array, t, comp);
        if (ind >= 0 && ((ind > 0 && lower) || (ind < array.size() - 1 && !lower))) {
            return ind - 1 + correction;
        }
        if ((ind == 0 - 1 || ind == 0) && lower || (ind == array.size() - 1 || ind == -array.size() - 1) && !lower) {
            return null;
        }
        if (!lower) {
            correction = 1;
        }
        return -ind - 2 + correction;
    }

    private Integer floorAndCeilingInd(T t, boolean floor) {
        Comparator comp = comparator;
        if (reversed) {
            comp = comparator.reversed();
            floor = !floor;
        }
        int correction = 1;
        if (floor) {
            correction = 2;
        }
        int ind = Collections.binarySearch(array, t, comp);
        if (ind >= 0) {
            return ind;
        }
        if ((ind == -1 && floor) || (ind == -array.size() - 1 && !floor)) {
            return null;
        }
        return -ind - correction;
    }

    private T wrapInd(Integer ind) {
        if (ind == null) {
            return null;
        }
        return array.get(ind);
    }

    @Override
    public T lower(T t) {
        return wrapInd(lowerAndHigherInd(t, true));
    }

    @Override
    public T floor(T t) {
        return wrapInd(floorAndCeilingInd(t, true));
    }

    @Override
    public T ceiling(T t) {
        return wrapInd(floorAndCeilingInd(t, false));
    }

    @Override
    public T higher(T t) {
        return wrapInd(lowerAndHigherInd(t, false));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return array.size();
    }


    private Iterator<T> makeIterator(boolean rev) {
        return new Iterator<T>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                if (rev) {
                    return (index < array.size());
                }
                return (array.size() - 1 - index >= 0);
            }

            @Override
            public T next() {
                if (hasNext()) {
                    if (rev) {
                        return array.get(index++);
                    }
                    return array.get(array.size() - 1 - index++);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    public Iterator<T> iterator() {
        return makeIterator(!reversed);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        int ind = Collections.binarySearch(array, (T) o, comparator);
        return ind >= 0;
    }

    @Override
    public NavigableSet<T> descendingSet() {
        return new ArraySet<T>(array, comparator.reversed(), naturalOrdering, !reversed);
    }

    @Override
    public Iterator<T> descendingIterator() {
        return makeIterator(reversed);
    }

    @Override
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {

        if (comparator.compare(fromElement, toElement) > 0) {
            return new ArraySet<T>(array.subList(0, 0), comparator, naturalOrdering, reversed);
        }
        Integer indFrom, indTo;
        if (fromInclusive) {
            indFrom = floorAndCeilingInd(fromElement, false);
        } else {
            indFrom = lowerAndHigherInd(fromElement, false);
        }
        if (toInclusive) {
            indTo = floorAndCeilingInd(toElement, true);
        } else {
            indTo = lowerAndHigherInd(toElement, true);
        }
        if (reversed && indFrom > indTo) {
            Integer tmp = indFrom;
            indFrom = indTo;
            indTo = tmp;
        }

        if (indFrom == null || indTo == null || indFrom > indTo) {
            return new ArraySet<T>(array.subList(0, 0), comparator, naturalOrdering, reversed);
        }

        return new ArraySet<T>(array.subList(indFrom, indTo + 1), comparator, naturalOrdering, reversed);
    }

    @Override
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        if (size() == 0) {
            return this;
        }
        return subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        if (size() == 0) {
            return this;
        }
        return subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super T> comparator() {
        if (naturalOrdering) {
            return null;
        }
        return comparator;
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public T first() {
        return edges(!reversed);
    }

    @Override
    public T last() {
        return edges(reversed);
    }

    private T edges(boolean rev) {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        if (rev) {
            return array.get(0);
        }
        return array.get(array.size() - 1);
    }
}
