package org.openmrs;

import junit.framework.Assert;
import org.junit.Test;

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

public class ProviderAttributeTypeTest {

    /**
     * @verifies compares as equal if names are same
     * @see ProviderAttributeType#compareTo(ProviderAttributeType)
     */
    @Test
    public void compareTo_shouldCompareAsEqualIfNamesAreSame() {
        ProviderAttributeType providerAttributeType1 = new ProviderAttributeType();
        providerAttributeType1.setName("occupation");
        providerAttributeType1.setDatatype("string");

        ProviderAttributeType providerAttributeType2 = new ProviderAttributeType();
        providerAttributeType2.setName("occupation");
        providerAttributeType2.setDatatype("string");
        Assert.assertEquals(0, providerAttributeType1.compareTo(providerAttributeType2));

    }

    /**
     * @verifies compares based on names
     * @see ProviderAttributeType#compareTo(ProviderAttributeType)
     */
    @Test
    public void compareTo_shouldCompareBasedOnNames() {
        ProviderAttributeType providerAttributeType1 = new ProviderAttributeType();
        providerAttributeType1.setName("cityOfBirth");
        providerAttributeType1.setDatatype("string");

        ProviderAttributeType providerAttributeType2 = new ProviderAttributeType();
        providerAttributeType2.setName("occupation");
        providerAttributeType2.setDatatype("string");
        Assert.assertTrue(providerAttributeType1.compareTo(providerAttributeType2) > 1);
        Assert.assertTrue(providerAttributeType2.compareTo(providerAttributeType1) < 1);
    }

}
