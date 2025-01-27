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
package org.openmrs.web.filter.initialization;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.zip.ZipInputStream;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import liquibase.changelog.ChangeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.xerces.impl.dv.util.Base64;
import org.openmrs.ImplementationId;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.PasswordException;
import org.openmrs.api.context.Context;
import org.openmrs.module.MandatoryModuleException;
import org.openmrs.module.OpenmrsCoreModuleException;
import org.openmrs.module.web.WebModuleUtil;
import org.openmrs.scheduler.SchedulerUtil;
import org.openmrs.util.DatabaseUpdateException;
import org.openmrs.util.DatabaseUpdater;
import org.openmrs.util.DatabaseUpdater.ChangeSetExecutorCallback;
import org.openmrs.util.DatabaseUtil;
import org.openmrs.util.InputRequiredException;
import org.openmrs.util.MemoryAppender;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.util.PrivilegeConstants;
import org.openmrs.util.Security;
import org.openmrs.web.Listener;
import org.openmrs.web.WebConstants;
import org.openmrs.web.filter.StartupFilter;
import org.openmrs.web.filter.util.CustomResourceLoader;
import org.openmrs.web.filter.util.ErrorMessageConstants;
import org.openmrs.web.filter.util.FilterUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ContextLoader;

/**
 * This is the first filter that is processed. It is only active when starting OpenMRS for the very
 * first time. It will redirect all requests to the {@link WebConstants#SETUP_PAGE_URL} if the
 * {@link Listener} wasn't able to find any runtime properties
 */
public class InitializationFilter extends StartupFilter {
	
	private static final Log log = LogFactory.getLog(InitializationFilter.class);
	
	private static final String LIQUIBASE_SCHEMA_DATA = "liquibase-schema-only.xml";
	
	private static final String LIQUIBASE_CORE_DATA = "liquibase-core-data.xml";
	
	private static final String LIQUIBASE_DEMO_DATA = "liquibase-demo-data.xml";
	
	/**
	 * The very first page of wizard, that asks user for select his preferred language
	 */
	private final String CHOOSE_LANG = "chooselang.vm";
	
	/**
	 * The second page of the wizard that asks for simple or advanced installation.
	 */
	private final String INSTALL_METHOD = "installmethod.vm";
	
	/**
	 * The simple installation setup page.
	 */
	private final String SIMPLE_SETUP = "simplesetup.vm";
	
	/**
	 * The first page of the advanced installation of the wizard that asks for a current or past
	 * database
	 */
	private final String DATABASE_SETUP = "databasesetup.vm";
	
	/**
	 * The page from where the user specifies the url to a remote system, username and password
	 */
	private final String TESTING_REMOTE_DETAILS_SETUP = "remotedetails.vm";
	
	/**
	 * The velocity macro page to redirect to if an error occurs or on initial startup
	 */
	private final String DEFAULT_PAGE = CHOOSE_LANG;
	
	/**
	 * This page asks whether database tables/demo data should be inserted and what the
	 * username/password that will be put into the runtime properties is
	 */
	private final String DATABASE_TABLES_AND_USER = "databasetablesanduser.vm";
	
	/**
	 * This page lets the user define the admin user
	 */
	private static final String ADMIN_USER_SETUP = "adminusersetup.vm";
	
	/**
	 * This page lets the user pick an implementation id
	 */
	private static final String IMPLEMENTATION_ID_SETUP = "implementationidsetup.vm";
	
	/**
	 * This page asks for settings that will be put into the runtime properties files
	 */
	private final String OTHER_RUNTIME_PROPS = "otherruntimeproperties.vm";
	
	/**
	 * A page that tells the user that everything is collected and will now be processed
	 */
	private static final String WIZARD_COMPLETE = "wizardcomplete.vm";
	
	/**
	 * A page that lists off what is happening while it is going on. This page has ajax that callst
	 * he {@value #PROGRESS_VM_AJAXREQUEST} page
	 */
	private static final String PROGRESS_VM = "progress.vm";
	
	/**
	 * This url is called by javascript to get the status of the install
	 */
	private static final String PROGRESS_VM_AJAXREQUEST = "progress.vm.ajaxRequest";
	
	public static final String RELEASE_TESTING_MODULE_PATH = "/module/releasetestinghelper/";
	
	/**
	 * The model object that holds all the properties that the rendered templates use. All
	 * attributes on this object are made available to all templates via reflection in the
	 * {@link #renderTemplate(String, Map, httpResponse)} method.
	 */
	private InitializationWizardModel wizardModel = null;
	
	private InitializationCompletion initJob;
	
	/**
	 * Variable set to true as soon as the installation begins and set to false when the process
	 * ends This thread should only be accesses through the synchronized method.
	 */
	private static boolean isInstallationStarted = false;
	
	// the actual driver loaded by the DatabaseUpdater class
	private String loadedDriverString;
	
	/**
	 * Variable set at the end of the wizard when spring is being restarted
	 */
	private static boolean initializationComplete = false;
	
	synchronized protected void setInitializationComplete(boolean initializationComplete) {
		InitializationFilter.initializationComplete = initializationComplete;
	}
	
	/**
	 * Called by {@link #doFilter(ServletRequest, ServletResponse, FilterChain)} on GET requests
	 * 
	 * @param httpRequest
	 * @param httpResponse
	 */
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException,
	        ServletException {
		
		String page = httpRequest.getParameter("page");
		Map<String, Object> referenceMap = new HashMap<String, Object>();
		if (httpRequest.getServletPath().equals("/" + AUTO_RUN_OPENMRS)) {
			autoRunOpenMRS(httpRequest);
			return;
		}
		// we need to save current user language in references map since it will be used when template
		// will be rendered
		if (httpRequest.getSession().getAttribute(FilterUtil.LOCALE_ATTRIBUTE) != null) {
			referenceMap
			        .put(FilterUtil.LOCALE_ATTRIBUTE, httpRequest.getSession().getAttribute(FilterUtil.LOCALE_ATTRIBUTE));
		}
		if (page == null) {
			checkLocaleAttributesForFirstTime(httpRequest);
			referenceMap
			        .put(FilterUtil.LOCALE_ATTRIBUTE, httpRequest.getSession().getAttribute(FilterUtil.LOCALE_ATTRIBUTE));
			httpResponse.setContentType("text/html");
			renderTemplate(DEFAULT_PAGE, referenceMap, httpResponse);
		} else if (INSTALL_METHOD.equals(page)) {
			// get props and render the second page
			File runtimeProperties = getRuntimePropertiesFile();
			
			if (!runtimeProperties.exists()) {
				try {
					runtimeProperties.createNewFile();
					// reset the error objects in case of refresh
					wizardModel.canCreate = true;
					wizardModel.cannotCreateErrorMessage = "";
				}
				catch (IOException io) {
					wizardModel.canCreate = false;
					wizardModel.cannotCreateErrorMessage = io.getMessage();
				}
				
				// check this before deleting the file again
				wizardModel.canWrite = runtimeProperties.canWrite();
				
				// delete the file again after testing the create/write
				// so that if the user stops the webapp before finishing
				// this wizard, they can still get back into it
				runtimeProperties.delete();
				
			} else {
				wizardModel.canWrite = runtimeProperties.canWrite();
				
				wizardModel.databaseConnection = Context.getRuntimeProperties().getProperty("connection.url",
				    wizardModel.databaseConnection);
				
				wizardModel.currentDatabaseUsername = Context.getRuntimeProperties().getProperty("connection.username",
				    wizardModel.currentDatabaseUsername);
				
				wizardModel.currentDatabasePassword = Context.getRuntimeProperties().getProperty("connection.password",
				    wizardModel.currentDatabasePassword);
			}
			
			wizardModel.runtimePropertiesPath = runtimeProperties.getAbsolutePath();
			
			// do step one of the wizard
			httpResponse.setContentType("text/html");
			// if any body has already started installation
			if (isInstallationStarted()) {
				renderTemplate(PROGRESS_VM, referenceMap, httpResponse);
			} else {
				renderTemplate(INSTALL_METHOD, referenceMap, httpResponse);
			}
		} else if (PROGRESS_VM_AJAXREQUEST.equals(page)) {
			httpResponse.setContentType("text/json");
			httpResponse.setHeader("Cache-Control", "no-cache");
			Map<String, Object> result = new HashMap<String, Object>();
			if (initJob != null) {
				result.put("hasErrors", initJob.hasErrors());
				if (initJob.hasErrors()) {
					result.put("errorPage", initJob.getErrorPage());
					errors.putAll(initJob.getErrors());
				}
				
				result.put("initializationComplete", isInitializationComplete());
				result.put("message", initJob.getMessage());
				result.put("actionCounter", initJob.getStepsComplete());
				if (!isInitializationComplete()) {
					result.put("executingTask", initJob.getExecutingTask());
					result.put("executedTasks", initJob.getExecutedTasks());
					result.put("completedPercentage", initJob.getCompletedPercentage());
				}
				
				Appender appender = Logger.getRootLogger().getAppender("MEMORY_APPENDER");
				if (appender instanceof MemoryAppender) {
					MemoryAppender memoryAppender = (MemoryAppender) appender;
					List<String> logLines = memoryAppender.getLogLines();
					// truncate the list to the last 5 so we don't overwhelm jquery
					if (logLines.size() > 5)
						logLines = logLines.subList(logLines.size() - 5, logLines.size());
					result.put("logLines", logLines);
				} else {
					result.put("logLines", new ArrayList<String>());
				}
			}
			
			httpResponse.getWriter().write(toJSONString(result, true));
		}
	}
	
