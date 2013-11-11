package com.amazonaws.services.route53.infima.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * An iterable sublist generator.
 * 
 * This utility class generates all possible sublists of a desired size from a
 * master list. E.g. for the input list [ "A", "B", "C", "D" ] and desired size
 * 2, this class will generate [ "A", "B" ], [ "A", "C" ], [ "A", "D" ], 
 * [ "B", "C" ], [ "B", "D" ], [ "C", "D" ].
 * 
 * @param <T>
 *            The type for the list entries
 */
public class IterableSubListGenerator<T> implements Iterable<List<T>> {

    private class SubListIterator<E> implements Iterator<List<E>> {
        private final List<E> masterList;
        private final int subListSize;
        private int[] cursors;

        public SubListIterator(List<E> masterList, int subListSize) {
            this.masterList = masterList;
            this.subListSize = subListSize;

            if (subListSize > masterList.size()) {
                throw new IllegalArgumentException("subListSize must be <= master list size. " + subListSize + " > "
                        + masterList.size());
            }

            /* Initialize the cursors */
            this.cursors = new int[subListSize];
            for (int i = 0; i < this.cursors.length; i++) {
                this.cursors[i] = i;
            }
        }

        @Override
        public boolean hasNext() {
            return cursors[0] <= (masterList.size() - cursors.length);
        }

        @Override
        public List<E> next() {
            if (hasNext() == false) {
                throw new NoSuchElementException();
            }

            List<E> subList = new ArrayList<E>(subListSize);
            for (int index : cursors) {
                subList.add(masterList.get(index));
            }

            boolean reset = false;
            for (int i = 0; i < cursors.length; i++) {
                if (reset) {
                    cursors[i] = cursors[i - 1] + 1;
                } else if (i + 1 < cursors.length && cursors[i + 1] == masterList.size() - (cursors.length - (i + 1))) {
                    cursors[i]++;
                    reset = true;
                } else if (i == cursors.length - 1) {
                    cursors[i]++;
                }
            }

            return subList;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final List<T> masterList;
    private final int subListSize;

    /**
     * Create an IterableSubListGenerator. This generator will generate all
     * possible sublists of size subListSize from the master list masterList.
     * 
     * @param masterList
     *            The master list to generate sub lists of
     * @param subListSize
     *            The size of the sublists to generate
     */
    public IterableSubListGenerator(List<T> masterList, int subListSize) {
        this.masterList = masterList;
        this.subListSize = subListSize;
    }

    /**
     * @return An iterator associated with an IterableSubListGenerator instance.
     */
    @Override
    public Iterator<List<T>> iterator() {
        return new SubListIterator<T>(masterList, subListSize);
    }

}
