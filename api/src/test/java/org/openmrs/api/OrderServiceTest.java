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
package org.openmrs.api;

import java.util.Date;
import java.util.Calendar;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseContextSensitiveTest;
import org.openmrs.test.Verifies;

/**
 * TODO clean up and test all methods in OrderService
 */
public class OrderServiceTest extends BaseContextSensitiveTest {
	
	/**
	 * @see {@link OrderService#saveOrder(Order)}
	 */
	@Test(expected = APIException.class)
	@Verifies(value = "should not save order if order doesnt validate", method = "saveOrder(Order)")
	public void saveOrder_shouldNotSaveOrderIfOrderDoesntValidate() throws Exception {
		OrderService orderService = Context.getOrderService();
		Order order = new Order();
		order.setPatient(null);
		orderService.saveOrder(order);
	}
	
	/**
	 * @see {@link OrderService#getOrderByUuid(String)}
	 * 
	 */
	@Test
	@Verifies(value = "should find object given valid uuid", method = "getOrderByUuid(String)")
	public void getOrderByUuid_shouldFindObjectGivenValidUuid() throws Exception {
		String uuid = "921de0a3-05c4-444a-be03-e01b4c4b9142";
		Order order = Context.getOrderService().getOrderByUuid(uuid);
		Assert.assertEquals(1, (int) order.getOrderId());
	}
	
	/**
	 * @see {@link OrderService#getOrderByUuid(String)}
	 * 
	 */
	@Test
	@Verifies(value = "should return null if no object found with given uuid", method = "getOrderByUuid(String)")
	public void getOrderByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() throws Exception {
		Assert.assertNull(Context.getOrderService().getOrderByUuid("some invalid uuid"));
	}
	
	/**
	 * @see {@link OrderService#getOrderTypeByUuid(String)}
	 * 
	 */
	@Test
	@Verifies(value = "should find object given valid uuid", method = "getOrderTypeByUuid(String)")
	public void getOrderTypeByUuid_shouldFindObjectGivenValidUuid() throws Exception {
		String uuid = "84ce45a8-5e7c-48f7-a581-ca1d17d63a62";
		OrderType orderType = Context.getOrderService().getOrderTypeByUuid(uuid);
		Assert.assertEquals(1, (int) orderType.getOrderTypeId());
	}
	
	/**
	 * @see {@link OrderService#getOrderTypeByUuid(String)}
	 * 
	 */
	@Test
	@Verifies(value = "should return null if no object found with given uuid", method = "getOrderTypeByUuid(String)")
	public void getOrderTypeByUuid_shouldReturnNullIfNoObjectFoundWithGivenUuid() throws Exception {
		Assert.assertNull(Context.getOrderService().getOrderTypeByUuid("some invalid uuid"));
	}
	
	/**
	 * @see {@link OrderService#saveOrder(Order)}
	 */
	@Test
	@Verifies(value = "when saving a discontinuedReasonNonCoded parameter the value is correctly stored to the database", method = "saveOrder(Order)")
	public void saveOrder_shouldSaveDiscontinuedReasonNonCoded() throws Exception {
		String uuid = "921de0a3-05c4-444a-be03-e01b4c4b9142";
		Order order = Context.getOrderService().getOrderByUuid(uuid);
		String discontinuedReasonNonCoded = "Non coded discontinued reason";
		
		order.setDiscontinuedReasonNonCoded(discontinuedReasonNonCoded);
		OrderService orderService = Context.getOrderService();
		orderService.saveOrder(order);
		
		order = Context.getOrderService().getOrderByUuid(uuid);
		
		Assert.assertEquals(discontinuedReasonNonCoded, order.getDiscontinuedReasonNonCoded());
	}
	
	/**
	 * @see {@link OrderService#discontinueOrder(Order, Concept, Date)}
	 */
	@Test
	@Verifies(value = "should set discontinuedDate if the discontinue date is not in future", method = "discontinueOrder(Order order, Concept discontinueReason, Date discontinueDate)")
	public void discontinueOrder_shouldSetDiscontinuedDateIfTheDiscontinueDateIsNotInFuture() throws Exception {
		String uuid = "921de0a3-05c4-444a-be03-e01b4c4b9142";
		Order order = Context.getOrderService().getOrderByUuid(uuid);
		Concept discontinudReason = Context.getConceptService().getConcept(1107);
		Date discontinueDate = new Date();
		
		Order updatedOrder = Context.getOrderService().discontinueOrder(order, discontinudReason, discontinueDate);
		
		Assert.assertEquals(discontinueDate, updatedOrder.getDiscontinuedDate());
		
	}
	
	/**
	 * @see {@link OrderService#discontinueOrder(Order, Concept, Date)}
	 */
	@Test
	@Verifies(value = "should set autoExpireDate if the discontinue date is in future", method = "discontinueOrder(Order order, Concept discontinueReason, Date discontinueDate)")
	public void discontinueOrder_shouldSetAutoExpireDateIfTheDiscontinueDateIsInFuture() throws Exception {
		String uuid = "921de0a3-05c4-444a-be03-e01b4c4b9142";
		Order order = Context.getOrderService().getOrderByUuid(uuid);
		Concept discontinudReason = Context.getConceptService().getConcept(1107);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 10);
		Date autoExpireDate = cal.getTime();
		Order updatedOrder = Context.getOrderService().discontinueOrder(order, discontinudReason, autoExpireDate);
		
		Assert.assertEquals(autoExpireDate, updatedOrder.getAutoExpireDate());
		
	}
	
	@Test
	public void voidDrugSet_shouldNotVoidThePatient() throws Exception {
		Patient p = Context.getPatientService().getPatient(2);
		Assert.assertFalse(p.isVoided());
		Context.getOrderService().voidDrugSet(p, "1", "Reason", OrderService.SHOW_ALL);
		Assert.assertFalse(p.isVoided());
	}
	
}
