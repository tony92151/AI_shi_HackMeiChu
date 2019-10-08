/*
 * Copyright Notice
 * Copyright © 2019 VIA Technologies Inc. All Rights Reserved. No part of this document may be
 * reproduced, transmitted, transcribed, stored in a retrieval system, or translated into any language, in
 * any form or by any means, electronic, mechanical, magnetic, optical, chemical, manual or otherwise
 * without the prior written permission of VIA Technologies Inc. The material in this document is for
 * information only and is subject to change without notice. VIA Technologies Inc. reserves the right to
 * make changes in the product design without reservation and without notice to its users.
 *
 * Trademarks
 * A920 and AltaDs3 may only be used to identify products of VIA Technologies, Inc.
 * All trademarks are the properties of their prospective owners.
 */

package com.example.via_hack;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceView;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by damon on 1/29/18.
 */

public class AutoFitSurfaceView extends SurfaceView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    private final List<DrawCallback> callbacks = new LinkedList<DrawCallback>();

    public AutoFitSurfaceView(Context context) {
        super(context);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    /**
     * Interface defining the callback for client classes.
     */
    public interface DrawCallback {
        public void drawCallback(final Canvas canvas);
    }

    public void addCallback(final DrawCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public synchronized void onDraw(final Canvas canvas) {
        super.draw(canvas);
        for (final DrawCallback callback : callbacks) {
            callback.drawCallback(canvas);
        }
    }
}
