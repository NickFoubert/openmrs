package org.openmrs;

import junit.framework.Assert;
import org.junit.Test;
import org.openmrs.attribute.AttributeType;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 * <p/>
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 * <p/>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
public class ProviderAttributeTest {
	
	/**
	 * @verifies compare as equal for same attribute type names
	 * @see ProviderAttribute#compareTo(ProviderAttribute)
	 */
	@Test
	public void compareTo_shouldCompareAsEqualForSameAttributeTypeNames() throws Exception {
		ProviderAttributeType providerAttributeType = new ProviderAttributeType();
		providerAttributeType.setName("occupation");
		providerAttributeType.setDatatype("string");
		ProviderAttribute providerAttribute1 = createProviderAttribute(providerAttributeType, new SimpleDateFormat(
		        "yyyy-MM-dd").parse("2011-04-25"));
		ProviderAttribute providerAttribute2 = createProviderAttribute(providerAttributeType, new SimpleDateFormat(
		        "yyyy-MM-dd").parse("2011-04-25"));
		Assert.assertEquals(0, providerAttribute1.compareTo(providerAttribute2));
	}
	
	/**
	 * @verifies compare based on attribute type
	 * @see ProviderAttribute#compareTo(ProviderAttribute)
	 */
	@Test
	public void compareTo_shouldCompareBasedOnAttributeType() throws Exception {
		ProviderAttributeType providerAttributeType = new ProviderAttributeType();
		providerAttributeType.setName("occupation");
		providerAttributeType.setDatatype("string");
		ProviderAttribute providerAttribute1 = createProviderAttribute(providerAttributeType, new SimpleDateFormat(
		        "yyyy-MM-dd").parse("2011-04-25"));
		
		ProviderAttributeType providerAttributeType2 = new ProviderAttributeType();
		providerAttributeType2.setName("place");
		providerAttributeType2.setDatatype("string");
		ProviderAttribute providerAttribute2 = createProviderAttribute(providerAttributeType2, "bangalore");
		
		Assert.assertTrue(providerAttribute1.compareTo(providerAttribute2) > 0);
		Assert.assertTrue(providerAttribute2.compareTo(providerAttribute1) < 0);
	}
	
	/**
	 * @verifies compare based on serialized value when attribute types are same
	 * @see ProviderAttribute#compareTo(ProviderAttribute)
	 */
	@Test
	public void compareTo_shouldCompareBasedOnSerializedValueWhenAttributeTypesAreSame() throws Exception {
		ProviderAttributeType providerAttributeType = new ProviderAttributeType();
		providerAttributeType.setName("occupation");
		providerAttributeType.setDatatype("string");
		ProviderAttribute providerAttribute1 = createProviderAttribute(providerAttributeType, new SimpleDateFormat(
		        "yyyy-MM-dd").parse("2011-04-25"));
		ProviderAttribute providerAttribute2 = createProviderAttribute(providerAttributeType, new SimpleDateFormat(
		        "yyyy-MM-dd").parse("2012-04-25"));
		Assert.assertTrue(providerAttribute1.compareTo(providerAttribute2) < 0);
		Assert.assertTrue(providerAttribute2.compareTo(providerAttribute1) > 0);
	}
	
	/**
	 * @verifies return negative if other type is null
	 * @see ProviderAttribute#compareTo(ProviderAttribute)
	 */
	@Test
	public void compareTo_shouldReturnNegativeIfOtherTypeIsNull() throws Exception {
		ProviderAttributeType providerAttributeType = new ProviderAttributeType();
		providerAttributeType.setName("occupation");
		providerAttributeType.setDatatype("string");
		ProviderAttribute providerAttribute1 = createProviderAttribute(providerAttributeType, new SimpleDateFormat(
		        "yyyy-MM-dd").parse("2011-04-25"));
		Assert.assertTrue(providerAttribute1.compareTo(null) < 0);
	}
	
	private ProviderAttribute createProviderAttribute(AttributeType<Provider> providerAttributeType, Object value)
	        throws Exception {
		ProviderAttribute providerAttribute = new ProviderAttribute();
		providerAttribute.setAttributeType(providerAttributeType);
		providerAttribute.setSerializedValue(value.toString());
		providerAttribute.setDateCreated(new Date());
		return providerAttribute;
	}
	
}
