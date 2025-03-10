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
package org.openmrs.web.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.LocationService;
import org.openmrs.api.PasswordException;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.web.OptionsForm;
import org.openmrs.web.WebConstants;
import org.openmrs.web.WebUtil;
import org.openmrs.web.user.UserProperties;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * This is the controller for the "My Profile" page. This lets logged in users set personal
 * preferences, update their own information, etc.
 * 
 * @see OptionsForm
 */
public class OptionsFormController extends SimpleFormController {
	
	/** Logger for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	protected ModelAndView processFormSubmission(HttpServletRequest request, HttpServletResponse response, Object object,
	        BindException errors) throws Exception {
		OptionsForm opts = (OptionsForm) object;
		
		if (opts.getUsername().length() > 0) {
			if (opts.getUsername().length() < 3) {
				errors.rejectValue("username", "error.username.weak");
			}
			if (opts.getUsername().charAt(0) < 'A' || opts.getUsername().charAt(0) > 'z') {
				errors.rejectValue("username", "error.username.invalid");
			}
			
		}
		if (opts.getUsername().length() > 0)
			
			if (!opts.getOldPassword().equals("")) {
				if (opts.getNewPassword().equals(""))
					errors.rejectValue("newPassword", "error.password.weak");
				else if (!opts.getNewPassword().equals(opts.getConfirmPassword())) {
					errors.rejectValue("newPassword", "error.password.match");
					errors.rejectValue("confirmPassword", "error.password.match");
				}
			}
		
		if (!opts.getSecretQuestionPassword().equals("")) {
			if (!opts.getSecretAnswerConfirm().equals(opts.getSecretAnswerNew())) {
				errors.rejectValue("secretAnswerNew", "error.options.secretAnswer.match");
				errors.rejectValue("secretAnswerConfirm", "error.options.secretAnswer.match");
			}
			if (opts.getSecretAnswerNew().isEmpty()) {
				errors.rejectValue("secretAnswerNew", "error.options.secretAnswer.empty");
			}
			if (opts.getSecretQuestionNew().isEmpty()) {
				errors.rejectValue("secretQuestionNew", "error.options.secretQuestion.empty");
			}
		}
		
		return super.processFormSubmission(request, response, object, errors);
	}
	
	/**
	 * The onSubmit function receives the form/command object that was modified by the input form
	 * and saves it to the db
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#onSubmit(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.Object,
	 *      org.springframework.validation.BindException)
	 */
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object obj,
	        BindException errors) throws Exception {
		
		HttpSession httpSession = request.getSession();
		
		String view = getFormView();
		
		if (!errors.hasErrors()) {
			User loginUser = Context.getAuthenticatedUser();
			UserService us = Context.getUserService();
			User user = null;
			try {
				Context.addProxyPrivilege(PrivilegeConstants.VIEW_USERS);
				user = us.getUser(loginUser.getUserId());
			}
			finally {
				Context.removeProxyPrivilege(PrivilegeConstants.VIEW_USERS);
			}
			
			OptionsForm opts = (OptionsForm) obj;
			
			Map<String, String> properties = user.getUserProperties();
			
			properties.put(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCATION, opts.getDefaultLocation());
			
			Locale locale = WebUtil.normalizeLocale(opts.getDefaultLocale());
			if (locale != null)
				properties.put(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE, locale.toString());
			
			properties.put(OpenmrsConstants.USER_PROPERTY_PROFICIENT_LOCALES, WebUtil.sanitizeLocales(opts
			        .getProficientLocales()));
			properties.put(OpenmrsConstants.USER_PROPERTY_SHOW_RETIRED, opts.getShowRetiredMessage().toString());
			properties.put(OpenmrsConstants.USER_PROPERTY_SHOW_VERBOSE, opts.getVerbose().toString());
			properties.put(OpenmrsConstants.USER_PROPERTY_NOTIFICATION, opts.getNotification() == null ? "" : opts
			        .getNotification().toString());
			properties.put(OpenmrsConstants.USER_PROPERTY_NOTIFICATION_ADDRESS, opts.getNotificationAddress() == null ? ""
			        : opts.getNotificationAddress().toString());
			
			if (!opts.getOldPassword().equals("")) {
				try {
					String password = opts.getNewPassword();
					
					// check password strength
					if (password.length() > 0) {
						try {
							OpenmrsUtil.validatePassword(user.getUsername(), password, String.valueOf(user.getUserId()));
						}
						catch (PasswordException e) {
							errors.reject(e.getMessage());
						}
						if (password.equals(opts.getOldPassword()) && !errors.hasErrors())
							errors.reject("error.password.different");
					}
					
					if (!errors.hasErrors()) {
						us.changePassword(opts.getOldPassword(), password);
						opts.setSecretQuestionPassword(password);
						new UserProperties(user.getUserProperties()).setSupposedToChangePassword(false);
					}
				}
				catch (APIException e) {
					errors.rejectValue("oldPassword", "error.password.match");
				}
			} else {
				// if they left the old password blank but filled in new
				// password
				if (!opts.getNewPassword().equals("")) {
					errors.rejectValue("oldPassword", "error.password.incorrect");
				}
			}
			
			if (!opts.getSecretQuestionPassword().equals("")) {
				if (!errors.hasErrors()) {
					try {
						user.setSecretQuestion(opts.getSecretQuestionNew());
						us.changeQuestionAnswer(opts.getSecretQuestionPassword(), opts.getSecretQuestionNew(), opts
						        .getSecretAnswerNew());
					}
					catch (APIException e) {
						errors.rejectValue("secretQuestionPassword", "error.password.match");
					}
				}
			} else if (!opts.getSecretAnswerNew().equals("")) {
				// if they left the old password blank but filled in new
				// password
				errors.rejectValue("secretQuestionPassword", "error.password.incorrect");
			}
			
			if (opts.getUsername().length() > 0 && !errors.hasErrors()) {
				try {
					Context.addProxyPrivilege(PrivilegeConstants.VIEW_USERS);
					if (us.hasDuplicateUsername(user)) {
						errors.rejectValue("username", "error.username.taken");
					}
				}
				finally {
					Context.removeProxyPrivilege(PrivilegeConstants.VIEW_USERS);
				}
			}
			
			if (!errors.hasErrors()) {
				
				user.setUsername(opts.getUsername());
				user.setUserProperties(properties);
				
				// new name
				PersonName newPersonName = opts.getPersonName();
				
				// existing name
				PersonName existingPersonName = user.getPersonName();
				
				// if two are not equal then make the new one the preferred,
				// make the old one voided
				if (!existingPersonName.equalsContent(newPersonName)) {
					existingPersonName.setPreferred(false);
					existingPersonName.setVoided(true);
					existingPersonName.setVoidedBy(user);
					existingPersonName.setDateVoided(new Date());
					existingPersonName.setVoidReason("Changed name on own options form");
					
					newPersonName.setPreferred(true);
					user.addName(newPersonName);
				}
				
				try {
					Context.addProxyPrivilege(PrivilegeConstants.EDIT_USERS);
					Context.addProxyPrivilege(PrivilegeConstants.VIEW_USERS);
					us.saveUser(user, null);
					//trigger updating of the javascript file cache
					PseudoStaticContentController.invalidateCachedResources(properties);
					// update login user object so that the new name is visible
					// in the webapp
					Context.refreshAuthenticatedUser();
				}
				finally {
					Context.removeProxyPrivilege(PrivilegeConstants.EDIT_USERS);
					Context.removeProxyPrivilege(PrivilegeConstants.VIEW_USERS);
				}
				
				httpSession.setAttribute(WebConstants.OPENMRS_MSG_ATTR, "options.saved");
			} else {
				return super.processFormSubmission(request, response, opts, errors);
			}
			
			view = getSuccessView();
		}
		return new ModelAndView(new RedirectView(view));
	}
	
	/**
	 * This is called prior to displaying a form for the first time. It tells Spring the
	 * form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		
		OptionsForm opts = new OptionsForm();
		
		if (Context.isAuthenticated()) {
			User user = Context.getAuthenticatedUser();
			
			Map<String, String> props = user.getUserProperties();
			opts.setDefaultLocation(props.get(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCATION));
			opts.setDefaultLocale(props.get(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCALE));
			opts.setProficientLocales(props.get(OpenmrsConstants.USER_PROPERTY_PROFICIENT_LOCALES));
			opts.setShowRetiredMessage(new Boolean(props.get(OpenmrsConstants.USER_PROPERTY_SHOW_RETIRED)));
			opts.setVerbose(new Boolean(props.get(OpenmrsConstants.USER_PROPERTY_SHOW_VERBOSE)));
			opts.setUsername(user.getUsername());
			opts.setSecretQuestionNew(user.getSecretQuestion());
			// Get a copy of the current person name and clear the id so that
			// they are separate objects
			PersonName personName = PersonName.newInstance(user.getPersonName());
			personName.setPersonNameId(null);
			opts.setPersonName(personName);
			opts.setNotification(props.get(OpenmrsConstants.USER_PROPERTY_NOTIFICATION));
			opts.setNotificationAddress(props.get(OpenmrsConstants.USER_PROPERTY_NOTIFICATION_ADDRESS));
		}
		
		return opts;
	}
	
	/**
	 * Called prior to form display. Allows for data to be put in the request to be used in the view
	 * 
	 * @see org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest)
	 */
	protected Map<String, Object> referenceData(HttpServletRequest request) throws Exception {
		
		HttpSession httpSession = request.getSession();
		
		Map<String, Object> map = new HashMap<String, Object>();
		
		if (Context.isAuthenticated()) {
			
			LocationService ls = Context.getLocationService();
			
			// set location options
			map.put("locations", ls.getAllLocations());
			
			// set language/locale options
			map.put("languages", Context.getAdministrationService().getPresentationLocales());
			
			String resetPassword = (String) httpSession.getAttribute("resetPassword");
			if (resetPassword == null)
				resetPassword = "";
			else
				httpSession.removeAttribute("resetPassword");
			map.put("resetPassword", resetPassword);
			
		}
		
		return map;
	}
}