	/**
	 * Called by {@link #doFilter(ServletRequest, ServletResponse, FilterChain)} on POST requests
	 * 
	 * @param httpRequest
	 * @param httpResponse
	 */
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException,
	        ServletException {
		
		String page = httpRequest.getParameter("page");
		Map<String, Object> referenceMap = new HashMap<String, Object>();
		// we need to save current user language in references map since it will be used when template
		// will be rendered
		if (httpRequest.getSession().getAttribute(FilterUtil.LOCALE_ATTRIBUTE) != null) {
			referenceMap
			        .put(FilterUtil.LOCALE_ATTRIBUTE, httpRequest.getSession().getAttribute(FilterUtil.LOCALE_ATTRIBUTE));
		}
		
		// if any body has already started installation
		if (isInstallationStarted()) {
			httpResponse.setContentType("text/html");
			renderTemplate(PROGRESS_VM, referenceMap, httpResponse);
			return;
		}
		if (DEFAULT_PAGE.equals(page)) {
			// get props and render the first page
			File runtimeProperties = getRuntimePropertiesFile();
			if (!runtimeProperties.exists()) {
				try {
					runtimeProperties.createNewFile();
					// reset the error objects in case of refresh
					wizardModel.canCreate = true;
					wizardModel.cannotCreateErrorMessage = "";
				}
				catch (IOException io) {
					wizardModel.canCreate = false;
					wizardModel.cannotCreateErrorMessage = io.getMessage();
				}
				// check this before deleting the file again
				wizardModel.canWrite = runtimeProperties.canWrite();
				
				// delete the file again after testing the create/write
				// so that if the user stops the webapp before finishing
				// this wizard, they can still get back into it
				runtimeProperties.delete();
			} else {
				wizardModel.canWrite = runtimeProperties.canWrite();
				
				wizardModel.databaseConnection = Context.getRuntimeProperties().getProperty("connection.url",
				    wizardModel.databaseConnection);
				
				wizardModel.currentDatabaseUsername = Context.getRuntimeProperties().getProperty("connection.username",
				    wizardModel.currentDatabaseUsername);
				
				wizardModel.currentDatabasePassword = Context.getRuntimeProperties().getProperty("connection.password",
				    wizardModel.currentDatabasePassword);
			}
			
			wizardModel.runtimePropertiesPath = runtimeProperties.getAbsolutePath();
			
			checkLocaleAttributes(httpRequest);
			referenceMap
			        .put(FilterUtil.LOCALE_ATTRIBUTE, httpRequest.getSession().getAttribute(FilterUtil.LOCALE_ATTRIBUTE));
			log.info("Locale stored in session is " + httpRequest.getSession().getAttribute(FilterUtil.LOCALE_ATTRIBUTE));
			
			httpResponse.setContentType("text/html");
			// otherwise do step one of the wizard
			renderTemplate(INSTALL_METHOD, referenceMap, httpResponse);
		} else if (INSTALL_METHOD.equals(page)) {
			if (goBack(httpRequest)) {
				referenceMap.put(FilterUtil.REMEMBER_ATTRIBUTE, httpRequest.getSession().getAttribute(
				    FilterUtil.REMEMBER_ATTRIBUTE) != null);
				referenceMap.put(FilterUtil.LOCALE_ATTRIBUTE, httpRequest.getSession().getAttribute(
				    FilterUtil.LOCALE_ATTRIBUTE));
				renderTemplate(CHOOSE_LANG, referenceMap, httpResponse);
				return;
			}
			wizardModel.installMethod = httpRequest.getParameter("install_method");
			if (InitializationWizardModel.INSTALL_METHOD_SIMPLE.equals(wizardModel.installMethod)) {
				page = SIMPLE_SETUP;
			} else if (InitializationWizardModel.INSTALL_METHOD_TESTING.equals(wizardModel.installMethod)) {
				page = TESTING_REMOTE_DETAILS_SETUP;
				wizardModel.currentStepNumber = 1;
				wizardModel.numberOfSteps = skipDatabaseSetupPage() ? 1 : 3;
			} else {
				page = DATABASE_SETUP;
				wizardModel.currentStepNumber = 1;
				wizardModel.numberOfSteps = 5;
			}
			renderTemplate(page, referenceMap, httpResponse);
		} // simple method
		else if (SIMPLE_SETUP.equals(page)) {
			if (goBack(httpRequest)) {
				renderTemplate(INSTALL_METHOD, referenceMap, httpResponse);
				return;
			}
			wizardModel.databaseConnection = Context.getRuntimeProperties().getProperty("connection.url",
			    wizardModel.databaseConnection);
			
			wizardModel.createDatabaseUsername = Context.getRuntimeProperties().getProperty("connection.username",
			    wizardModel.createDatabaseUsername);
			
			wizardModel.createUserUsername = wizardModel.createDatabaseUsername;
			
			wizardModel.databaseRootPassword = httpRequest.getParameter("database_root_password");
			checkForEmptyValue(wizardModel.databaseRootPassword, errors, ErrorMessageConstants.ERROR_DB_PSDW_REQ);
			
			wizardModel.hasCurrentOpenmrsDatabase = false;
			wizardModel.createTables = true;
			// default wizardModel.databaseName is openmrs
			// default wizardModel.createDatabaseUsername is root
			wizardModel.createDatabasePassword = wizardModel.databaseRootPassword;
			wizardModel.addDemoData = "yes".equals(httpRequest.getParameter("add_demo_data"));
			
			wizardModel.hasCurrentDatabaseUser = false;
			wizardModel.createDatabaseUser = true;
			// default wizardModel.createUserUsername is root
			wizardModel.createUserPassword = wizardModel.databaseRootPassword;
			
			wizardModel.moduleWebAdmin = true;
			wizardModel.autoUpdateDatabase = false;
			
			wizardModel.adminUserPassword = InitializationWizardModel.ADMIN_DEFAULT_PASSWORD;
			
			createSimpleSetup(httpRequest.getParameter("database_root_password"), httpRequest.getParameter("add_demo_data"));
			
			try {
				loadedDriverString = DatabaseUtil.loadDatabaseDriver(wizardModel.databaseConnection,
				    wizardModel.databaseDriver);
			}
			catch (ClassNotFoundException e) {
				errors.put(ErrorMessageConstants.ERROR_DB_DRIVER_CLASS_REQ, null);
				renderTemplate(page, referenceMap, httpResponse);
				return;
			}
			
			if (errors.isEmpty()) {
				page = WIZARD_COMPLETE;
			}
			renderTemplate(page, referenceMap, httpResponse);
		} // step one
		else if (DATABASE_SETUP.equals(page)) {
			if (goBack(httpRequest)) {
				wizardModel.currentStepNumber -= 1;
				if (InitializationWizardModel.INSTALL_METHOD_TESTING.equals(wizardModel.installMethod)) {
					renderTemplate(TESTING_REMOTE_DETAILS_SETUP, referenceMap, httpResponse);
				} else {
					renderTemplate(INSTALL_METHOD, referenceMap, httpResponse);
				}
				return;
			}
			
			wizardModel.databaseConnection = httpRequest.getParameter("database_connection");
			checkForEmptyValue(wizardModel.databaseConnection, errors, ErrorMessageConstants.ERROR_DB_CONN_REQ);
			
			wizardModel.databaseDriver = httpRequest.getParameter("database_driver");
			checkForEmptyValue(wizardModel.databaseConnection, errors, ErrorMessageConstants.ERROR_DB_DRIVER_REQ);
			
			loadedDriverString = loadDriver(wizardModel.databaseConnection, wizardModel.databaseDriver);
			if (!StringUtils.hasText(loadedDriverString)) {
				errors.put(ErrorMessageConstants.ERROR_DB_DRIVER_CLASS_REQ, null);
				renderTemplate(page, referenceMap, httpResponse);
				return;
			}
			
			//TODO make each bit of page logic a (unit testable) method
			
			// asked the user for their desired database name
			
			if ("yes".equals(httpRequest.getParameter("current_openmrs_database"))) {
				wizardModel.databaseName = httpRequest.getParameter("openmrs_current_database_name");
				checkForEmptyValue(wizardModel.databaseName, errors, ErrorMessageConstants.ERROR_DB_CURR_NAME_REQ);
				wizardModel.hasCurrentOpenmrsDatabase = true;
				// TODO check to see if this is an active database
				
			} else {
				// mark this wizard as a "to create database" (done at the end)
				wizardModel.hasCurrentOpenmrsDatabase = false;
				
				wizardModel.createTables = true;
				
				wizardModel.databaseName = httpRequest.getParameter("openmrs_new_database_name");
				checkForEmptyValue(wizardModel.databaseName, errors, ErrorMessageConstants.ERROR_DB_NEW_NAME_REQ);
				// TODO create database now to check if its possible?
				
				wizardModel.createDatabaseUsername = httpRequest.getParameter("create_database_username");
				checkForEmptyValue(wizardModel.createDatabaseUsername, errors, ErrorMessageConstants.ERROR_DB_USER_NAME_REQ);
				wizardModel.createDatabasePassword = httpRequest.getParameter("create_database_password");
				checkForEmptyValue(wizardModel.createDatabasePassword, errors, ErrorMessageConstants.ERROR_DB_USER_PSWD_REQ);
			}
			
			if (errors.isEmpty()) {
				page = DATABASE_TABLES_AND_USER;
				
				if (InitializationWizardModel.INSTALL_METHOD_TESTING.equals(wizardModel.installMethod)) {
					wizardModel.currentStepNumber = 3;
				} else {
					wizardModel.currentStepNumber = 2;
				}
			}
			
			renderTemplate(page, referenceMap, httpResponse);
			
		} // step two
		else if (DATABASE_TABLES_AND_USER.equals(page)) {
			
			if (goBack(httpRequest)) {
				wizardModel.currentStepNumber -= 1;
				renderTemplate(DATABASE_SETUP, referenceMap, httpResponse);
				return;
			}
			
			if (wizardModel.hasCurrentOpenmrsDatabase) {
				wizardModel.createTables = "yes".equals(httpRequest.getParameter("create_tables"));
			}
			
			wizardModel.addDemoData = "yes".equals(httpRequest.getParameter("add_demo_data"));
			
			if ("yes".equals(httpRequest.getParameter("current_database_user"))) {
				wizardModel.currentDatabaseUsername = httpRequest.getParameter("current_database_username");
				checkForEmptyValue(wizardModel.currentDatabaseUsername, errors,
				    ErrorMessageConstants.ERROR_DB_CUR_USER_NAME_REQ);
				wizardModel.currentDatabasePassword = httpRequest.getParameter("current_database_password");
				checkForEmptyValue(wizardModel.currentDatabasePassword, errors,
				    ErrorMessageConstants.ERROR_DB_CUR_USER_PSWD_REQ);
				wizardModel.hasCurrentDatabaseUser = true;
				wizardModel.createDatabaseUser = false;
			} else {
				wizardModel.hasCurrentDatabaseUser = false;
				wizardModel.createDatabaseUser = true;
				// asked for the root mysql username/password
				wizardModel.createUserUsername = httpRequest.getParameter("create_user_username");
				checkForEmptyValue(wizardModel.createUserUsername, errors, ErrorMessageConstants.ERROR_DB_USER_NAME_REQ);
				wizardModel.createUserPassword = httpRequest.getParameter("create_user_password");
				checkForEmptyValue(wizardModel.createUserPassword, errors, ErrorMessageConstants.ERROR_DB_USER_PSWD_REQ);
			}
			
			if (errors.isEmpty()) { // go to next page
				page = InitializationWizardModel.INSTALL_METHOD_TESTING.equals(wizardModel.installMethod) ? WIZARD_COMPLETE
				        : OTHER_RUNTIME_PROPS;
			}
			
			renderTemplate(page, referenceMap, httpResponse);
		} // step three
		else if (OTHER_RUNTIME_PROPS.equals(page)) {
			
			if (goBack(httpRequest)) {
				renderTemplate(DATABASE_TABLES_AND_USER, referenceMap, httpResponse);
				return;
			}
			
			wizardModel.moduleWebAdmin = "yes".equals(httpRequest.getParameter("module_web_admin"));
			wizardModel.autoUpdateDatabase = "yes".equals(httpRequest.getParameter("auto_update_database"));
			
			if (wizardModel.createTables) { // go to next page if they are creating tables
				page = ADMIN_USER_SETUP;
			} else { // skip a page
				page = IMPLEMENTATION_ID_SETUP;
			}
			
			renderTemplate(page, referenceMap, httpResponse);
			
		} // optional step four
		else if (ADMIN_USER_SETUP.equals(page)) {
			
			if (goBack(httpRequest)) {
				renderTemplate(OTHER_RUNTIME_PROPS, referenceMap, httpResponse);
				return;
			}
			
			wizardModel.adminUserPassword = httpRequest.getParameter("new_admin_password");
			String adminUserConfirm = httpRequest.getParameter("new_admin_password_confirm");
			
			// throw back to admin user if passwords don't match
			if (!wizardModel.adminUserPassword.equals(adminUserConfirm)) {
				errors.put(ErrorMessageConstants.ERROR_DB_ADM_PSWDS_MATCH, null);
				renderTemplate(ADMIN_USER_SETUP, referenceMap, httpResponse);
				return;
			}
			
			// throw back if the user didn't put in a password
			if (wizardModel.adminUserPassword.equals("")) {
				errors.put(ErrorMessageConstants.ERROR_DB_ADM_PSDW_EMPTY, null);
				renderTemplate(ADMIN_USER_SETUP, referenceMap, httpResponse);
				return;
			}
			
			try {
				OpenmrsUtil.validatePassword("admin", wizardModel.adminUserPassword, "admin");
			}
			catch (PasswordException p) {
				errors.put(ErrorMessageConstants.ERROR_DB_ADM_PSDW_WEAK, null);
				renderTemplate(ADMIN_USER_SETUP, referenceMap, httpResponse);
				return;
			}
			
			if (errors.isEmpty()) { // go to next page
				page = IMPLEMENTATION_ID_SETUP;
			}
			
			renderTemplate(page, referenceMap, httpResponse);
			
		} // optional step five
		else if (IMPLEMENTATION_ID_SETUP.equals(page)) {
			
			if (goBack(httpRequest)) {
				if (wizardModel.createTables)
					renderTemplate(ADMIN_USER_SETUP, referenceMap, httpResponse);
				else
					renderTemplate(OTHER_RUNTIME_PROPS, referenceMap, httpResponse);
				return;
			}
			
			wizardModel.implementationIdName = httpRequest.getParameter("implementation_name");
			wizardModel.implementationId = httpRequest.getParameter("implementation_id");
			wizardModel.implementationIdPassPhrase = httpRequest.getParameter("pass_phrase");
			wizardModel.implementationIdDescription = httpRequest.getParameter("description");
			
			// throw back if the user-specified ID is invalid (contains ^ or |).
			if (wizardModel.implementationId.indexOf('^') != -1 || wizardModel.implementationId.indexOf('|') != -1) {
				errors.put(ErrorMessageConstants.ERROR_DB_IMPL_ID_REQ, null);
				renderTemplate(IMPLEMENTATION_ID_SETUP, referenceMap, httpResponse);
				return;
			}
			
			if (errors.isEmpty()) { // go to next page
				page = WIZARD_COMPLETE;
			}
			
			renderTemplate(page, referenceMap, httpResponse);
		} else if (WIZARD_COMPLETE.equals(page)) {
			
			if (goBack(httpRequest)) {
				
				if (InitializationWizardModel.INSTALL_METHOD_SIMPLE.equals(wizardModel.installMethod)) {
					page = SIMPLE_SETUP;
				} else if (InitializationWizardModel.INSTALL_METHOD_TESTING.equals(wizardModel.installMethod)) {
					if (skipDatabaseSetupPage()) {
						page = TESTING_REMOTE_DETAILS_SETUP;
					} else {
						page = DATABASE_TABLES_AND_USER;
					}
				} else {
					page = IMPLEMENTATION_ID_SETUP;
				}
				renderTemplate(page, referenceMap, httpResponse);
				return;
			}
			
			wizardModel.tasksToExecute = new ArrayList<WizardTask>();
			createDatabaseTask();
			if (InitializationWizardModel.INSTALL_METHOD_TESTING.equals(wizardModel.installMethod)) {
				wizardModel.importTestData = true;
				wizardModel.createTables = false;
				wizardModel.addDemoData = false;
				//if we have a runtime properties file
				if (skipDatabaseSetupPage()) {
					wizardModel.hasCurrentOpenmrsDatabase = false;
					wizardModel.hasCurrentDatabaseUser = true;
					wizardModel.createDatabaseUser = false;
					Properties props = OpenmrsUtil.getRuntimeProperties(WebConstants.WEBAPP_NAME);
					wizardModel.currentDatabaseUsername = props.getProperty("connection.username");
					wizardModel.currentDatabasePassword = props.getProperty("connection.password");
					wizardModel.createDatabaseUsername = wizardModel.currentDatabaseUsername;
					wizardModel.createDatabasePassword = wizardModel.currentDatabasePassword;
				}
				
				wizardModel.tasksToExecute.add(WizardTask.IMPORT_TEST_DATA);
				wizardModel.tasksToExecute.add(WizardTask.ADD_MODULES);
			} else {
				createTablesTask();
				createDemoDataTask();
			}
			wizardModel.tasksToExecute.add(WizardTask.UPDATE_TO_LATEST);
			
			referenceMap.put("tasksToExecute", wizardModel.tasksToExecute);
			startInstallation();
			referenceMap.put("isInstallationStarted", isInstallationStarted());
			renderTemplate(PROGRESS_VM, referenceMap, httpResponse);
		} else if (TESTING_REMOTE_DETAILS_SETUP.equals(page)) {
			if (goBack(httpRequest)) {
				wizardModel.currentStepNumber -= 1;
				renderTemplate(INSTALL_METHOD, referenceMap, httpResponse);
				return;
			}
			
			wizardModel.remoteUrl = httpRequest.getParameter("remoteUrl");
			checkForEmptyValue(wizardModel.remoteUrl, errors, "install.testing.remote.url.required");
			if (errors.isEmpty()) {
				//Check if the remote system is running
				if (TestInstallUtil.testConnection(wizardModel.remoteUrl)) {
					//Check if the test module is installed by connecting to its setting page
					if (TestInstallUtil.testConnection(wizardModel.remoteUrl.concat(RELEASE_TESTING_MODULE_PATH
					        + "settings.htm"))) {
						
						wizardModel.remoteUsername = httpRequest.getParameter("username");
						wizardModel.remotePassword = httpRequest.getParameter("password");
						checkForEmptyValue(wizardModel.remoteUsername, errors, "install.testing.username.required");
						checkForEmptyValue(wizardModel.remotePassword, errors, "install.testing.password.required");
						
						if (errors.isEmpty()) {
							//check if the username and password are valid
							try {
								TestInstallUtil.getResourceInputStream(wizardModel.remoteUrl + RELEASE_TESTING_MODULE_PATH
								        + "verifycredentials.htm", wizardModel.remoteUsername, wizardModel.remotePassword);
							}
							catch (APIAuthenticationException e) {
								if (log.isDebugEnabled())
									log.debug("Error generated: ", e);
								page = TESTING_REMOTE_DETAILS_SETUP;
								errors.put(ErrorMessageConstants.UPDATE_ERROR_UNABLE_AUTHENTICATE, null);
								renderTemplate(page, referenceMap, httpResponse);
								return;
							}
							
							//If we have a runtime properties file, get the database setup details from it
							if (skipDatabaseSetupPage()) {
								Properties props = OpenmrsUtil.getRuntimeProperties(WebConstants.WEBAPP_NAME);
								wizardModel.databaseConnection = props.getProperty("connection.url");
								loadedDriverString = loadDriver(wizardModel.databaseConnection, wizardModel.databaseDriver);
								if (!StringUtils.hasText(loadedDriverString)) {
									page = TESTING_REMOTE_DETAILS_SETUP;
									errors.put(ErrorMessageConstants.ERROR_DB_DRIVER_CLASS_REQ, null);
									renderTemplate(page, referenceMap, httpResponse);
									return;
								}
								
								wizardModel.databaseName = InitializationWizardModel.DEFAULT_DATABASE_NAME;
								page = WIZARD_COMPLETE;
							} else {
								page = DATABASE_SETUP;
								wizardModel.currentStepNumber = 2;
							}
							msgs.put("install.testing.testingModuleFound", null);
						} else {
							renderTemplate(page, referenceMap, httpResponse);
							return;
						}
					} else {
						errors.put("install.testing.noTestingModule", null);
					}
				} else {
					errors.put("install.testing.invalidProductionUrl", new Object[] { wizardModel.remoteUrl });
				}
			}
			
			renderTemplate(page, referenceMap, httpResponse);
			return;
		}
	}
	
