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
import org.openmrs.ProviderAttribute;
import org.openmrs.api.APIException;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseContextSensitiveTest;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

public class ProviderValidatorTest extends BaseContextSensitiveTest {
	
	private Provider provider;
	
	private Errors errors;
	
	private ProviderValidator providerValidator;
	
	private ProviderService providerService;
	
	private static final String PROVIDER_ATTRIBUTE_TYPES_XML = "org/openmrs/api/include/ProviderServiceTest-providerAttributes.xml";;
	
	@Before
	public void setup() throws Exception {
		provider = new Provider();
		errors = new BindException(provider, "provider");
		providerValidator = new ProviderValidator();
		providerService = Context.getProviderService();
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies be invalid if identifier is not set
	 */
	@Test
	public void validate_shouldBeInvalidIfIdentifierIsNotSet() throws Exception {
		//given
		provider.setIdentifier(null);
		
		//when
		providerValidator.validate(provider, errors);
		
		//then
		Assert.assertTrue(errors.hasFieldErrors("identifier"));
		Assert.assertEquals("Provider.error.identifier.required", errors.getFieldError("identifier").getCode());
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies be valid if identifier is set
	 */
	@Test
	public void validate_shouldBeValidIfIdentifierIsSet() throws Exception {
		//given
		provider.setIdentifier("id");
		
		//when
		providerValidator.validate(provider, errors);
		
		//then
		Assert.assertFalse(errors.hasFieldErrors("identifier"));
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies be invalid if provider is retired and the retired reason is not mentioned
	 */
	@Test
	public void validate_shouldBeInvalidIfProviderIsRetiredAndTheRetiredReasonIsNotMentioned() throws Exception {
		provider.setIdentifier("id");
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
	 * @verifies be invalid if person or name is not set
	 */
	@Test
	public void validate_shouldBeInvalidIfPersonOrNameIsNotSet() throws Exception {
		//given
		provider.setIdentifier("id");
		provider.setPerson(null);
		provider.setName(null);
		
		//when
		providerValidator.validate(provider, errors);
		
		//then
		Assert.assertTrue(errors.hasErrors());
		Assert.assertTrue(errors.hasFieldErrors("name"));
		Assert.assertEquals("Provider.error.personOrName.required", errors.getFieldError("name").getCode());
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies be invalid if both person and name are set
	 */
	@Test
	public void validate_shouldBeInvalidIfBothPersonAndNameAreSet() throws Exception {
		//given
		provider.setIdentifier("id");
		provider.setPerson(new Person(1));
		provider.setName("1");
		
		//when
		providerValidator.validate(provider, errors);
		
		//then
		Assert.assertTrue(errors.hasErrors());
		Assert.assertTrue(errors.hasFieldErrors("name"));
		Assert.assertEquals("Provider.error.personOrName.required", errors.getFieldError("name").getCode());
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies be valid if only name is set
	 */
	@Test
	public void validate_shouldBeValidIfOnlyNameIsSet() throws Exception {
		//given
		provider.setIdentifier("id");
		provider.setName("1");
		
		//when
		providerValidator.validate(provider, errors);
		
		//then
		Assert.assertFalse(errors.hasErrors());
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies be valid if only person is set
	 */
	@Test
	public void validate_shouldBeValidIfOnlyPersonIsSet() throws Exception {
		//given
		provider.setIdentifier("id");
		provider.setPerson(new Person(1));
		
		//when
		providerValidator.validate(provider, errors);
		
		//then
		Assert.assertFalse(errors.hasErrors());
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies reject a provider if it has fewer than min occurs of an attribute
	 */
	@Test(expected = APIException.class)
	public void validate_shouldRejectAProviderIfItHasFewerThanMinOccursOfAnAttribute() throws Exception {
		executeDataSet(PROVIDER_ATTRIBUTE_TYPES_XML);
		provider.addAttribute(makeAttribute("one"));
		ValidateUtil.validate(provider);
	}
	
	/**
	 * @see ProviderValidator#validate(Object,Errors)
	 * @verifies reject a Provider if it has more than max occurs of an attribute
	 */
	@Test(expected = APIException.class)
	public void validate_shouldRejectAProviderIfItHasMoreThanMaxOccursOfAnAttribute() throws Exception {
		provider.addAttribute(makeAttribute("one"));
		provider.addAttribute(makeAttribute("two"));
		provider.addAttribute(makeAttribute("three"));
		provider.addAttribute(makeAttribute("four"));
		ValidateUtil.validate(provider);
	}
	
	private ProviderAttribute makeAttribute(String serializedValue) {
		ProviderAttribute attr = new ProviderAttribute();
		attr.setAttributeType(providerService.getProviderAttributeType(1));
		attr.setSerializedValue(serializedValue);
		return attr;
	}
}