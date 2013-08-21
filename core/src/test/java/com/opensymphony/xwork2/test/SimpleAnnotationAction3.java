/*
 * Copyright (c) 2002-2006 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork2.test;

import com.opensymphony.xwork2.SimpleAnnotationAction;
import com.opensymphony.xwork2.util.Bar;


/**
 * Extend SimpleAction to test class hierarchy traversal.
 *
 * @author Mark Woon
 * @author Rainer Hermanns
 */
public class SimpleAnnotationAction3 extends SimpleAnnotationAction implements AnnotationDataAware {

    private Bar bar;
    private String data;


    public void setBarObj(Bar b) {
        bar = b;
    }

    public Bar getBarObj() {
        return bar;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