	private void startInstallation() {
		//if no one has run any installation
		if (!isInstallationStarted()) {
			initJob = new InitializationCompletion();
			setInstallationStarted(true);
			initJob.start();
		}
	}
	
	private void createDemoDataTask() {
		if (wizardModel.addDemoData)
			wizardModel.tasksToExecute.add(WizardTask.ADD_DEMO_DATA);
	}
	
	private void createTablesTask() {
		if (wizardModel.createTables) {
			wizardModel.tasksToExecute.add(WizardTask.CREATE_TABLES);
			wizardModel.tasksToExecute.add(WizardTask.ADD_CORE_DATA);
		}
	}
	
	private void createDatabaseTask() {
		if (!wizardModel.hasCurrentOpenmrsDatabase)
			wizardModel.tasksToExecute.add(WizardTask.CREATE_SCHEMA);
		if (wizardModel.createDatabaseUser)
			wizardModel.tasksToExecute.add(WizardTask.CREATE_DB_USER);
	}
	
	private void createSimpleSetup(String databaseRootPassword, String addDemoData) {
		setDatabaseNameIfInTestMode();
		wizardModel.databaseConnection = Context.getRuntimeProperties().getProperty("connection.url",
		    wizardModel.databaseConnection);
		
		wizardModel.createDatabaseUsername = Context.getRuntimeProperties().getProperty("connection.username",
		    wizardModel.createDatabaseUsername);
		
		wizardModel.createUserUsername = wizardModel.createDatabaseUsername;
		
		wizardModel.databaseRootPassword = databaseRootPassword;
		checkForEmptyValue(wizardModel.databaseRootPassword, errors, ErrorMessageConstants.ERROR_DB_PSDW_REQ);
		
		wizardModel.hasCurrentOpenmrsDatabase = false;
		wizardModel.createTables = true;
		// default wizardModel.databaseName is openmrs
		// default wizardModel.createDatabaseUsername is root
		wizardModel.createDatabasePassword = wizardModel.databaseRootPassword;
		wizardModel.addDemoData = "yes".equals(addDemoData);
		
		wizardModel.hasCurrentDatabaseUser = false;
		wizardModel.createDatabaseUser = true;
		// default wizardModel.createUserUsername is root
		wizardModel.createUserPassword = wizardModel.databaseRootPassword;
		
		wizardModel.moduleWebAdmin = true;
		wizardModel.autoUpdateDatabase = false;
		
		wizardModel.adminUserPassword = InitializationWizardModel.ADMIN_DEFAULT_PASSWORD;
	}
	
