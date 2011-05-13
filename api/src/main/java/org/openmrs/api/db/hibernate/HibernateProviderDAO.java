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
package org.openmrs.api.db.hibernate;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.IlikeExpression;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Provider;
import org.openmrs.ProviderAttributeType;
import org.openmrs.api.db.ProviderDAO;

/**
 * Hibernate specific Provider related functions. This class should not be used directly. All calls
 * should go through the {@link org.openmrs.api.ProviderService} methods.
 * 
 * @since 1.9
 */
public class HibernateProviderDAO implements ProviderDAO {
	
	private SessionFactory sessionFactory;
	
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @see org.openmrs.api.db.ProviderDAO#getAllProviders()
	 */
	@Override
	public List<Provider> getAllProviders(boolean includeRetired) {
		return getAll(includeRetired, Provider.class);
	}
	
	private Session getSession() {
		return sessionFactory.getCurrentSession();
	}
	
	/**
	 * @see org.openmrs.api.db.ProviderDAO#saveProvider(org.openmrs.Provider)
	 */
	public Provider saveProvider(Provider provider) {
		getSession().saveOrUpdate(provider);
		return provider;
	}
	
	/**
	 * @see org.openmrs.api.db.ProviderDAO#deleteProvider(org.openmrs.Provider)
	 */
	@Override
	public void deleteProvider(Provider provider) {
		getSession().delete(provider);
	}
	
	/**
	 * @see org.openmrs.api.db.ProviderDAO#getProvider(java.lang.Integer)
	 */
	@Override
	public Provider getProvider(Integer id) {
		return (Provider) getSession().load(Provider.class, id);
	}
	
	/**
	 * @see org.openmrs.api.db.ProviderDAO#getProviderByUuid(java.lang.String)
	 */
	@Override
	public Provider getProviderByUuid(String uuid) {
		return getByUuid(uuid, Provider.class);
	}
	
	/**
	 * @see org.openmrs.api.db.ProviderDAO#getProviders(java.lang.String,java.lang.String,
	 *      java.lang.Integer, java.lang.Integer)
	 */
	@Override
	public List<Provider> getProviders(String name, String identifier, Integer start, Integer length) {
		Criteria criteria = prepareProviderCriteria(name, identifier);
		if (start != null)
			criteria.setFirstResult(start);
		if (length != null)
			criteria.setMaxResults(length);
		List list = criteria.list();
		return list;
	}
	
	/**
	 * Creates a Provider Criteria based on name or identifier
	 * 
	 * @param name
	 * @param identifier
	 * @return Criteria
	 */
	private Criteria prepareProviderCriteria(String name, String identifier) {
		
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Provider.class).createAlias("person", "p",
		    Criteria.LEFT_JOIN);
		
		criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		if (name != null) {
			criteria.createAlias("p.names", "personName", Criteria.LEFT_JOIN);
			criteria.add(getNameSearchExpression(name));
		} else if (identifier != null) {
			criteria.add(Expression.ilike("identifier", identifier, MatchMode.START));
		}
		return criteria;
	}
	
	/**
	 * Creates Logical expression that matches the input name with Provider -Name or
	 * Provider-Person-Names(that are not voided)
	 * 
	 * @param name
	 * @return LogicalExpression
	 */
	private LogicalExpression getNameSearchExpression(String name) {
		
		MatchMode mode = MatchMode.START;
		IlikeExpression providerNameExpression = (IlikeExpression) Expression.ilike("name", name, mode);
		IlikeExpression givenName = (IlikeExpression) Expression.ilike("personName.givenName", name, mode);
		IlikeExpression middleName = (IlikeExpression) Expression.ilike("personName.middleName", name, mode);
		IlikeExpression familyName = (IlikeExpression) Expression.ilike("personName.familyName", name, mode);
		IlikeExpression familyName2 = (IlikeExpression) Expression.ilike("personName.familyName2", name, mode);
		LogicalExpression personNameExpression = Expression.and(Expression.eq("personName.voided", false), Expression.or(
		    familyName2, Expression.or(familyName, Expression.or(middleName, givenName))));
		return Expression.or(providerNameExpression, personNameExpression);
	}
	
	/**
	 * @see org.openmrs.api.db.ProviderDAO#getCountOfProviders(java.lang.String, java.lang.String)
	 */
	@Override
	public Integer getCountOfProviders(String name, String identifier) {
		Criteria criteria = prepareProviderCriteria(name, identifier);
		criteria.setProjection(Projections.countDistinct("providerId"));
		return (Integer) criteria.uniqueResult();
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.api.db.ProviderDAO#getAllProviderAttributeTypes(boolean)
	 */
	@Override
	public List<ProviderAttributeType> getAllProviderAttributeTypes(boolean includeRetired) {
		return getAll(includeRetired, ProviderAttributeType.class);
	}
	
	private <T> List<T> getAll(boolean includeRetired, Class<T> clazz) {
		Criteria criteria = getSession().createCriteria(clazz);
		if (!includeRetired) {
			criteria.add(Expression.eq("retired", false));
		} else {
			//push retired Provider to the end of the returned list
			criteria.addOrder(Order.asc("retired"));
		}
		criteria.addOrder(Order.asc("name"));
		return criteria.list();
	}
	
	private <T> T getByUuid(String uuid, Class<T> clazz) {
		Criteria criteria = getSession().createCriteria(clazz);
		criteria.add(Restrictions.eq("uuid", uuid));
		return (T) criteria.uniqueResult();
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.api.db.ProviderDAO#getProviderAttributeType(java.lang.Integer)
	 */
	@Override
	public ProviderAttributeType getProviderAttributeType(Integer providerAttributeTypeId) {
		return (ProviderAttributeType) getSession().load(ProviderAttributeType.class, providerAttributeTypeId);
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.api.db.ProviderDAO#getProviderAttributeTypeByUuid(java.lang.String)
	 */
	@Override
	public ProviderAttributeType getProviderAttributeTypeByUuid(String uuid) {
		return getByUuid(uuid, ProviderAttributeType.class);
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.api.db.ProviderDAO#saveProviderAttributeType(org.openmrs.ProviderAttributeType)
	 */
	@Override
	public ProviderAttributeType saveProviderAttributeType(ProviderAttributeType providerAttributeType) {
		getSession().saveOrUpdate(providerAttributeType);
		return providerAttributeType;
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.api.db.ProviderDAO#deleteProviderAttributeType(org.openmrs.ProviderAttributeType)
	 */
	@Override
	public void deleteProviderAttributeType(ProviderAttributeType providerAttributeType) {
		getSession().delete(providerAttributeType);
	}
	
}
