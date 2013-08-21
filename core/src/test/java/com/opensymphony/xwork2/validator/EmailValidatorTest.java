/*
 * Copyright (c) 2002-2006 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork2.validator;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.XWorkTestCase;
import com.opensymphony.xwork2.validator.validators.EmailValidator;

/**
 * Test case for Email Validator
 * 
 * 
 * @author tm_jee
 * @version $Date: 2008-12-16 00:02:09 +0800 (星期二, 16 十二月 2008) $ $Id: EmailValidatorTest.java 1885 2008-12-15 16:02:09Z musachy $
 */
public class EmailValidatorTest extends XWorkTestCase {
	
	public void testEmailValidity() throws Exception {
		assertTrue(verifyEmailValidity("tmjee@yahoo.com"));
		assertTrue(verifyEmailValidity("tm_jee@yahoo.co"));
		assertTrue(verifyEmailValidity("tm.jee@yahoo.co.uk"));
		assertTrue(verifyEmailValidity("tm.jee@yahoo.co.biz"));
		assertTrue(verifyEmailValidity("tm_jee@yahoo.com"));
		assertTrue(verifyEmailValidity("tm_jee@yahoo.net"));
		assertTrue(verifyEmailValidity(" user@subname1.subname2.subname3.domainname.co.uk "));
        assertTrue(verifyEmailValidity("tm.j'ee@yahoo.co.uk"));
        assertTrue(verifyEmailValidity("tm.j'e.e'@yahoo.co.uk"));
        assertTrue(verifyEmailValidity("tmj'ee@yahoo.com"));
		
		assertFalse(verifyEmailValidity("tm_jee#marry@yahoo.co.uk"));
		assertFalse(verifyEmailValidity("tm_jee@ yahoo.co.uk"));
		assertFalse(verifyEmailValidity("tm_jee  @yahoo.co.uk"));
		assertFalse(verifyEmailValidity("tm_j ee  @yah oo.co.uk"));
		assertFalse(verifyEmailValidity("tm_jee  @yah oo.co.uk"));
		assertFalse(verifyEmailValidity("tm_jee @ yahoo.com"));
		assertFalse(verifyEmailValidity(" user@subname1.subname2.subname3.domainn#ame.co.uk "));
	}
	
	protected boolean verifyEmailValidity(final String email) throws Exception {
		ActionSupport action = new ActionSupport() {
			public String getMyEmail() {
				return email;
			}
		};
		
		EmailValidator validator = new EmailValidator();
		validator.setValidatorContext(new DelegatingValidatorContext(action));
		validator.setFieldName("myEmail");
		validator.setDefaultMessage("invalid email");
        validator.setValueStack(ActionContext.getContext().getValueStack());
        validator.validate(action);
		
		return (action.getFieldErrors().size() == 0);
	}
}