	private void setDatabaseNameIfInTestMode() {
		if (OpenmrsUtil.isTestMode()) {
			wizardModel.databaseName = OpenmrsUtil.getOpenMRSVersionInTestMode();
		}
	}
	
	private void autoRunOpenMRS(HttpServletRequest httpRequest) {
		File runtimeProperties = getRuntimePropertiesFile();
		wizardModel.runtimePropertiesPath = runtimeProperties.getAbsolutePath();
		if (httpRequest.getParameter("database_user_name") != null)
			wizardModel.createDatabaseUsername = httpRequest.getParameter("database_user_name");
		checkLocaleAttributes(httpRequest);
		createSimpleSetup(httpRequest.getParameter("database_root_password"), "yes");
		try {
			loadedDriverString = DatabaseUtil.loadDatabaseDriver(wizardModel.databaseConnection, wizardModel.databaseDriver);
		}
		catch (ClassNotFoundException e) {
			errors.put(ErrorMessageConstants.ERROR_DB_DRIVER_CLASS_REQ, null);
			return;
		}
		wizardModel.tasksToExecute = new ArrayList<WizardTask>();
		createDatabaseTask();
		createTablesTask();
		createDemoDataTask();
		wizardModel.tasksToExecute.add(WizardTask.UPDATE_TO_LATEST);
		startInstallation();
	}
	
