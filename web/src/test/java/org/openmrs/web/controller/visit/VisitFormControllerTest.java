package org.openmrs.web.controller.visit;

import junit.framework.Assert;
import org.junit.Test;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.api.context.Context;
import org.openmrs.test.Verifies;
import org.openmrs.web.test.BaseWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.Arrays;

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

/**
 * Tests against the {@link VisitFormController}
 */
public class VisitFormControllerTest extends BaseWebContextSensitiveTest {

	protected static final String VISITS_ATTRIBUTES_XML = "org/openmrs/api/include/VisitServiceTest-visitAttributes.xml";
    
    /**
     * @see {@link VisitFormController#saveVisit(WebRequest, Visit, BindingResult, SessionStatus, ModelMap)}
     */
    @Test
    @Verifies(value = "should not void or change attribute list if the attribute values are same", method = "saveVisit(Visit)")
    public void saveVisit_shouldNotVoidOrChangeAttributeListIfTheAttributeValuesAreSame() throws Exception {
        executeDataSet(VISITS_ATTRIBUTES_XML);
        Visit visit = Context.getVisitService().getVisit(1);
        VisitAttributeType visitAttributeType = Context.getVisitService().getVisitAttributeType(1);
        visitAttributeType.setName("visit type");
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.setParameter("attribute."+visitAttributeType.getId(),"2011-04-25");
        ServletWebRequest mockWebRequest = new ServletWebRequest(mockHttpServletRequest);
        BindException errors = new BindException(visit, "visit");
        VisitFormController visitFormController = (VisitFormController)applicationContext.getBean("visitFormController");
        visitFormController.saveVisit(mockWebRequest, visit, errors, null, createModelMap(visitAttributeType));
        Assert.assertFalse(visit.getVoided());
        Assert.assertEquals(1, visit.getAttributes().size());
    }

    /**
     * @see {@link VisitFormController#saveVisit(WebRequest, Visit, BindingResult, SessionStatus, ModelMap)}
     */
    @Test
    @Verifies(value = "should set attributes to voided if the value is not set", method = "saveVisit(Visit)")
    public void saveVisit_shouldSetAttributesToVoidIfTheValueIsNotSet() throws Exception {
        executeDataSet(VISITS_ATTRIBUTES_XML);
        Visit visit = Context.getVisitService().getVisit(1);
        VisitAttributeType visitAttributeType = Context.getVisitService().getVisitAttributeType(1);
        visitAttributeType.setName("visit type");
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        //If value is not set then void all the attributes.
        mockHttpServletRequest.setParameter("attribute."+visitAttributeType.getId(),"");
        ServletWebRequest mockWebRequest = new ServletWebRequest(mockHttpServletRequest);
        BindException errors = new BindException(visit, "visit");
        VisitFormController visitFormController = (VisitFormController)applicationContext.getBean("visitFormController");
        visitFormController.saveVisit(mockWebRequest, visit, errors, null, createModelMap(visitAttributeType));
        Assert.assertEquals(1,visit.getAttributes().size());
        Assert.assertTrue(((VisitAttribute) (visit.getAttributes().toArray()[0])).isVoided());
    }


    private ModelMap createModelMap(VisitAttributeType visitAttributeType) {
        ModelMap modelMap = new ModelMap();
        modelMap.put("visitAttributeTypes", Arrays.asList(visitAttributeType));
        return modelMap;
    }

}
