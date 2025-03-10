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
package org.openmrs;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jbehave.core.annotations.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.lift.TestContext;
import org.openqa.selenium.lift.find.*;
import org.openqa.selenium.support.ui.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import static org.hamcrest.Matchers.equalTo;
import static org.openqa.selenium.lift.Finders.button;
import static org.openqa.selenium.lift.Finders.link;
import static org.openqa.selenium.lift.Matchers.attribute;
import static org.openqa.selenium.lift.Matchers.text;
import static org.openqa.selenium.lift.match.NumericalMatchers.exactly;
import static org.openqa.selenium.lift.match.SelectionMatcher.selection;

public abstract class Steps {

	private static final long DEFAULT_TIMEOUT = 5000;

	protected WebDriver driver;

	private TestContext context;

    public Steps(WebDriver driver) {
        this.driver = driver;
        this.context = new CustomWebDriverContext(driver);
    }

	protected WebDriver getWebDriver() {
		return driver;
	}

	protected void clickOn(Finder<WebElement, WebDriver> finder) {
		context.clickOn(finder);
	}

	protected void waitAndClickOn(Finder finder) {
		waitFor(finder);
		clickOn(finder);
	}

	protected void assertPresenceOf(Finder<WebElement, WebDriver> finder) {
		context.assertPresenceOf(finder);
	}

	protected void assertAbsenceOf(Finder<WebElement, WebDriver> finder) {
		((CustomWebDriverContext) context).assertAbsenceOf(finder);
	}

	protected void assertPresenceOf(Matcher<Integer> cardinalityConstraint, Finder<WebElement, WebDriver> finder) {
		context.assertPresenceOf(cardinalityConstraint, finder);
	}

	protected void waitFor(Finder<WebElement, WebDriver> finder) {
		waitFor(finder, DEFAULT_TIMEOUT);
	}

	protected void waitFor(Finder<WebElement, WebDriver> finder, long timeout) {
		context.waitFor(finder, timeout);
	}

	/**
	 * Cause the browser to navigate to the given URL
	 *
	 * @param url
	 */
	protected void goTo(String url) {
		context.goTo(url);
	}

	/**
	 * Type characters into an element of the page, typically an input field
	 *
	 * @param text - characters to type
	 * @param inputFinder - specification for the page element
	 */
	protected void type(String text, Finder<WebElement, WebDriver> inputFinder) {
		context.type(text, inputFinder);
	}

	/**
	 * Syntactic sugar to use with {@link HamcrestWebDriverTestCase#type(String, Finder<WebElement,
	 * WebDriver>)}, e.g. type("cheese", into(textbox())); The into() method simply returns its
	 * argument.
	 */
	protected Finder<WebElement, WebDriver> into(Finder<WebElement, WebDriver> input) {
		return input;
	}

	/**
	 * replace the default {@link TestContext}
	 */
	void setContext(TestContext context) {
		this.context = context;
	}

	/**
	 * Returns the current page source
	 */
	public String getPageSource() {
		return getWebDriver().getPageSource();
	}

	/**
	 * Returns the current page title
	 */
	public String getTitle() {
		return getWebDriver().getTitle();
	}

	public Finder<WebElement, WebDriver> finderByXpath(String xpath) {
		return new XPathFinder(xpath);
	}

	public Select selectAValueInDropDownByXpath(String identifierLocation) {
		return new Select(getWebDriver().findElement(By.xpath(identifierLocation)));
	}

	/**
	 * Returns the current URL
	 */
	public String getCurrentUrl() {
		return getWebDriver().getCurrentUrl();
	}

	protected void assertSelected(Finder<WebElement, WebDriver> finder) {
		assertPresenceOf(finder.with(selection()));
	}

	protected void assertNotSelected(Finder<WebElement, WebDriver> finder) {
		assertPresenceOf(exactly(0), finder.with(selection()));
	}

	protected HtmlTagFinder passwordtextbox() {
		return new InputFinder().with(attribute("type", equalTo("password")));
	}

	protected HtmlTagFinder checkbox() {
		return new InputFinder().with(attribute("type", equalTo("checkbox")));
	}

	/**
	 * Randomizes a give value
	 *
	 * @param value to be randomized
	 * @return value suffixed with a random number
	 */
	protected String random(String value) {
		return value + new Random().nextInt(100);
	}

	/**
	 * A finder which returns the first element matched - such as if you have multiple elements
	 * which match the finder (such as multiple links with the same text on a page etc)
	 */
	public static Finder<WebElement, WebDriver> second(final Finder<WebElement, WebDriver> finder) {
		return new BaseFinder<WebElement, WebDriver>() {
			
			@Override
			public Collection<WebElement> findFrom(WebDriver context) {
				Collection<WebElement> collection = super.findFrom(context);
				if (collection.size() > 1) {
					Iterator<WebElement> iter = collection.iterator();
					iter.next();
					return Collections.singletonList(iter.next());
				} else {
					return collection;
				}
			}
			
			protected Collection<WebElement> extractFrom(WebDriver context) {
				return finder.findFrom(context);
			}
			
			protected void describeTargetTo(Description description) {
				description.appendText("second ");
				finder.describeTo(description);
			}
		};
	}

    @When("I click on the $linkName link")
	public void clickLinkWithName(String linkName) {
		clickOn(link().with(text(equalTo(linkName))));
	}

    @When("I choose to $domain")
	public void addEncounter(String domain) {
		clickOn(link().with(text(equalTo(domain))));
	}

    @When("I click on the button $buttonName")
	public void save(String buttonName) {
		clickOn(button(buttonName));
	}

}
