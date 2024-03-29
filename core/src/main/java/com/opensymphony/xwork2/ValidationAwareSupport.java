/*
 * Copyright (c) 2002-2006 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork2;

import java.io.Serializable;
import java.util.*;

/**
 * Provides a default implementation of ValidationAware. Returns new collections for
 * errors and messages (defensive copy).
 *
 * @author Jason Carreira
 * @author tm_jee
 * @version $Date: 2008-06-21 17:29:39 +0800 (星期六, 21 六月 2008) $ $Id: ValidationAwareSupport.java 1833 2008-06-21 09:29:39Z rainerh $
 */
public class ValidationAwareSupport implements ValidationAware, Serializable {

    private Collection<String> actionErrors;
    private Collection<String> actionMessages;
    private Map<String, List<String>> fieldErrors;


    public synchronized void setActionErrors(Collection<String> errorMessages) {
        this.actionErrors = errorMessages;
    }

    public synchronized Collection<String> getActionErrors() {
        return new ArrayList<String>(internalGetActionErrors());
    }

    public synchronized void setActionMessages(Collection<String> messages) {
        this.actionMessages = messages;
    }

    public synchronized Collection<String> getActionMessages() {
        return new ArrayList<String>(internalGetActionMessages());
    }

    public synchronized void setFieldErrors(Map<String, List<String>> errorMap) {
        this.fieldErrors = errorMap;
    }

    public synchronized Map<String, List<String>> getFieldErrors() {
        return new LinkedHashMap<String, List<String>>(internalGetFieldErrors());
    }

    public synchronized void addActionError(String anErrorMessage) {
        internalGetActionErrors().add(anErrorMessage);
    }

    public synchronized void addActionMessage(String aMessage) {
        internalGetActionMessages().add(aMessage);
    }

    public synchronized void addFieldError(String fieldName, String errorMessage) {
        final Map<String, List<String>> errors = internalGetFieldErrors();
        List<String> thisFieldErrors = errors.get(fieldName);

        if (thisFieldErrors == null) {
            thisFieldErrors = new ArrayList<String>();
            errors.put(fieldName, thisFieldErrors);
        }

        thisFieldErrors.add(errorMessage);
    }

    public synchronized boolean hasActionErrors() {
        return (actionErrors != null) && !actionErrors.isEmpty();
    }

    public synchronized boolean hasActionMessages() {
        return (actionMessages != null) && !actionMessages.isEmpty();
    }

    public synchronized boolean hasErrors() {
        return (hasActionErrors() || hasFieldErrors());
    }

    public synchronized boolean hasFieldErrors() {
        return (fieldErrors != null) && !fieldErrors.isEmpty();
    }

    private Collection<String> internalGetActionErrors() {
        if (actionErrors == null) {
            actionErrors = new ArrayList<String>();
        }

        return actionErrors;
    }

    private Collection<String> internalGetActionMessages() {
        if (actionMessages == null) {
            actionMessages = new ArrayList<String>();
        }

        return actionMessages;
    }

    private Map<String, List<String>> internalGetFieldErrors() {
        if (fieldErrors == null) {
            fieldErrors = new LinkedHashMap<String, List<String>>();
        }

        return fieldErrors;
    }

    /**
     * Clears field errors map.
     * <p/>
     * Will clear the map that contains field errors.
     */
    public synchronized void clearFieldErrors() {
        internalGetFieldErrors().clear();
    }

    /**
     * Clears action errors list.
     * <p/>
     * Will clear the list that contains action errors.
     */
    public synchronized void clearActionErrors() {
        internalGetActionErrors().clear();
    }

    /**
     * Clears messages list.
     * <p/>
     * Will clear the list that contains action messages.
     */
    public synchronized void clearMessages() {
        internalGetActionMessages().clear();
    }

    /**
     * Clears all error list/maps.
     * <p/>
     * Will clear the map and list that contain
     * field errors and action errors.
     */
    public synchronized void clearErrors() {
        internalGetFieldErrors().clear();
        internalGetActionErrors().clear();
    }

    /**
     * Clears all error and messages list/maps.
     * <p/>
     * Will clear the maps/lists that contain
     * field errors, action errors and action messages.
     */
    public synchronized void clearErrorsAndMessages() {
        internalGetFieldErrors().clear();
        internalGetActionErrors().clear();
        internalGetActionMessages().clear();
    }
}