	/**
	 * This method should be called after the user has left wizard's first page (i.e. choose
	 * language). It checks if user has changed any of locale related parameters and makes
	 * appropriate corrections with filter's model or/and with locale attribute inside user's
	 * session.
	 * 
	 * @param httpRequest the http request object
	 */
	private void checkLocaleAttributes(HttpServletRequest httpRequest) {
		String localeParameter = httpRequest.getParameter(FilterUtil.LOCALE_ATTRIBUTE);
		Boolean rememberLocale = false;
		// we need to check if user wants that system will remember his selection of language
		if (httpRequest.getParameter(FilterUtil.REMEMBER_ATTRIBUTE) != null)
			rememberLocale = true;
		if (localeParameter != null) {
			String storedLocale = null;
			if (httpRequest.getSession().getAttribute(FilterUtil.LOCALE_ATTRIBUTE) != null) {
				storedLocale = httpRequest.getSession().getAttribute(FilterUtil.LOCALE_ATTRIBUTE).toString();
			}
			// if user has changed locale parameter to new one
			// or chooses it parameter at first page loading
			if ((storedLocale == null) || (storedLocale != null && !storedLocale.equals(localeParameter))) {
				log.info("Stored locale parameter to session " + localeParameter);
				httpRequest.getSession().setAttribute(FilterUtil.LOCALE_ATTRIBUTE, localeParameter);
			}
			if (rememberLocale) {
				httpRequest.getSession().setAttribute(FilterUtil.LOCALE_ATTRIBUTE, localeParameter);
				httpRequest.getSession().setAttribute(FilterUtil.REMEMBER_ATTRIBUTE, true);
				wizardModel.localeToSave = localeParameter;
			} else {
				// we need to reset it if it was set before
				httpRequest.getSession().setAttribute(FilterUtil.REMEMBER_ATTRIBUTE, null);
				wizardModel.localeToSave = null;
			}
		}
	}
	
	/**
	 * It sets locale parameter for current session when user is making first GET http request to
	 * application. It retrieves user locale from request object and checks if this locale is
	 * supported by application. If not, it uses {@link Locale#ENGLISH} by default
	 * 
	 * @param httpRequest the http request object
	 */
	public void checkLocaleAttributesForFirstTime(HttpServletRequest httpRequest) {
		Locale locale = httpRequest.getLocale();
		if (CustomResourceLoader.getInstance(httpRequest).getAvailablelocales().contains(locale)) {
			httpRequest.getSession().setAttribute(FilterUtil.LOCALE_ATTRIBUTE, locale.toString());
		} else {
			httpRequest.getSession().setAttribute(FilterUtil.LOCALE_ATTRIBUTE, Locale.ENGLISH.toString());
		}
	}
	
	/**
	 * Verify the database connection works.
	 * 
	 * @param connectionUsername
	 * @param connectionPassword
	 * @param databaseConnectionFinalUrl
	 * @return true/false whether it was verified or not
	 */
	private boolean verifyConnection(String connectionUsername, String connectionPassword, String databaseConnectionFinalUrl) {
		try {
			// verify connection
			//Set Database Driver using driver String
			Class.forName(loadedDriverString).newInstance();
			DriverManager.getConnection(databaseConnectionFinalUrl, connectionUsername, connectionPassword);
			return true;
			
		}
		catch (Exception e) {
			errors.put("User account " + connectionUsername + " does not work. " + e.getMessage()
			        + " See the error log for more details", null); // TODO internationalize this
			log.warn("Error while checking the connection user account", e);
			return false;
		}
	}
	
	/**
	 * Convenience method to load the runtime properties file.
	 * 
	 * @return the runtime properties file.
	 */
	private File getRuntimePropertiesFile() {
		File file = null;
		
		String pathName = OpenmrsUtil.getRuntimePropertiesFilePathName(WebConstants.WEBAPP_NAME);
		if (pathName != null) {
			file = new File(pathName);
		} else {
			file = new File(OpenmrsUtil.getApplicationDataDirectory(), getRuntimePropertiesFileName());
		}
		
		log.debug("Using file: " + file.getAbsolutePath());
		
		return file;
	}
	
	private String getRuntimePropertiesFileName() {
		String fileName = OpenmrsUtil.getRuntimePropertiesFileNameInTestMode();
		if (fileName == null) {
			fileName = WebConstants.WEBAPP_NAME + "-runtime.properties";
		}
		return fileName;
	}
	
	/**
	 * @see org.openmrs.web.filter.StartupFilter#getTemplatePrefix()
	 */
	@Override
	protected String getTemplatePrefix() {
		return "org/openmrs/web/filter/initialization/";
	}
	
	/**
	 * @see org.openmrs.web.filter.StartupFilter#getModel()
	 */
	@Override
	protected Object getModel() {
		return wizardModel;
	}
	
	/**
	 * @see org.openmrs.web.filter.StartupFilter#skipFilter()
	 */
	@Override
	public boolean skipFilter(HttpServletRequest httpRequest) {
		// If progress.vm makes an ajax request even immediately after initialization has completed
		// let the request pass in order to let progress.vm load the start page of OpenMRS
		// (otherwise progress.vm is displayed "forever")
		return !PROGRESS_VM_AJAXREQUEST.equals(httpRequest.getParameter("page")) && !initializationRequired();
	}
	
	/**
	 * Public method that returns true if database+runtime properties initialization is required
	 * 
	 * @return true if this initialization wizard needs to run
	 */
	public static boolean initializationRequired() {
		return !isInitializationComplete();
	}
	
	/**
	 * @param isInstallationStarted the value to set
	 */
	protected static synchronized void setInstallationStarted(boolean isInstallationStarted) {
		InitializationFilter.isInstallationStarted = isInstallationStarted;
	}
	
	/**
	 * @return true if installation has been started
	 */
	protected static boolean isInstallationStarted() {
		return isInstallationStarted;
	}
	
	/**
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		super.init(filterConfig);
		wizardModel = new InitializationWizardModel();
		//set whether need to do initialization work
		if (isDatabaseEmpty(OpenmrsUtil.getRuntimeProperties(WebConstants.WEBAPP_NAME))) {
			//if runtime-properties file doesn't exist, have to do initialization work
			setInitializationComplete(false);
		} else {
			//if database is not empty, then let UpdaterFilter to judge whether need database update
			setInitializationComplete(true);
		}
	}
	
	private void importTestDataSet(InputStream in, String connectionUrl, String connectionUsername, String connectionPassword)
	        throws IOException {
		File tempFile = null;
		FileOutputStream fileOut = null;
		try {
			ZipInputStream zipIn = new ZipInputStream(in);
			zipIn.getNextEntry();
			
			tempFile = File.createTempFile("testDataSet", "dump");
			fileOut = new FileOutputStream(tempFile);
			
			IOUtils.copy(zipIn, fileOut);
			
			fileOut.close();
			zipIn.close();
			
			//Cater for the stand-alone connection url with has :mxj:
			if (connectionUrl.contains(":mxj:")) {
				connectionUrl = connectionUrl.replace(":mxj:", ":");
			}
			
			URI uri = URI.create(connectionUrl.substring(5)); //remove 'jdbc:' prefix to conform to the URI format
			String host = uri.getHost();
			int port = uri.getPort();
			
			TestInstallUtil.addTestData(host, port, wizardModel.databaseName, connectionUsername, connectionPassword,
			    tempFile.getAbsolutePath());
		}
		finally {
			IOUtils.closeQuietly(in);
			IOUtils.closeQuietly(fileOut);
			
			if (tempFile != null) {
				tempFile.delete();
			}
		}
	}
	
	/**
	 * @param silent if this statement fails do not display stack trace or record an error in the
	 *            wizard object.
	 * @param user username to connect with
	 * @param pw password to connect with
	 * @param sql String containing sql and question marks
	 * @param args the strings to fill into the question marks in the given sql
	 * @return result of executeUpdate or -1 for error
	 */
	private int executeStatement(boolean silent, String user, String pw, String sql, String... args) {
		
		Connection connection = null;
		try {
			String replacedSql = sql;
			
			// TODO how to get the driver for the other dbs...
			if (wizardModel.databaseConnection.contains("mysql")) {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
			} else {
				replacedSql = replacedSql.replaceAll("`", "\"");
			}
			
			String tempDatabaseConnection = "";
			if (sql.contains("create database")) {
				tempDatabaseConnection = wizardModel.databaseConnection.replace("@DBNAME@", ""); // make this dbname agnostic so we can create the db
			} else {
				tempDatabaseConnection = wizardModel.databaseConnection.replace("@DBNAME@", wizardModel.databaseName);
			}
			
			connection = DriverManager.getConnection(tempDatabaseConnection, user, pw);
			
			for (String arg : args) {
				arg = arg.replace(";", "&#094"); // to prevent any sql injection
				replacedSql = replacedSql.replaceFirst("\\?", arg);
			}
			
			// run the sql statement
			Statement statement = connection.createStatement();
			return statement.executeUpdate(replacedSql);
			
		}
		catch (SQLException sqlex) {
			if (!silent) {
				// log and add error
				log.warn("error executing sql: " + sql, sqlex);
				errors.put("Error executing sql: " + sql + " - " + sqlex.getMessage(), null);
			}
		}
		catch (InstantiationException e) {
			log.error("Error generated", e);
		}
		catch (IllegalAccessException e) {
			log.error("Error generated", e);
		}
		catch (ClassNotFoundException e) {
			log.error("Error generated", e);
		}
		finally {
			try {
				if (connection != null) {
					connection.close();
				}
			}
			catch (Throwable t) {
				log.warn("Error while closing connection", t);
			}
		}
		
		return -1;
	}
	
