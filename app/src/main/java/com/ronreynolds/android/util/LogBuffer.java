package com.ronreynolds.android.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class LogBuffer implements Logs.LogObserver {
    private final Deque<String> lines = new ArrayDeque<>();
    private final int maxLines;

    public LogBuffer(int maxLines) {
        this.maxLines = maxLines;
    }

    @Override
    public void onLog(Logs.LogEvent event) {
        String formattedLine = event.formatLine();
        synchronized (lines) {
            lines.addLast(formattedLine);
            while (lines.size() > maxLines) {
                lines.removeFirst();
            }
        }
    }

    public List<String> getLines() {
        synchronized (lines) {
            return new ArrayList<>(lines);
        }
    }
}
