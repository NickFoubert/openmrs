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
package org.openmrs.hl7.db.hibernate;

import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.hl7.HL7InArchive;
import org.openmrs.hl7.HL7InError;
import org.openmrs.hl7.HL7InQueue;
import org.openmrs.hl7.HL7Source;
import org.openmrs.hl7.Hl7InArchivesMigrateThread;
import org.openmrs.hl7.db.HL7DAO;

/**
 * OpenMRS HL7 API database default hibernate implementation This class shouldn't be instantiated by
 * itself. Use the {@link org.openmrs.api.context.Context}
 * 
 * @see org.openmrs.hl7.HL7Service
 * @see org.openmrs.hl7.db.HL7DAO
 */
public class HibernateHL7DAO implements HL7DAO {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Hibernate session factory
	 */
	private SessionFactory sessionFactory;
	
	public HibernateHL7DAO() {
	}
	
	/**
	 * Set session factory
	 * 
	 * @param sessionFactory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#saveHL7Source(org.openmrs.hl7.HL7Source)
	 */
	public HL7Source saveHL7Source(HL7Source hl7Source) throws DAOException {
		sessionFactory.getCurrentSession().saveOrUpdate(hl7Source);
		return hl7Source;
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getHL7Source(java.lang.Integer)
	 */
	public HL7Source getHL7Source(Integer hl7SourceId) throws DAOException {
		return (HL7Source) sessionFactory.getCurrentSession().get(HL7Source.class, hl7SourceId);
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getHL7SourceByName(java.lang.String)
	 */
	public HL7Source getHL7SourceByName(String name) throws DAOException {
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(HL7Source.class);
		crit.add(Restrictions.eq("name", name));
		return (HL7Source) crit.uniqueResult();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getAllHL7Sources()
	 */
	@SuppressWarnings("unchecked")
	public List<HL7Source> getAllHL7Sources() throws DAOException {
		return sessionFactory.getCurrentSession().createQuery("from HL7Source").list();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#deleteHL7Source(org.openmrs.hl7.HL7Source)
	 */
	public void deleteHL7Source(HL7Source hl7Source) throws DAOException {
		sessionFactory.getCurrentSession().delete(hl7Source);
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#saveHL7InQueue(org.openmrs.hl7.HL7InQueue)
	 */
	public HL7InQueue saveHL7InQueue(HL7InQueue hl7InQueue) throws DAOException {
		sessionFactory.getCurrentSession().saveOrUpdate(hl7InQueue);
		return hl7InQueue;
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getHL7InQueue(java.lang.Integer)
	 */
	public HL7InQueue getHL7InQueue(Integer hl7InQueueId) throws DAOException {
		return (HL7InQueue) sessionFactory.getCurrentSession().get(HL7InQueue.class, hl7InQueueId);
	}
	
	@Override
	public HL7InQueue getHL7InQueueByUuid(String uuid) throws DAOException {
		return (HL7InQueue) sessionFactory.getCurrentSession().createCriteria(HL7InQueue.class).add(
		    Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getAllHL7InQueues()
	 */
	@SuppressWarnings("unchecked")
	public List<HL7InQueue> getAllHL7InQueues() throws DAOException {
		return sessionFactory.getCurrentSession()
		        .createQuery("from HL7InQueue where messageState = ? order by HL7InQueueId").setParameter(0,
		            HL7Constants.HL7_STATUS_PENDING, Hibernate.INTEGER).list();
	}
	
	/**
	 * creates a Criteria object for use with counting and finding HL7InQueue objects
	 * 
	 * @param messageState status of HL7InQueue object
	 * @param query string query to match against
	 * @return a Criteria object
	 */
	@SuppressWarnings("rawtypes")
	private Criteria getHL7SearchCriteria(Class clazz, Integer messageState, String query) throws DAOException {
		if (clazz == null)
			throw new DAOException("no class defined for HL7 search");
		
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(clazz);
		
		if (query != null && !query.isEmpty())
			if (clazz == HL7InError.class)
				crit.add(Restrictions.or(Restrictions.like("HL7Data", query, MatchMode.ANYWHERE), Restrictions.or(
				    Restrictions.like("errorDetails", query, MatchMode.ANYWHERE), Restrictions.like("error", query,
				        MatchMode.ANYWHERE))));
			else
				crit.add(Restrictions.like("HL7Data", query, MatchMode.ANYWHERE));
		
		if (messageState != null)
			crit.add(Restrictions.eq("messageState", messageState));
		
		return crit;
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getHL7InQueueBatch(java.lang.Class, int, int,
	 *      java.lang.Integer, java.lang.String)
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" })
	public <T> List<T> getHL7Batch(Class clazz, int start, int length, Integer messageState, String query)
	        throws DAOException {
		Criteria crit = getHL7SearchCriteria(clazz, messageState, query);
		crit.setFirstResult(start);
		crit.setMaxResults(length);
		crit.addOrder(Order.asc("dateCreated"));
		return crit.list();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#countHL7s(java.lang.Class, java.lang.Integer,
	 *      java.lang.String)
	 */
	@SuppressWarnings("rawtypes")
	public Integer countHL7s(Class clazz, Integer messageState, String query) {
		Criteria crit = getHL7SearchCriteria(clazz, messageState, query);
		crit.setProjection(Projections.rowCount());
		return (Integer) crit.uniqueResult();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getNextHL7InQueue()
	 */
	public HL7InQueue getNextHL7InQueue() throws DAOException {
		Query query = sessionFactory.getCurrentSession().createQuery(
		    "from HL7InQueue as hiq where hiq.messageState = ? order by HL7InQueueId").setParameter(0,
		    HL7Constants.HL7_STATUS_PENDING, Hibernate.INTEGER).setMaxResults(1);
		if (query == null)
			return null;
		return (HL7InQueue) query.uniqueResult();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#deleteHL7InQueue(org.openmrs.hl7.HL7InQueue)
	 */
	public void deleteHL7InQueue(HL7InQueue hl7InQueue) throws DAOException {
		sessionFactory.getCurrentSession().delete(hl7InQueue);
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#saveHL7InArchive(org.openmrs.hl7.HL7InArchive)
	 */
	public HL7InArchive saveHL7InArchive(HL7InArchive hl7InArchive) throws DAOException {
		sessionFactory.getCurrentSession().save(hl7InArchive);
		return hl7InArchive;
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getHL7InArchive(java.lang.Integer)
	 */
	public HL7InArchive getHL7InArchive(Integer hl7InArchiveId) throws DAOException {
		return (HL7InArchive) sessionFactory.getCurrentSession().get(HL7InArchive.class, hl7InArchiveId);
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getHL7InArchiveByState(Integer state)
	 */
	public List<HL7InArchive> getHL7InArchiveByState(Integer state) throws DAOException {
		return getHL7InArchiveByState(state, null);
	}
	
	/**
	 * limits results of getHL7InArchiveByState
	 */
	@SuppressWarnings("unchecked")
	private List<HL7InArchive> getHL7InArchiveByState(Integer state, Integer maxResults) throws DAOException {
		Query q = sessionFactory.getCurrentSession().createQuery("from HL7InArchive where messageState = ?").setParameter(0,
		    state, Hibernate.INTEGER);
		if (maxResults != null)
			q.setMaxResults(maxResults);
		return q.list();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getHL7InQueueByState(Integer stateId)
	 */
	@SuppressWarnings("unchecked")
	public List<HL7InQueue> getHL7InQueueByState(Integer state) throws DAOException {
		return sessionFactory.getCurrentSession().createQuery("from HL7InQueue where messageState = ?").setParameter(0,
		    state, Hibernate.INTEGER).list();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getAllHL7InArchives()
	 */
	public List<HL7InArchive> getAllHL7InArchives() throws DAOException {
		return getAllHL7InArchives(null);
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getAllHL7InArchives(Integer)
	 */
	@SuppressWarnings("unchecked")
	public List<HL7InArchive> getAllHL7InArchives(Integer maxResults) {
		Query q = sessionFactory.getCurrentSession().createQuery("from HL7InArchive order by HL7InArchiveId");
		if (maxResults != null)
			q.setMaxResults(maxResults);
		return q.list();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#deleteHL7InArchive(org.openmrs.hl7.HL7InArchive)
	 */
	public void deleteHL7InArchive(HL7InArchive hl7InArchive) throws DAOException {
		sessionFactory.getCurrentSession().delete(hl7InArchive);
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#saveHL7InError(HL7InError)
	 */
	public HL7InError saveHL7InError(HL7InError hl7InError) throws DAOException {
		sessionFactory.getCurrentSession().save(hl7InError);
		return hl7InError;
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getHL7InError(Integer)
	 */
	public HL7InError getHL7InError(Integer hl7InErrorId) throws DAOException {
		return (HL7InError) sessionFactory.getCurrentSession().get(HL7InError.class, hl7InErrorId);
	}
	
	@Override
	public HL7InError getHL7InErrorByUuid(String uuid) throws DAOException {
		return (HL7InError) sessionFactory.getCurrentSession().createCriteria(HL7InError.class).add(
		    Restrictions.eq("uuid", uuid)).uniqueResult();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getAllHL7InErrors()
	 */
	@SuppressWarnings("unchecked")
	public List<HL7InError> getAllHL7InErrors() throws DAOException {
		return sessionFactory.getCurrentSession().createQuery("from HL7InError order by HL7InErrorId").list();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#deleteHL7InError(HL7InError)
	 */
	public void deleteHL7InError(HL7InError hl7InError) throws DAOException {
		sessionFactory.getCurrentSession().delete(hl7InError);
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#garbageCollect()
	 */
	public void garbageCollect() {
		Context.clearSession();
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getHL7InArchiveByUuid(java.lang.String)
	 */
	public HL7InArchive getHL7InArchiveByUuid(String uuid) throws DAOException {
		Query query = sessionFactory.getCurrentSession().createQuery("from HL7InArchive where uuid = ?").setParameter(0,
		    uuid, Hibernate.STRING);
		Object record = query.uniqueResult();
		if (record == null)
			return null;
		return (HL7InArchive) record;
	}
	
	/**
	 * @see org.openmrs.hl7.db.HL7DAO#getHL7InArchivesToMigrate()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<HL7InArchive> getHL7InArchivesToMigrate() {
		Integer daysToKeep = Hl7InArchivesMigrateThread.getDaysKept();
		Criteria crit = getHL7SearchCriteria(HL7InArchive.class, HL7Constants.HL7_STATUS_PROCESSED, null);
		crit.setMaxResults(HL7Constants.MIGRATION_MAX_BATCH_SIZE);
		if (daysToKeep != null) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -1 * daysToKeep);
			crit.add(Restrictions.lt("dateCreated", cal.getTime()));
		}
		return crit.list();
	}
	
}