	/**
	 * Convenience variable to know if this wizard has completed successfully and that this wizard
	 * does not need to be executed again
	 * 
	 * @return true if this has been run already
	 */
	synchronized private static boolean isInitializationComplete() {
		return initializationComplete;
	}
	
	/**
	 * Check if the given value is null or a zero-length String
	 * 
	 * @param value the string to check
	 * @param errors the list of errors to append the errorMessage to if value is empty
	 * @param errorMessageCode the string with code of error message translation to append if value
	 *            is empty
	 * @return true if the value is non-empty
	 */
	private boolean checkForEmptyValue(String value, Map<String, Object[]> errors, String errorMessageCode) {
		if (value != null && !value.equals("")) {
			return true;
		}
		errors.put(errorMessageCode, null);
		return false;
	}
	
	/**
	 * Separate thread that will run through all tasks to complete the initialization. The database
	 * is created, user's created, etc here
	 */
	private class InitializationCompletion {
		
		private Thread thread;
		
		private int steps = 0;
		
		private String message = "";
		
		private Map<String, Object[]> errors = new HashMap<String, Object[]>();
		
		private String errorPage = null;
		
		private boolean erroneous = false;
		
		private int completedPercentage = 0;
		
		private WizardTask executingTask;
		
		private List<WizardTask> executedTasks = new ArrayList<WizardTask>();
		
		synchronized public void reportError(String error, String errorPage, Object... params) {
			errors.put(error, params);
			this.errorPage = errorPage;
			erroneous = true;
		}
		
		synchronized public boolean hasErrors() {
			return erroneous;
		}
		
		synchronized public String getErrorPage() {
			return errorPage;
		}
		
		synchronized public Map<String, Object[]> getErrors() {
			return errors;
		}
		
		/**
		 * Start the completion stage. This fires up the thread to do all the work.
		 */
		public void start() {
			setStepsComplete(0);
			setInitializationComplete(false);
			thread.start();
		}
		
		public void waitForCompletion() {
			try {
				thread.join();
			}
			catch (InterruptedException e) {
				log.error("Error generated", e);
			}
		}
		
		synchronized protected void setStepsComplete(int steps) {
			this.steps = steps;
		}
		
		synchronized protected int getStepsComplete() {
			return steps;
		}
		
		synchronized public String getMessage() {
			return message;
		}
		
		synchronized public void setMessage(String message) {
			this.message = message;
			setStepsComplete(getStepsComplete() + 1);
		}
		
		/**
		 * @return the executingTask
		 */
		synchronized protected WizardTask getExecutingTask() {
			return executingTask;
		}
		
		/**
		 * @return the completedPercentage
		 */
		protected synchronized int getCompletedPercentage() {
			return completedPercentage;
		}
		
		/**
		 * @param completedPercentage the completedPercentage to set
		 */
		protected synchronized void setCompletedPercentage(int completedPercentage) {
			this.completedPercentage = completedPercentage;
		}
		
		/**
		 * Adds a task that has been completed to the list of executed tasks
		 * 
		 * @param task
		 */
		synchronized protected void addExecutedTask(WizardTask task) {
			this.executedTasks.add(task);
		}
		
		/**
		 * @param executingTask the executingTask to set
		 */
		synchronized protected void setExecutingTask(WizardTask executingTask) {
			this.executingTask = executingTask;
		}
		
		/**
		 * @return the executedTasks
		 */
		synchronized protected List<WizardTask> getExecutedTasks() {
			return this.executedTasks;
		}
		
