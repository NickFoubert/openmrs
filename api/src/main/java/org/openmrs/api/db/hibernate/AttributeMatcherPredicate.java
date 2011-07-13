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
package org.openmrs.api.db.hibernate;

import org.apache.commons.collections.Predicate;
import org.openmrs.attribute.Attribute;
import org.openmrs.attribute.AttributeType;
import org.openmrs.attribute.Customizable;

import java.util.Map;

/**
 * A predicate for matching attribute values
 * @since 1.9
 */
public class AttributeMatcherPredicate<T extends Customizable> implements Predicate {
	
	private final Map<AttributeType, String> serializedAttributeValues;
	
	public AttributeMatcherPredicate(Map<AttributeType, String> serializedAttributeValues) {
		this.serializedAttributeValues = serializedAttributeValues;
	}
	
	@Override
	public boolean evaluate(Object o) {
		final T customizable = (T) o;
		for (Map.Entry<AttributeType, String> entry : serializedAttributeValues.entrySet()) {
			for (Object attr : customizable.getActiveAttributes(entry.getKey())) {
				Attribute attribute = (Attribute) attr;
				if (attribute.getSerializedValue().equals(entry.getValue())) {
					return true;
				}
			}
		}
		return false;
	}
}