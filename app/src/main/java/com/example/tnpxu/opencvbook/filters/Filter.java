package com.example.tnpxu.opencvbook.filters;

import org.opencv.core.Mat;

public interface Filter {
    public abstract void apply(final Mat src, final Mat dst);
}
