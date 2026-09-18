package com.ronreynolds.android.util;

import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * limits a TextView to a maximum number of lines
 * @author Copilot (mostly)
 */
public class LimitedTextView {
    private final StringBuilder textBuffer = new StringBuilder();
    private final Deque<Integer> lineLengths = new ArrayDeque<>();
    private final int maxLines;
    private final TextView textView;
    private final ScrollView scrollView;

    public LimitedTextView(int maxLines, TextView textView, ScrollView scrollView) {
        this.maxLines = maxLines;
        this.textView = textView;
        this.scrollView = scrollView;
    }

    public void appendLine(String msg) {
        int length = msg.length() + 1;  // add 1 for \n
        textBuffer.append(msg).append('\n');
        lineLengths.addLast(length);

        // check if we've exceeded the max line-count
        if (lineLengths.size() > maxLines) {
            int oldestLength = lineLengths.removeFirst();
            // remove the oldest length from the beginning of the buffer
            textBuffer.delete(0, oldestLength);
        }

        // update the textView to reflect the new textBuffer
        textView.setText(textBuffer.toString());
        // scroll to the bottom of the view
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
