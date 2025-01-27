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
package org.openmrs.api.context;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleException;
import org.openmrs.module.ModuleFactory;
import org.openmrs.scheduler.Task;
import org.openmrs.scheduler.timer.TimerSchedulerTask;
import org.openmrs.util.OpenmrsSecurityManager;

/**
 * This class allows certain tasks to run with elevated privileges. Primary use is scheduling and
 * module startup when there is no user to authenticate as.
 */
public class Daemon {
	
	/**
	 * The uuid defined for the daemon user object
	 */
	protected static final String DAEMON_USER_UUID = "A4F30A1B-5EB9-11DF-A648-37A07F9C90FB";
	
	protected static final ThreadLocal<Boolean> isDaemonThread = new ThreadLocal<Boolean>();
	
	/**
	 * This method should not be called directly. The {@link ModuleFactory#startModule(Module)}
	 * method uses this to start the given module in a new thread that is authenticated as the
	 * daemon user
	 * 
	 * @param module the module to start
	 * @returns the module returned from {@link ModuleFactory#startModuleInternal(Module)}
	 */
	public static Module startModule(final Module module) throws ModuleException {
		
		// create a new thread and execute that task in it
		DaemonThread startModuleThread = new DaemonThread() {
			
			@Override
			public void run() {
				isDaemonThread.set(true);
				try {
					Context.openSession();
					returnedObject = ModuleFactory.startModuleInternal(module);
				}
				catch (Throwable t) {
					exceptionThrown = t;
				}
				finally {
					Context.closeSession();
				}
			}
		};
		
		startModuleThread.start();
		
		// wait for the "startModule" thread to finish
		try {
			startModuleThread.join();
		}
		catch (InterruptedException e) {
			// ignore
		}
		
		if (startModuleThread.exceptionThrown != null) {
			if (startModuleThread.exceptionThrown instanceof ModuleException)
				throw (ModuleException) startModuleThread.exceptionThrown;
			else
				throw new ModuleException("Unable to start module as Daemon", startModuleThread.exceptionThrown);
		}
		
		return (Module) startModuleThread.returnedObject;
		
	}
	
	/**
	 * Executes the given task in a new thread that is authenticated as the daemon user. <br/>
	 * <br/>
	 * This can only be called from {@link TimerSchedulerTask} during actual task execution
	 * 
	 * @param task the task to run
	 * @should not be called from other methods other than TimerSchedulerTask
	 * @should not throw error if called from a TimerSchedulerTask class
	 */
	public static void executeScheduledTask(final Task task) throws Throwable {
		
		// quick check to make sure we're only being called by ourselves
		//Class<?> callerClass = Reflection.getCallerClass(0);
		Class<?> callerClass = new OpenmrsSecurityManager().getCallerClass(0);
		if (!TimerSchedulerTask.class.isAssignableFrom(callerClass))
			throw new APIException("This method can only be called from the TimerSchedulerTask class, not "
			        + callerClass.getName());
		
		// now create a new thread and execute that task in it
		DaemonThread executeTaskThread = new DaemonThread() {
			
			@Override
			public void run() {
				isDaemonThread.set(true);
				
				try {
					Context.openSession();
					TimerSchedulerTask.execute(task);
				}
				catch (Throwable t) {
					exceptionThrown = t;
				}
				finally {
					Context.closeSession();
				}
				
			}
		};
		
		executeTaskThread.start();
		
		// wait for the "executeTaskThread" thread to finish
		try {
			executeTaskThread.join();
		}
		catch (InterruptedException e) {
			// ignore
		}
		
		if (executeTaskThread.exceptionThrown != null)
			throw executeTaskThread.exceptionThrown;
		
	}
	
	/**
	 * Call this method if you are inside a Daemon thread (for example in a Module activator or a
	 * scheduled task) and you want to start up a new parallel Daemon thread. You may only call this
	 * method from a Daemon thread.
	 * 
	 * @param runnable what to run in a new thread
	 * @return the newly spawned {@link Thread}
	 * @should throw error if called from a non daemon thread
	 * @should not throw error if called from a daemon thread
	 */
	public static Thread runInNewDaemonThread(final Runnable runnable) {
		// make sure we're already in a daemon thread
		if (!isDaemonThread())
			throw new APIAuthenticationException("Only daemon threads can spawn new daemon threads");
		
		// we should consider making DaemonThread public, so the caller can access returnedObject and exceptionThrown
		DaemonThread thread = new DaemonThread() {
			
			@Override
			public void run() {
				isDaemonThread.set(true);
				try {
					Context.openSession();
					runnable.run();
				}
				finally {
					Context.closeSession();
				}
			}
		};
		
		thread.start();
		return thread;
	}
	
	/**
	 * @return true if the current thread was started by this class and so is a daemon thread that
	 *         has all privileges
	 * @see Context#hasPrivilege(String)
	 */
	public static boolean isDaemonThread() {
		Boolean b = isDaemonThread.get();
		if (b == null)
			return false;
		else
			return b.booleanValue();
	}
	
	/**
	 * Calls the {@link OpenmrsService#onStartup()} method, as a daemon, for an instance
	 * implementing the {@link OpenmrsService} interface.
	 * 
	 * @param openmrsService instance implementing the {@link OpenmrsService} interface.
	 * @since 1.9
	 */
	public static void runStartupForService(final OpenmrsService service) throws ModuleException {
		
		DaemonThread onStartupThread = new DaemonThread() {
			
			@Override
			public void run() {
				isDaemonThread.set(true);
				try {
					Context.openSession();
					service.onStartup();
				}
				catch (Throwable t) {
					exceptionThrown = t;
				}
				finally {
					Context.closeSession();
				}
			}
		};
		
		onStartupThread.start();
		
		// wait for the "onStartup" thread to finish
		try {
			onStartupThread.join();
		}
		catch (InterruptedException e) {
			// ignore
			e.printStackTrace();
		}
		
		if (onStartupThread.exceptionThrown != null) {
			if (onStartupThread.exceptionThrown instanceof ModuleException)
				throw (ModuleException) onStartupThread.exceptionThrown;
			else
				throw new ModuleException("Unable to run onStartup() method as Daemon", onStartupThread.exceptionThrown);
		}
	}
	
	/**
	 * Executes the given runnable in a new thread that is authenticated as the daemon user.
	 * 
	 * @param runnable an object implementing the {@link Runnable} interface.
	 * @return the newly spawned {@link Thread}
	 * @since 1.9
	 */
	public static Thread runInDaemonThread(final Runnable runnable) {
		
		DaemonThread thread = new DaemonThread() {
			
			@Override
			public void run() {
				isDaemonThread.set(true);
				try {
					Context.openSession();
					runnable.run();
				}
				finally {
					Context.closeSession();
				}
			}
		};
		
		thread.start();
		return thread;
	}
	
	/**
	 * Thread class used by the {@link Daemon#startModule(Module)} and
	 * {@link Daemon#executeScheduledTask(Task)} methods so that the returned object and the
	 * exception thrown can be returned to calling class
	 */
	protected static class DaemonThread extends Thread {
		
		/**
		 * The object returned from the method called in {@link #run()}
		 */
		protected Object returnedObject = null;
		
		/**
		 * The exception thrown (if any) by the method called in {@link #run()}
		 */
		protected Throwable exceptionThrown = null;
		
		/**
		 * Gets the exception thrown (if any) by the method called in {@link #run()}
		 * 
		 * @return the thrown exception (if any).
		 */
		public Throwable getExceptionThrown() {
			return exceptionThrown;
		}
	}
}
