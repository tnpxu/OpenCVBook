package com.example.tnpxu.opencvbook.filters;

import org.opencv.core.Mat;

public class NoneFilter implements Filter {

    @Override
    public void apply(final Mat src, final Mat dst) {
        // Do nothing.
    }
}
