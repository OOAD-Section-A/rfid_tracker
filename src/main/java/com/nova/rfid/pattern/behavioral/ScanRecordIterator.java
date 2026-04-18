package com.nova.rfid.pattern.behavioral;

import com.nova.rfid.model.ScanRecord;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * BEHAVIORAL PATTERN: ITERATOR
 *
 * Provides a standardised way to traverse scan transaction records
 * without exposing the underlying List implementation.
 * Useful when Reporting Subsystem (Sub. 5) iterates our logs.
 *
 * GRASP - Information Expert: Knows how to iterate its own collection.
 * SOLID - ISP: Consumers only depend on Iterator<ScanRecord>, not List.
 */
public class ScanRecordIterator implements Iterator<ScanRecord> {

    private final List<ScanRecord> records;
    private int currentIndex = 0;

    public ScanRecordIterator(List<ScanRecord> records) {
        this.records = records;
    }

    @Override
    public boolean hasNext() {
        return currentIndex < records.size();
    }

    @Override
    public ScanRecord next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more scan records.");
        }
        return records.get(currentIndex++);
    }

    /** Reset to beginning (for re-iteration). */
    public void reset() {
        currentIndex = 0;
    }

    /** Total count. */
    public int size() {
        return records.size();
    }
}
