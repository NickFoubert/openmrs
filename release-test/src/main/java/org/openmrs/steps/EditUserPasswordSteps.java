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
package org.openmrs.steps;

import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.openmrs.Steps;
import org.openqa.selenium.WebDriver;

import static org.hamcrest.Matchers.equalTo;
import static org.openqa.selenium.lift.Finders.*;
import static org.openqa.selenium.lift.Matchers.attribute;
import static org.openqa.selenium.lift.Matchers.text;

public class EditUserPasswordSteps extends Steps {

	public EditUserPasswordSteps(WebDriver driver) {
		super(driver);
	}

	@Given("I login to the openmrs application with username $username and password $password")
	public void logIn(String username, String password) {
		type(username, into(textbox().with(attribute("id", equalTo("username")))));
		type(password, into(passwordtextbox().with(attribute("id", equalTo("password")))));
		clickOn(button());
	}

	@Given("I navigate to the the administration page")
	public void navigateToAdminUrl() {
		clickOn(link().with(text(equalTo("Administration"))));
	}

	@When("I click on the Manage Users")
	public void navigateToManageUsersUrl() {
		clickOn(link().with(text(equalTo("Manage Users"))));
	}

	@When("I search for user $name")
	public void searchUser(String name) {
		type(name, into(textbox().with(attribute("name", equalTo("name")))));
		clickOn(button().with(attribute("name", equalTo("action"))));
	}

	@When("I chose to edit the user")
	public void editUser() {
		clickOn(link().with(text(equalTo("6-7"))));
		//String userXpath = "id('content')/x:div[2]/x:table/x:tbody/x:tr[1]/x:td[1]/x:a"; //html/body/div/div[3]/div[3]/table/tbody/tr/td/a
		//waitFor(finderByXpath(userXpath));
		//clickOn(finderByXpath(userXpath));
	}

	@When("I changed the $password, $confirmPassword")
	public void editPassword(String password, String confirmPassword) {
		type(password, into(passwordtextbox().with(attribute("name", equalTo("userFormPassword")))));
		type(confirmPassword, into(passwordtextbox().with(attribute("name", equalTo("confirm")))));
	}

	@When("I save the user")
	public void save() {
		clickOn(button().with(attribute("id", equalTo("saveButton"))));
	}

	@Then("the user's password should be changed")
	public void verifyUser() {
		assertPresenceOf(div().with(text(equalTo("User Saved"))));
	}
}
