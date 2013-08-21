/*
 * Copyright (c) 2002-2006 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork2.test;

import com.opensymphony.xwork2.validator.annotations.ExpressionValidator;
import com.opensymphony.xwork2.validator.annotations.RequiredFieldValidator;
import com.opensymphony.xwork2.validator.annotations.Validation;
import com.opensymphony.xwork2.validator.annotations.Validations;

/**
 * Marker interface to help test hierarchy traversal.
 *
 * @author Mark Woon
 * @author Rainer Hermanns
 */
@Validation(
        validations = @Validations(
                requiredFields = {
                    @RequiredFieldValidator(fieldName = "email", shortCircuit = true, message = "You must enter a value for email."),
                    @RequiredFieldValidator(fieldName = "email2", shortCircuit = true, message = "You must enter a value for email2.")
                },
                expressions = {
                        @ExpressionValidator(shortCircuit = true, expression = "email.equals(email2)", message = "Email not the same as email2" )
                }
        )
)
public interface AnnotationUserMarker {
}
