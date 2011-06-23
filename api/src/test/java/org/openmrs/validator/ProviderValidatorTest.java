/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.validator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

public class ProviderValidatorTest {
	
	private Provider provider;
	
	private Errors errors;
	
	private ProviderValidator providerValidator;
	
	@Before
	public void setup() {
		provider = new Provider();
		errors = new BindException(provider, "provider");
		providerValidator = new ProviderValidator();
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies be invalid if provider is retired and the retired reason is not mentioned
	 */
	@Test
	public void validate_shouldBeInvalidIfProviderIsRetiredAndTheRetiredReasonIsNotMentioned() throws Exception {
		provider.setRetired(true);
		provider.setPerson(new Person());
		
		providerValidator.validate(provider, errors);
		
		Assert.assertTrue(errors.hasErrors());
		Assert.assertTrue(errors.hasFieldErrors("retireReason"));
		Assert.assertEquals("Provider.error.retireReason.required", errors.getFieldError("retireReason").getCode());
		
		errors = new BindException(provider, "provider");
		provider.setRetireReason("getting old..");
		
		providerValidator.validate(provider, errors);
		
		Assert.assertFalse(errors.hasErrors());
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies be invalid if person is not set
	 */
	@Test
	public void validate_shouldBeInvalidIfPersonIsNotSet() throws Exception {
		providerValidator.validate(provider, errors);
		
		Assert.assertTrue(errors.hasErrors());
		Assert.assertTrue(errors.hasFieldErrors("person"));
		Assert.assertEquals("Provider.error.person.required", errors.getFieldError("person").getCode());
		
		errors = new BindException(provider, "provider");
		
		provider.setPerson(new Person(1));
		providerValidator.validate(provider, errors);
		
		Assert.assertFalse(errors.hasErrors());
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies be invalid if provider details are not set
	 */
	@Test
	public void validate_shouldBeInvalidIfProviderDetailsAreNotSet() throws Exception {
		providerValidator.validate(provider, errors);
		
		Assert.assertTrue(errors.hasErrors());
		Assert.assertTrue(errors.hasFieldErrors("person"));
		Assert.assertEquals("Provider.error.person.required", errors.getFieldError("person").getCode());
		
		errors = new BindException(provider, "provider");
		
		provider.setName("Provider");
		providerValidator.validate(provider, errors);
		
		Assert.assertTrue(errors.hasErrors());
		Assert.assertEquals("Provider.error.name.identifier.required", errors.getFieldError("name").getCode());
		
		providerValidator.validate(provider, errors);
		
		errors = new BindException(provider, "provider");
		provider.setIdentifier("identifier");
		provider.setName(null);
		providerValidator.validate(provider, errors);
		
		Assert.assertTrue(errors.hasErrors());
		Assert.assertEquals("Provider.error.name.identifier.required", errors.getFieldError("name").getCode());
		
		errors = new BindException(provider, "provider");
		provider.setIdentifier("identifier");
		provider.setName("name");
		providerValidator.validate(provider, errors);
		
		Assert.assertFalse(errors.hasErrors());
		
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies be invalid if both provider details and person are set
	 */
	@Test
	public void validate_shouldBeInvalidIfBothProviderDetailsAndPersonAreSet() throws Exception {
		provider.setPerson(new Person(1));
		provider.setName("provider name");
		
		providerValidator.validate(provider, errors);
		
		Assert.assertTrue(errors.hasErrors());
		Assert.assertEquals("Provider.error.person.required", errors.getFieldError("person").getCode());
		
		errors = new BindException(provider, "provider");
		
		provider.setName(null);
		provider.setIdentifier("identifier");
		providerValidator.validate(provider, errors);
		
		Assert.assertTrue(errors.hasErrors());
		Assert.assertEquals("Provider.error.person.required", errors.getFieldError("person").getCode());
	}
	
}
