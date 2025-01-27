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

import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.junit.Assert;
import org.openmrs.Steps;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.lift.Finders;

import static org.hamcrest.Matchers.equalTo;
import static org.openqa.selenium.lift.Finders.*;
import static org.openqa.selenium.lift.Matchers.attribute;
import static org.openqa.selenium.lift.Matchers.text;

public class EditAConceptSteps extends Steps {
	
	public EditAConceptSteps(WebDriver driver) {
		super(driver);
	}
	
	@When("I navigate to the $dictionary page")
	public void navigateToDictonaryPage(String dictionaryTitle) {
		clickOn(link().with(text(equalTo(dictionaryTitle))));
		assertPresenceOf(title().with(text(equalTo("OpenMRS - " + dictionaryTitle))));
	}
	
	@When("I search for a concept by typing $aspirin and wait for the search hits")
	public void searchForAConceptAndWaitForTheHits(String phrase) {
		type(phrase, into(textbox().with(attribute("id", equalTo("inputNode")))));
		waitFor(Finders.table().with(attribute("id", equalTo("openmrsSearchTable"))));
	}
	
	@When("I select $aspirin from the hits")
	public void takeMeToCreateNewConceptPage(String selection) {
		clickOn(finderByXpath("//table[@id='openmrsSearchTable']/tbody/tr[@class='odd']/td/span[starts-with(text(),'" + selection + "')]"));
	}
	
	@Then("Take me to the viewing concept page")
	public void takeMeToViewingConceptPage() {
		Assert.assertTrue("Page title was expected to start with 'OpenMRS - Viewing Concept' but was:" + title(), getTitle()
		        .startsWith("OpenMRS - Viewing Concept"));
	}
	

	@When("I change the fully specified name to $aspirin")
	public void editTheFullySpecifiedName(String newName) {
		type(random(newName), into(textbox().with(attribute("id", equalTo("namesByLocale[en].name")))));
	}
	
	@When("I edit the synonym")
	public void editTheSynonymName() {
		type(random("syn"), into(textbox().with(attribute("name", equalTo("synonymsByLocale[en][0].name")))));
	}
	@When("I click on Add Search Term")
    public void clickOnAddSearchTerm(){
        getWebDriver().findElement(By.id("addSearch")).click();
    }
	@When("I edit the index term name")
	public void changeSearchTermName() {
		type(random("term"), into(textbox().with(attribute("name", equalTo("indexTermsByLocale[en][0].name")))));
	}
	
	@When("I edit the short name")
	public void changeShortName() {
		type(random("SHT"), into(textbox().with(attribute("name", equalTo("shortNamesByLocale[en].name")))));
	}
	
	@When("I change the concept class to Test")
	public void changeConceptClass() {
		selectAValueInDropDownByXpath("//select[@id=\'conceptClass\']").selectByValue("1");
	}
	
	@When("I check/uncheck is set")
	public void changeIsSet() {
		clickOn(checkbox().with(attribute("id", equalTo("conceptSet"))));
	}
	
	@When("I change the datatype to Text")
	public void changeDatatype() {
		selectAValueInDropDownByXpath("//select[@id=\'datatype\']").selectByValue("3");
	}
	
	@When("I click $saveConcept button")
	public void clickSaveButton(String saveButtonLabel) {
		clickOn(Finders.button(saveButtonLabel));
	}
	
	@Then("The concept should get saved with a success message")
	public void theConceptShouldGetCreated() {
		assertPresenceOf(Finders.div("openmrs_msg").with(text(equalTo("Concept saved successfully"))));
	}
}