		/**
		 * This class does all the work of creating the desired database, user, updates, etc
		 */
		public InitializationCompletion() {
			Runnable r = new Runnable() {
				
				/**
				 * TODO split this up into multiple testable methods
				 * 
				 * @see java.lang.Runnable#run()
				 */
				public void run() {
					try {
						String connectionUsername;
						String connectionPassword;
						
						if (!wizardModel.hasCurrentOpenmrsDatabase) {
							setMessage("Create database");
							setExecutingTask(WizardTask.CREATE_SCHEMA);
							// connect via jdbc and create a database
							String sql = "create database if not exists `?` default character set utf8";
							int result = executeStatement(false, wizardModel.createDatabaseUsername,
							    wizardModel.createDatabasePassword, sql, wizardModel.databaseName);
							// throw the user back to the main screen if this error occurs
							if (result < 0) {
								reportError(ErrorMessageConstants.ERROR_DB_CREATE_NEW, DEFAULT_PAGE);
								return;
							} else {
								wizardModel.workLog.add("Created database " + wizardModel.databaseName);
							}
							
							addExecutedTask(WizardTask.CREATE_SCHEMA);
						}
						
						if (wizardModel.createDatabaseUser) {
							setMessage("Create database user");
							setExecutingTask(WizardTask.CREATE_DB_USER);
							connectionUsername = wizardModel.databaseName + "_user";
							if (connectionUsername.length() > 16)
								connectionUsername = wizardModel.databaseName.substring(0, 11) + "_user"; // trim off enough to leave space for _user at the end
								
							connectionPassword = "";
							// generate random password from this subset of alphabet
							// intentionally left out these characters: ufsb$() to prevent certain words forming randomly
							String chars = "acdeghijklmnopqrtvwxyzACDEGHIJKLMNOPQRTVWXYZ0123456789.|~@#^&";
							Random r = new Random();
							for (int x = 0; x < 12; x++) {
								connectionPassword += chars.charAt(r.nextInt(chars.length()));
							}
							
							// connect via jdbc with root user and create an openmrs user
							String sql = "drop user '?'@'localhost'";
							executeStatement(true, wizardModel.createUserUsername, wizardModel.createUserPassword, sql,
							    connectionUsername);
							sql = "create user '?'@'localhost' identified by '?'";
							if (-1 != executeStatement(false, wizardModel.createUserUsername,
							    wizardModel.createUserPassword, sql, connectionUsername, connectionPassword)) {
								wizardModel.workLog.add("Created user " + connectionUsername);
							} else {
								// if error occurs stop
								reportError(ErrorMessageConstants.ERROR_DB_CREATE_DB_USER, DEFAULT_PAGE);
								return;
							}
							
							// grant the roles
							sql = "GRANT ALL ON `?`.* TO '?'@'localhost'";
							int result = executeStatement(false, wizardModel.createUserUsername,
							    wizardModel.createUserPassword, sql, wizardModel.databaseName, connectionUsername);
							// throw the user back to the main screen if this error occurs
							if (result < 0) {
								reportError(ErrorMessageConstants.ERROR_DB_GRANT_PRIV, DEFAULT_PAGE);
								return;
							} else {
								wizardModel.workLog.add("Granted user " + connectionUsername
								        + " all privileges to database " + wizardModel.databaseName);
							}
							
							addExecutedTask(WizardTask.CREATE_DB_USER);
						} else {
							connectionUsername = wizardModel.currentDatabaseUsername;
							connectionPassword = wizardModel.currentDatabasePassword;
						}
						
						String finalDatabaseConnectionString = wizardModel.databaseConnection.replace("@DBNAME@",
						    wizardModel.databaseName);
						
						// verify that the database connection works
						if (!verifyConnection(connectionUsername, connectionPassword, finalDatabaseConnectionString)) {
							setMessage("Verify that the database connection works");
							// redirect to setup page if we got an error
							reportError("Unable to connect to database", DEFAULT_PAGE);
							return;
						}
						
						// save the properties for startup purposes
						Properties runtimeProperties = new Properties();
						
						runtimeProperties.put("connection.url", finalDatabaseConnectionString);
						runtimeProperties.put("connection.username", connectionUsername);
						runtimeProperties.put("connection.password", connectionPassword);
						if (StringUtils.hasText(wizardModel.databaseDriver))
							runtimeProperties.put("connection.driver_class", wizardModel.databaseDriver);
						if (finalDatabaseConnectionString.contains("postgres"))
							runtimeProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
						if (finalDatabaseConnectionString.contains("sqlserver"))
							runtimeProperties.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
						runtimeProperties.put("module.allow_web_admin", wizardModel.moduleWebAdmin.toString());
						runtimeProperties.put("auto_update_database", wizardModel.autoUpdateDatabase.toString());
						runtimeProperties.put(OpenmrsConstants.ENCRYPTION_VECTOR_RUNTIME_PROPERTY, Base64.encode(Security
						        .generateNewInitVector()));
						runtimeProperties.put(OpenmrsConstants.ENCRYPTION_KEY_RUNTIME_PROPERTY, Base64.encode(Security
						        .generateNewSecretKey()));
						
						Properties properties = Context.getRuntimeProperties();
						properties.putAll(runtimeProperties);
						runtimeProperties = properties;
						Context.setRuntimeProperties(runtimeProperties);
						
						/**
						 * A callback class that prints out info about liquibase changesets
						 */
						class PrintingChangeSetExecutorCallback implements ChangeSetExecutorCallback {
							
							private int i = 1;
							
							private String message;
							
							public PrintingChangeSetExecutorCallback(String message) {
								this.message = message;
							}
							
							/**
							 * @see org.openmrs.util.DatabaseUpdater.ChangeSetExecutorCallback#executing(liquibase.ChangeSet,
							 *      int)
							 */
							@Override
							public void executing(ChangeSet changeSet, int numChangeSetsToRun) {
								setMessage(message + " (" + i++ + "/" + numChangeSetsToRun + "): Author: "
								        + changeSet.getAuthor() + " Comments: " + changeSet.getComments() + " Description: "
								        + changeSet.getDescription());
								setCompletedPercentage(Math.round(i * 100 / numChangeSetsToRun));
							}
							
						}
						
						if (wizardModel.createTables) {
							// use liquibase to create core data + tables
							try {
								setMessage("Executing " + LIQUIBASE_SCHEMA_DATA);
								setExecutingTask(WizardTask.CREATE_TABLES);
								DatabaseUpdater.executeChangelog(LIQUIBASE_SCHEMA_DATA, null,
								    new PrintingChangeSetExecutorCallback("OpenMRS schema file"));
								addExecutedTask(WizardTask.CREATE_TABLES);
								
								//reset for this task
								setCompletedPercentage(0);
								setExecutingTask(WizardTask.ADD_CORE_DATA);
								DatabaseUpdater.executeChangelog(LIQUIBASE_CORE_DATA, null,
								    new PrintingChangeSetExecutorCallback("OpenMRS core data file"));
								wizardModel.workLog.add("Created database tables and added core data");
								addExecutedTask(WizardTask.ADD_CORE_DATA);
								
							}
							catch (Exception e) {
								reportError(ErrorMessageConstants.ERROR_DB_CREATE_TABLES_OR_ADD_DEMO_DATA, DEFAULT_PAGE, e
								        .getMessage());
								log.warn("Error while trying to create tables and demo data", e);
							}
						}
						
						if (wizardModel.importTestData) {
							try {
								setMessage("Importing test data");
								setExecutingTask(WizardTask.IMPORT_TEST_DATA);
								setCompletedPercentage(0);
								
								try {
									InputStream inData = TestInstallUtil.getResourceInputStream(wizardModel.remoteUrl
									        + RELEASE_TESTING_MODULE_PATH + "generateTestDataSet.form",
									    wizardModel.remoteUsername, wizardModel.remotePassword);
									if (inData == null) {
										reportError(ErrorMessageConstants.ERROR_DB_IMPORT_TEST_DATA, DEFAULT_PAGE, "");
										return;
									}
									
									setCompletedPercentage(40);
									setMessage("Loading imported test data...");
									importTestDataSet(inData, finalDatabaseConnectionString, connectionUsername,
									    connectionPassword);
									wizardModel.workLog.add("Imported test data");
									addExecutedTask(WizardTask.IMPORT_TEST_DATA);
									
									//reset the progress for the next task
									setCompletedPercentage(0);
									setMessage("Importing modules from remote server...");
									setExecutingTask(WizardTask.ADD_MODULES);
									
									InputStream inModules = TestInstallUtil.getResourceInputStream(wizardModel.remoteUrl
									        + RELEASE_TESTING_MODULE_PATH + "getModules.htm", wizardModel.remoteUsername,
									    wizardModel.remotePassword);
									if (inModules == null) {
										reportError(ErrorMessageConstants.ERROR_DB_UNABLE_TO_FETCH_MODULES, DEFAULT_PAGE, "");
										return;
									}
									
									setCompletedPercentage(90);
									setMessage("Adding imported modules...");
									if (!TestInstallUtil.addZippedTestModules(inModules)) {
										reportError(ErrorMessageConstants.ERROR_DB_UNABLE_TO_ADD_MODULES, DEFAULT_PAGE, "");
										return;
									} else {
										wizardModel.workLog.add("Added Modules");
										addExecutedTask(WizardTask.ADD_MODULES);
									}
								}
								catch (APIAuthenticationException e) {
									log.warn("Unable to authenticate as a User with the System Developer role");
									reportError(ErrorMessageConstants.UPDATE_ERROR_UNABLE_AUTHENTICATE,
									    TESTING_REMOTE_DETAILS_SETUP, "");
									return;
								}
							}
							catch (Exception e) {
								reportError(ErrorMessageConstants.ERROR_DB_IMPORT_TEST_DATA, DEFAULT_PAGE, e.getMessage());
								log.warn("Error while trying to import test data", e);
								return;
							}
						}
						
						// add demo data only if creating tables fresh and user selected the option add demo data
						if (wizardModel.createTables && wizardModel.addDemoData) {
							try {
								setMessage("Adding demo data");
								setCompletedPercentage(0);
								setExecutingTask(WizardTask.ADD_DEMO_DATA);
								DatabaseUpdater.executeChangelog(LIQUIBASE_DEMO_DATA, null,
								    new PrintingChangeSetExecutorCallback("OpenMRS demo patients, users, and forms"));
								wizardModel.workLog.add("Added demo data");
								
								addExecutedTask(WizardTask.ADD_DEMO_DATA);
							}
							catch (Exception e) {
								reportError(ErrorMessageConstants.ERROR_DB_CREATE_TABLES_OR_ADD_DEMO_DATA, DEFAULT_PAGE, e
								        .getMessage());
								log.warn("Error while trying to add demo data", e);
							}
						}
						
						// update the database to the latest version
						try {
							setMessage("Updating the database to the latest version");
							setCompletedPercentage(0);
							setExecutingTask(WizardTask.UPDATE_TO_LATEST);
							DatabaseUpdater.executeChangelog(null, null, new PrintingChangeSetExecutorCallback(
							        "Updating database tables to latest version "));
							addExecutedTask(WizardTask.UPDATE_TO_LATEST);
						}
						catch (Exception e) {
							reportError(ErrorMessageConstants.ERROR_DB_UPDATE_TO_LATEST, DEFAULT_PAGE, e.getMessage());
							log.warn("Error while trying to update to the latest database version", e);
							return;
						}
						
						setExecutingTask(null);
						setMessage("Starting OpenMRS");
						
						// start spring
						// after this point, all errors need to also call: contextLoader.closeWebApplicationContext(event.getServletContext())
						// logic copied from org.springframework.web.context.ContextLoaderListener
						ContextLoader contextLoader = new ContextLoader();
						contextLoader.initWebApplicationContext(filterConfig.getServletContext());
						
						// start openmrs
						try {
							Context.openSession();
							
							// load core modules so that required modules are known at openmrs startup
							Listener.loadBundledModules(filterConfig.getServletContext());
							
							Context.startup(runtimeProperties);
						}
						catch (DatabaseUpdateException updateEx) {
							log.warn("Error while running the database update file", updateEx);
							reportError(ErrorMessageConstants.ERROR_DB_UPDATE, DEFAULT_PAGE, updateEx.getMessage());
							return;
						}
						catch (InputRequiredException inputRequiredEx) {
							// TODO display a page looping over the required input and ask the user for each.
							// 		When done and the user and put in their say, call DatabaseUpdater.update(Map);
							//		with the user's question/answer pairs
							log
							        .warn("Unable to continue because user input is required for the db updates and we cannot do anything about that right now");
							reportError(ErrorMessageConstants.ERROR_INPUT_REQ, DEFAULT_PAGE);
							return;
						}
						catch (MandatoryModuleException mandatoryModEx) {
							log.warn(
							    "A mandatory module failed to start. Fix the error or unmark it as mandatory to continue.",
							    mandatoryModEx);
							reportError(ErrorMessageConstants.ERROR_MANDATORY_MOD_REQ, DEFAULT_PAGE, mandatoryModEx
							        .getMessage());
							return;
						}
						catch (OpenmrsCoreModuleException coreModEx) {
							log
							        .warn(
							            "A core module failed to start. Make sure that all core modules (with the required minimum versions) are installed and starting properly.",
							            coreModEx);
							reportError(ErrorMessageConstants.ERROR_CORE_MOD_REQ, DEFAULT_PAGE, coreModEx.getMessage());
							return;
						}
						
						// TODO catch openmrs errors here and drop the user back out to the setup screen
						
						if (!wizardModel.implementationId.equals("")) {
							try {
								Context.addProxyPrivilege(PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES);
								Context.addProxyPrivilege(PrivilegeConstants.MANAGE_CONCEPT_SOURCES);
								Context.addProxyPrivilege(PrivilegeConstants.VIEW_CONCEPT_SOURCES);
								Context.addProxyPrivilege(PrivilegeConstants.MANAGE_IMPLEMENTATION_ID);
								
								ImplementationId implId = new ImplementationId();
								implId.setName(wizardModel.implementationIdName);
								implId.setImplementationId(wizardModel.implementationId);
								implId.setPassphrase(wizardModel.implementationIdPassPhrase);
								implId.setDescription(wizardModel.implementationIdDescription);
								
								Context.getAdministrationService().setImplementationId(implId);
							}
							catch (Throwable t) {
								reportError(ErrorMessageConstants.ERROR_SET_INPL_ID, DEFAULT_PAGE, t.getMessage());
								log.warn("Implementation ID could not be set.", t);
								Context.shutdown();
								WebModuleUtil.shutdownModules(filterConfig.getServletContext());
								contextLoader.closeWebApplicationContext(filterConfig.getServletContext());
								return;
							}
							finally {
								Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_GLOBAL_PROPERTIES);
								Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_CONCEPT_SOURCES);
								Context.removeProxyPrivilege(PrivilegeConstants.VIEW_CONCEPT_SOURCES);
								Context.removeProxyPrivilege(PrivilegeConstants.MANAGE_IMPLEMENTATION_ID);
							}
						}
						
						try {
							// change the admin user password from "test" to what they input above
							if (wizardModel.createTables) {
								Context.authenticate("admin", "test");
								Context.getUserService().changePassword("test", wizardModel.adminUserPassword);
								Context.logout();
							}
							
							// web load modules
							Listener.performWebStartOfModules(filterConfig.getServletContext());
							
							// start the scheduled tasks
							SchedulerUtil.startup(runtimeProperties);
						}
						catch (Throwable t) {
							Context.shutdown();
							WebModuleUtil.shutdownModules(filterConfig.getServletContext());
							contextLoader.closeWebApplicationContext(filterConfig.getServletContext());
							reportError(ErrorMessageConstants.ERROR_COMPLETE_STARTUP, DEFAULT_PAGE, t.getMessage());
							log.warn("Unable to complete the startup.", t);
							return;
						}
						
						// output properties to the openmrs runtime properties file so that this wizard is not run again
						FileOutputStream fos = null;
						try {
							fos = new FileOutputStream(getRuntimePropertiesFile());
							OpenmrsUtil.storeProperties(runtimeProperties, fos,
							    "Auto generated by OpenMRS initialization wizard");
							wizardModel.workLog.add("Saved runtime properties file " + getRuntimePropertiesFile());
							
							// don't need to catch errors here because we tested it at the beginning of the wizard
						}
						finally {
							if (fos != null) {
								fos.close();
							}
						}
						
						// set this so that the wizard isn't run again on next page load
						Context.closeSession();
					}
					catch (IOException e) {
						reportError(ErrorMessageConstants.ERROR_COMPLETE_STARTUP, DEFAULT_PAGE, e.getMessage());
					}
					finally {
						if (!hasErrors()) {
							// set this so that the wizard isn't run again on next page load
							setInitializationComplete(true);
							// we should also try to store selected by user language
							// if user wants to system will do it for him 
							FilterUtil.storeLocale(wizardModel.localeToSave);
						}
						setInstallationStarted(false);
					}
				}
			};
			
			thread = new Thread(r);
		}
	}
	
	/**
	 * Check whether openmrs database is empty. Having just one non-liquibase table in the given
	 * database qualifies this as a non-empty database.
	 * 
	 * @param props the runtime properties
	 * @return true/false whether openmrs database is empty or doesn't exist yet
	 */
	private static boolean isDatabaseEmpty(Properties props) {
		if (props != null) {
			String databaseConnectionFinalUrl = props.getProperty("connection.url");
			if (databaseConnectionFinalUrl == null)
				return true;
			
			String connectionUsername = props.getProperty("connection.username");
			if (connectionUsername == null)
				return true;
			
			String connectionPassword = props.getProperty("connection.password");
			if (connectionPassword == null)
				return true;
			
			Connection connection = null;
			try {
				DatabaseUtil.loadDatabaseDriver(databaseConnectionFinalUrl);
				connection = DriverManager.getConnection(databaseConnectionFinalUrl, connectionUsername, connectionPassword);
				
				DatabaseMetaData dbMetaData = (DatabaseMetaData) connection.getMetaData();
				
				String[] types = { "TABLE" };
				
				//get all tables
				ResultSet tbls = dbMetaData.getTables(null, null, null, types);
				
				while (tbls.next()) {
					String tableName = tbls.getString("TABLE_NAME");
					//if any table exist besides "liquibasechangelog" or "liquibasechangeloglock", return false
					if (!("liquibasechangelog".equals(tableName)) && !("liquibasechangeloglock".equals(tableName)))
						return false;
				}
				return true;
			}
			catch (Exception e) {
				//pass
			}
			finally {
				try {
					if (connection != null) {
						connection.close();
					}
				}
				catch (Throwable t) {
					//pass
				}
			}
			//if catch an exception while query database, then consider as database is empty.
			return true;
		} else
			return true;
	}
	
	/**
	 * Convenience method that loads the database driver
	 * 
	 * @param connection the database connection string
	 * @param databaseDriver the database driver class name to load
	 * @return the loaded driver string
	 */
	public static String loadDriver(String connection, String databaseDriver) {
		String loadedDriverString = null;
		try {
			loadedDriverString = DatabaseUtil.loadDatabaseDriver(connection, databaseDriver);
			log.info("using database driver :" + loadedDriverString);
		}
		catch (ClassNotFoundException e) {
			log.error("The given database driver class was not found. "
			        + "Please ensure that the database driver jar file is on the class path "
			        + "(like in the webapp's lib folder)");
		}
		
		return loadedDriverString;
	}
	
	/**
	 * Utility method that checks if there is a runtime properties file containing database
	 * connection credentials
	 * 
	 * @return
	 */
	private static boolean skipDatabaseSetupPage() {
		Properties props = OpenmrsUtil.getRuntimeProperties(WebConstants.WEBAPP_NAME);
		return (props != null && StringUtils.hasText(props.getProperty("connection.url"))
		        && StringUtils.hasText(props.getProperty("connection.username")) && StringUtils.hasText(props
		        .getProperty("connection.password")));
	}
	
	/**
	 * Utility methods that checks if the user clicked the back image
	 * 
	 * @param httpRequest
	 * @return
	 */
	private static boolean goBack(HttpServletRequest httpRequest) {
		return "Back".equals(httpRequest.getParameter("back"))
		        || (httpRequest.getParameter("back.x") != null && httpRequest.getParameter("back.y") != null);
	}
}
