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

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptComplex;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.ConceptNumeric;
import org.openmrs.ConceptProposal;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptReferenceTermMap;
import org.openmrs.ConceptSearchResult;
import org.openmrs.ConceptSet;
import org.openmrs.ConceptSource;
import org.openmrs.ConceptStopWord;
import org.openmrs.ConceptWord;
import org.openmrs.Drug;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.db.ConceptDAO;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contains methods pertaining to creating/updating/deleting/retiring Concepts, Drugs, Concept
 * Proposals, and all other things 'Concept'.
 * <p>
 * To get a list of concepts:
 * 
 * <pre>
 * 
 * List&lt;Concept&gt; concepts = Context.getConceptService().getAllConcepts();
 * </pre>
 * 
 * To get a single concept:
 * 
 * <pre>
 * 
 * // if there is a concept row in the database with concept_id = 3845
 * Concept concept = Context.getConceptService().getConcept(3845);
 * 
 * String name = concept.getPreferredName(Context.getLocale()).getName();
 * </pre>
 * 
 * To save a concept to the database
 * 
 * <pre>
 *   Concept concept = new Concept();
 *   concept.setConceptClass(Context.getConceptService().getConceptClass(3));
 *   concept.setDatatype(Context.getConceptService().getConceptDatatype(17));
 *   concept.setName...
 *   ... // and other required values on the concept
 *   Context.getConceptService().saveConcept(concept);
 * </pre>
 * 
 * @see org.openmrs.api.context.Context
 */
@Transactional
public interface ConceptService extends OpenmrsService {
	
	/**
	 * Sets the data access object for Concepts. The dao is used for saving and getting concepts
	 * to/from the database
	 * 
	 * @param dao The data access object to use
	 */
	public void setConceptDAO(ConceptDAO dao);
	
	/**
	 * @deprecated use #saveConcept(Concept)
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void createConcept(Concept concept) throws APIException;
	
	/**
	 * @deprecated use #saveConcept(Concept)
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void createConcept(ConceptNumeric concept) throws APIException;
	
	/**
	 * @deprecated use #saveConcept(Concept)
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateConcept(Concept concept) throws APIException;
	
	/**
	 * Get Concept by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Concept getConceptByUuid(String uuid);
	
	/**
	 * @deprecated use #saveConcept(Concept)
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateConcept(ConceptNumeric concept) throws APIException;
	
	/**
	 * @deprecated use #saveDrug(Drug)
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void createDrug(Drug drug) throws APIException;
	
	/**
	 * @deprecated use #saveDrug(Drug)
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateDrug(Drug drug) throws APIException;
	
	/**
	 * @deprecated use #purgeConcept(Concept concept)
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.PURGE_CONCEPTS })
	public void deleteConcept(Concept concept) throws APIException;
	
	/**
	 * @deprecated use {@link #retireConcept(Concept, String)}
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void voidConcept(Concept concept, String reason) throws APIException;
	
	/**
	 * Save or update the given <code>Concept</code> or <code>ConceptNumeric</code> in the database
	 * <p>
	 * If this is a new concept, the returned concept will have a new {@link Concept#getConceptId()}
	 * inserted into it that was generated by the database
	 * 
	 * @param concept The <code>Concept</code> or <code>ConceptNumeric</code> to save or update
	 * @return the <code>Concept</code> or <code>ConceptNumeric</code> that was saved or updated
	 * @throws APIException
	 * @throws ConceptsLockedException
	 * @throws ConceptInUseException
	 * @should put generated concept id onto returned concept
	 * @should create new concept in database
	 * @should update concept already existing in database
	 * @should generate id for new concept if none is specified
	 * @should keep id for new concept if one is specified
	 * @should save non ConceptNumeric object as conceptNumeric
	 * @should save a ConceptNumeric as a concept
	 * @should save a new ConceptNumeric
	 * @should void the conceptName if the text of the name has changed
	 * @should create a new conceptName when the old name is changed
	 * @should set a preferred name for each locale if none is marked
	 * @should not fail when a duplicate name is edited to a unique value
	 * @should create a reference term for a concept mapping on the fly when editing a concept
	 * @should create a reference term for a concept mapping on the fly when creating a concept
	 */
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public Concept saveConcept(Concept concept) throws APIException;
	
	/**
	 * Save or update the given <code>Drug</code> in the database. If this is a new drug, the
	 * returned drug object will have a new {@link Drug#getDrugId()} inserted into it that was
	 * generated by the database
	 * 
	 * @param drug The Drug to save or update
	 * @return the Drug that was saved or updated
	 * @throws APIException
	 * @should put generated drug id onto returned drug
	 * @should create new drug in database
	 * @should update drug already existing in database
	 */
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public Drug saveDrug(Drug drug) throws APIException;
	
	/**
	 * Completely purge a <code>Concept</code> or <code>ConceptNumeric</code> from the database.
	 * This should not typically be used unless desperately needed. Most should just be retired. See
	 * {@link #retireConcept(Concept, String)}
	 * 
	 * @param conceptOrConceptNumeric The <code>Concept</code> or <code>ConceptNumeric</code> to
	 *            remove from the system
	 * @throws APIException
	 * @should fail if any of the conceptNames of the concept is being used by an obs
	 */
	@Authorized(PrivilegeConstants.PURGE_CONCEPTS)
	public void purgeConcept(Concept conceptOrConceptNumeric) throws APIException;
	
	/**
	 * Retiring a concept essentially removes it from circulation
	 * 
	 * @param conceptOrConceptNumeric The <code>Concept</code> or <code>ConceptNumeric</code> to
	 *            retire
	 * @param reason The retire reason
	 * @return the retired <code>Concept</code> or <code>ConceptNumeric</code>
	 * @throws APIException
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	public Concept retireConcept(Concept conceptOrConceptNumeric, String reason) throws APIException;
	
	/**
	 * Retiring a Drug essentially removes it from circulation
	 * 
	 * @param drug The Drug to retire
	 * @param reason The retire reason
	 * @throws APIException
	 * @return the retired Drug
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	public Drug retireDrug(Drug drug, String reason) throws APIException;
	
	/**
	 * Marks a drug that is currently retired as not retired.
	 * 
	 * @param drug that is current set as retired
	 * @return the given drug, marked as not retired now, and saved to the db
	 * @throws APIException
	 * @should mark drug as retired
	 * @should not change attributes of drug that is already retired
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	public Drug unretireDrug(Drug drug) throws APIException;
	
	/**
	 * Completely purge a Drug from the database. This should not typically be used unless
	 * desperately needed. Most Drugs should just be retired.
	 * 
	 * @param drug The Drug to remove from the system
	 * @throws APIException
	 */
	@Authorized(PrivilegeConstants.PURGE_CONCEPTS)
	public void purgeDrug(Drug drug) throws APIException;
	
	/**
	 * Gets the concept with the given id
	 * 
	 * @param conceptId
	 * @return the matching Concept object
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Concept getConcept(Integer conceptId) throws APIException;
	
	/**
	 * Gets the concept-name with the given id
	 * 
	 * @param conceptNameId
	 * @return the matching Concept object
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptName getConceptName(Integer conceptNameId) throws APIException;
	
	/**
	 * Gets the ConceptAnswer with the given id
	 * 
	 * @param conceptAnswerId
	 * @return the matching ConceptAnswer object
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptAnswer getConceptAnswer(Integer conceptAnswerId) throws APIException;
	
	/**
	 * Get the Drug with the given id
	 * 
	 * @param drugId
	 * @return the matching Drug object
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Drug getDrug(Integer drugId) throws APIException;
	
	/**
	 * Get the ConceptNumeric with the given id
	 * 
	 * @param conceptId The ConceptNumeric id
	 * @return the matching ConceptNumeric object
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptNumeric getConceptNumeric(Integer conceptId) throws APIException;
	
	/**
	 * Return a Concept class matching the given identifier
	 * 
	 * @throws APIException
	 * @param conceptClassId the concept class identifier
	 * @return the matching ConceptClass
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptClass getConceptClass(Integer conceptClassId) throws APIException;
	
	/**
	 * Return a list of unretired concepts sorted by concept id ascending and
	 * 
	 * @return a List<Concept> object containing all of the sorted concepts
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getAllConcepts() throws APIException;
	
	/**
	 * Return a list of concepts sorted on sortBy in dir direction (asc/desc)
	 * 
	 * @param sortBy The property name to sort by; if null or invalid, concept_id is used.
	 * @param asc true = sort ascending; false = sort descending
	 * @param includeRetired If <code>true</code>, retired concepts will also be returned
	 * @return a List<Concept> object containing all of the sorted concepts
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getAllConcepts(String sortBy, boolean asc, boolean includeRetired) throws APIException;
	
	/**
	 * @deprecated use {@link #getAllConcepts(String, boolean, boolean)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getConcepts(String sortBy, String dir) throws APIException;
	
	/**
	 * Returns a list of concepts matching any part of a concept name, this method is case
	 * insensitive to the concept name string
	 * 
	 * @param name The search string
	 * @throws APIException
	 * @return a List<Concept> object containing all of the matching concepts
	 * @should pass irrespective of the case of the passed parameter
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getConceptsByName(String name) throws APIException;
	
	/**
	 * Return a Concept that matches the name exactly
	 * 
	 * @param name The search string
	 * @throws APIException
	 * @return the found Concept
	 * @should get concept by name
	 * @should get concept by partial name
	 * @should return null given null parameter
	 * @should find concepts with names in more specific locales
	 * @should find concepts with names in more generic locales
	 * @should find concepts with names in same specific locale
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Concept getConceptByName(String name) throws APIException;
	
	/**
	 * Get Concepts by id or name
	 * <p>
	 * Note: this just calls other impl methods; no DAO of its own
	 * 
	 * @param idOrName
	 * @return the found Concept
	 * @deprecated use {@link #getConcept(String)}
	 * @throws APIException
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Concept getConceptByIdOrName(String idOrName) throws APIException;
	
	/**
	 * Get Concept by id or name convenience method
	 * 
	 * @param conceptIdOrName
	 * @return the found Concept
	 * @throws APIException
	 * @should return null given null parameter
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Concept getConcept(String conceptIdOrName) throws APIException;
	
	/**
	 * @deprecated use
	 *             {@link #getConcepts(String, List, boolean, List, List, List, List, Concept, Integer, Integer)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptWord> getConceptWords(String phrase, List<Locale> locales, boolean includeRetired,
	        List<ConceptClass> requireClasses, List<ConceptClass> excludeClasses, List<ConceptDatatype> requireDatatypes,
	        List<ConceptDatatype> excludeDatatypes, Concept answersToConcept, Integer start, Integer size)
	        throws APIException;
	
	/**
	 * @deprecated use {@link #getConcepts(String, Locale)} that returns a list of
	 *             ConceptSearchResults
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptWord> getConceptWords(String phrase, Locale locale) throws APIException;
	
	/**
	 * @deprecated use
	 *             {@link #getConcepts(String, List, boolean, List, List, List, List, Concept, Integer, Integer)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptWord> findConcepts(String phrase, Locale locale, boolean includeRetired) throws APIException;
	
	/**
	 * @deprecated use
	 *             {@link #getConcepts(String, List, boolean, List, List, List, List, Concept, Integer, Integer)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptWord> findConcepts(String phrase, Locale locale, boolean includeRetired,
	        List<ConceptClass> requireClasses, List<ConceptClass> excludeClasses, List<ConceptDatatype> requireDatatypes,
	        List<ConceptDatatype> excludeDatatypes) throws APIException;
	
	/**
	 * Get Drug by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Drug getDrugByUuid(String uuid);
	
	/**
	 * @deprecated Use
	 *             {@link #getConceptWords(String, List, boolean, List, List, List, List, Concept, Integer, Integer)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptWord> findConcepts(String phrase, Locale locale, boolean includeRetired, int start, int size)
	        throws APIException;
	
	/**
	 * Return the drug object corresponding to the given name or drugId
	 * 
	 * @param drugNameOrId String name or drugId to match exactly on
	 * @return matching Drug object
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Drug getDrug(String drugNameOrId) throws APIException;
	
	/**
	 * Return the drug object corresponding to the given name or drugId
	 * 
	 * @param drugId String
	 * @throws APIException
	 * @return matching Drug object
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Drug getDrugByNameOrId(String drugId) throws APIException;
	
	/**
	 * @deprecated use {@link ConceptService#getAllDrugs()}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Drug> getDrugs() throws APIException;
	
	/**
	 * Return a list of drugs currently in the database that are not retired
	 * 
	 * @throws APIException
	 * @return a List<Drug> object containing all drugs
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Drug> getAllDrugs() throws APIException;
	
	/**
	 * @deprecated Use {@link #getDrugsByConcept(Concept)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Drug> getDrugs(Concept concept) throws APIException;
	
	/**
	 * Return a list of drugs associated with the given concept
	 * 
	 * @throws APIException
	 * @param concept
	 * @return a List<Drug> object containing all matching drugs
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Drug> getDrugsByConcept(Concept concept) throws APIException;
	
	/**
	 * Get drugs by concept. This method is the utility method that should be used to generically
	 * retrieve all Drugs in the system.
	 * 
	 * @param includeRetired If <code>true</code> then the search will include voided Drugs
	 * @return A List<Drug> object containing all matching Drugs
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Drug> getAllDrugs(boolean includeRetired);
	
	/**
	 * @deprecated Use {@link #getDrugs(String)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Drug> findDrugs(String phrase, boolean includeVoided) throws APIException;
	
	/**
	 * Find drugs in the system. The string search can match either drug.name or drug.concept.name
	 * 
	 * @param phrase Search phrase
	 * @throws APIException
	 * @return A List<Drug> object containing all Drug matches
	 * @should return drugs that are retired
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Drug> getDrugs(String phrase) throws APIException;
	
	/**
	 * @param cc ConceptClass
	 * @return Returns all concepts in a given class
	 * @throws APIException
	 * @should not fail due to no name in search
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getConceptsByClass(ConceptClass cc) throws APIException;
	
	/**
	 * Return a list of concept classes currently in the database
	 * 
	 * @throws APIException
	 * @return List<ConceptClass> object with all ConceptClass objects
	 * @deprecated use {@link #getAllConceptClasses(boolean)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_CLASSES)
	public List<ConceptClass> getConceptClasses() throws APIException;
	
	/**
	 * Return a Concept class matching the given name
	 * 
	 * @param name
	 * @return ConceptClass matching the given name
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_CLASSES)
	public ConceptClass getConceptClassByName(String name) throws APIException;
	
	/**
	 * Return a list of concept classes currently in the database
	 * 
	 * @throws APIException
	 * @return List<ConceptClass> object with all ConceptClass objects
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_CLASSES)
	public List<ConceptClass> getAllConceptClasses() throws APIException;
	
	/**
	 * Return a list of concept classes currently in the database
	 * 
	 * @param includeRetired include retired concept classes in the search results?
	 * @throws APIException
	 * @return List<ConceptClass> object with all ConceptClass objects
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_CLASSES)
	public List<ConceptClass> getAllConceptClasses(boolean includeRetired) throws APIException;
	
	/**
	 * Get ConceptClass by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_CLASSES)
	public ConceptClass getConceptClassByUuid(String uuid);
	
	/**
	 * Get ConceptAnswer by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptAnswer getConceptAnswerByUuid(String uuid);
	
	/**
	 * Get ConceptName by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptName getConceptNameByUuid(String uuid);
	
	/**
	 * Get ConceptSet by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptSet getConceptSetByUuid(String uuid);
	
	/**
	 * Get ConceptSource by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_SOURCES)
	public ConceptSource getConceptSourceByUuid(String uuid);
	
	/**
	 * Creates or updates a concept class
	 * 
	 * @param cc ConceptClass to create or update
	 * @throws APIException
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_CLASSES)
	public ConceptClass saveConceptClass(ConceptClass cc) throws APIException;
	
	/**
	 * Purge a ConceptClass
	 * 
	 * @param cc ConceptClass to delete
	 * @throws APIException
	 */
	@Authorized(PrivilegeConstants.PURGE_CONCEPT_CLASSES)
	public void purgeConceptClass(ConceptClass cc) throws APIException;
	
	/**
	 * Create or update a ConceptDatatype
	 * 
	 * @param cd ConceptDatatype to create or update
	 * @throws NotImplementedException
	 * @deprecated as of 1.9 because users should never change datatypes, it could harm data and
	 *             other code expecting them to be here
	 */
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPT_DATATYPES })
	@Deprecated
	public ConceptDatatype saveConceptDatatype(ConceptDatatype cd) throws NotImplementedException;
	
	/**
	 * Purge a ConceptDatatype. This removes the concept datatype from the database completely.
	 * 
	 * @param cd ConceptDatatype to purge
	 * @throws NotImplementedException
	 * @deprecated as of 1.9 because users should never delete datatypes, it could harm data and
	 *             other code expecting them to be here
	 */
	@Authorized(PrivilegeConstants.PURGE_CONCEPT_DATATYPES)
	@Deprecated
	public void purgeConceptDatatype(ConceptDatatype cd) throws NotImplementedException;
	
	/**
	 * Return a list of all concept datatypes currently in the database
	 * 
	 * @throws APIException
	 * @return List of ConceptDatatypes
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_DATATYPES)
	public List<ConceptDatatype> getAllConceptDatatypes() throws APIException;
	
	/**
	 * Return a list of concept datatypes currently in the database
	 * 
	 * @param includeRetired boolean - include the retired datatypes?
	 * @throws APIException
	 * @return List of ConceptDatatypes
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_DATATYPES)
	public List<ConceptDatatype> getAllConceptDatatypes(boolean includeRetired) throws APIException;
	
	/**
	 * Find concept datatypes that contain the given name string
	 * 
	 * @deprecated you *probably* want to use
	 *             {@link ConceptService#getConceptDatatypeByName(String)}
	 * @param name
	 * @return List<ConceptDatatype> object of ConceptDatatypes matching the string
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_DATATYPES)
	@Deprecated
	public List<ConceptDatatype> getConceptDatatypes(String name) throws APIException;
	
	/**
	 * Return a ConceptDatatype matching the given identifier
	 * 
	 * @param i Integer for the requested ConceptDatatype
	 * @return ConceptDatatype matching the given identifier
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_DATATYPES)
	public ConceptDatatype getConceptDatatype(Integer i) throws APIException;
	
	/**
	 * Get ConceptDatatype by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_DATATYPES)
	public ConceptDatatype getConceptDatatypeByUuid(String uuid);
	
	/**
	 * Return a Concept datatype matching the given name
	 * 
	 * @param name
	 * @return ConceptDatatype matching the given name
	 * @throws APIException
	 * @should return an exact match on name
	 * @should not return a fuzzy match on name
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_DATATYPES)
	public ConceptDatatype getConceptDatatypeByName(String name) throws APIException;
	
	/**
	 * Updates the concept set derived business table for this concept (bursting the concept sets)
	 * 
	 * @param concept
	 * @throws APIException
	 */
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateConceptSetDerived(Concept concept) throws APIException;
	
	/**
	 * Iterates over all concepts calling updateConceptSetDerived(concept)
	 * 
	 * @throws APIException
	 */
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateConceptSetDerived() throws APIException;
	
	/**
	 * @deprecated use {@link #getConceptSetsByConcept(Concept)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptSet> getConceptSets(Concept concept) throws APIException;
	
	/**
	 * Return a list of the concept sets with concept_set matching concept
	 * <p>
	 * For example to find all concepts for ARVs, you would do
	 * getConceptSets(getConcept("ANTIRETROVIRAL MEDICATIONS")) and then take the conceptIds from
	 * the resulting list.
	 * 
	 * @param concept The concept representing the concept set
	 * @return A List<ConceptSet> object containing all matching ConceptSets
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptSet> getConceptSetsByConcept(Concept concept) throws APIException;
	
	/**
	 * @deprecated use {@link #getConceptsByConceptSet(Concept)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getConceptsInSet(Concept concept) throws APIException;
	
	/**
	 * Return a List of all concepts within a concept set
	 * 
	 * @param concept The concept representing the concept set
	 * @return A List<Concept> object containing all objects within the ConceptSet
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getConceptsByConceptSet(Concept concept) throws APIException;
	
	/**
	 * Find all sets that the given concept is a member of
	 * 
	 * @param concept
	 * @throws APIException
	 * @return A List<ConceptSet> object with all parent concept sets
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptSet> getSetsContainingConcept(Concept concept) throws APIException;
	
	/**
	 * @deprecated use {@link #getAllConceptProposals(boolean)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_PROPOSALS)
	public List<ConceptProposal> getConceptProposals(boolean includeCompleted) throws APIException;
	
	/**
	 * Get a List of all concept proposals
	 * 
	 * @param includeCompleted boolean - include completed proposals as well?
	 * @return a List<ConceptProposal> object of all found ConceptProposals
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_PROPOSALS)
	public List<ConceptProposal> getAllConceptProposals(boolean includeCompleted) throws APIException;
	
	/**
	 * Get ConceptNumeric by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptNumeric getConceptNumericByUuid(String uuid);
	
	/**
	 * Get a ConceptProposal by conceptProposalId
	 * 
	 * @param conceptProposalId the Integer concept proposal Id
	 * @return the found ConceptProposal
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_PROPOSALS)
	public ConceptProposal getConceptProposal(Integer conceptProposalId) throws APIException;
	
	/**
	 * Find matching concept proposals
	 * 
	 * @param text
	 * @return a List<ConceptProposal> object containing matching concept proposals
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_PROPOSALS)
	public List<ConceptProposal> getConceptProposals(String text) throws APIException;
	
	/**
	 * @deprecated Use {@link #getProposedConcepts(String)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_PROPOSALS)
	public List<Concept> findProposedConcepts(String text) throws APIException;
	
	/**
	 * Find matching proposed concepts
	 * 
	 * @param text
	 * @return a List<Concept> object containing matching proposed concepts
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_PROPOSALS)
	public List<Concept> getProposedConcepts(String text) throws APIException;
	
	/**
	 * @deprecated use
	 *             {@link #getConcepts(String, List, boolean, List, List, List, List, Concept, Integer, Integer)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized( { "View Concepts" })
	public List<ConceptWord> findConcepts(String phrase, List<Locale> searchLocales, boolean includeRetired,
	        List<ConceptClass> requireClasses, List<ConceptClass> excludeClasses, List<ConceptDatatype> requireDatatypes,
	        List<ConceptDatatype> excludeDatatypes);
	
	/**
	 * @deprecated use {@link #saveConceptProposal(ConceptProposal)}
	 */
	@Deprecated
	@Authorized(PrivilegeConstants.ADD_CONCEPT_PROPOSALS)
	public void proposeConcept(ConceptProposal conceptProposal) throws APIException;
	
	/**
	 * Saves/updates/proposes a concept proposal
	 * 
	 * @param conceptProposal The ConceptProposal to save
	 * @throws APIException
	 * @return the saved/updated ConceptProposal object
	 */
	@Authorized( { PrivilegeConstants.ADD_CONCEPT_PROPOSALS, PrivilegeConstants.EDIT_CONCEPT_PROPOSALS })
	public ConceptProposal saveConceptProposal(ConceptProposal conceptProposal) throws APIException;
	
	/**
	 * Removes a concept proposal from the database entirely.
	 * 
	 * @param cp
	 * @throws APIException
	 */
	@Authorized(PrivilegeConstants.PURGE_CONCEPT_PROPOSALS)
	public void purgeConceptProposal(ConceptProposal cp) throws APIException;
	
	/**
	 * Maps a concept proposal to a concept
	 * 
	 * @param cp
	 * @param mappedConcept
	 * @return the mappedConcept
	 * @throws APIException
	 * @should not require mapped concept on reject action
	 * @should allow rejecting proposals
	 * @should throw APIException when mapping to null concept
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	public Concept mapConceptProposalToConcept(ConceptProposal cp, Concept mappedConcept) throws APIException;
	
	/**
	 * Maps a concept proposal to a concept
	 * 
	 * @param cp
	 * @param mappedConcept
	 * @param locale of concept proposal
	 * @return the mappedConcept
	 * @throws APIException
	 * @should not require mapped concept on reject action
	 * @should allow rejecting proposals
	 * @should throw APIException when mapping to null concept
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPTS)
	public Concept mapConceptProposalToConcept(ConceptProposal cp, Concept mappedConcept, Locale locale) throws APIException;
	
	/**
	 * @deprecated use {@link ConceptProposal#rejectConceptProposal()}
	 */
	@Deprecated
	@Authorized(PrivilegeConstants.EDIT_CONCEPT_PROPOSALS)
	public void rejectConceptProposal(ConceptProposal cp) throws APIException;
	
	/**
	 * @deprecated use {@link ConceptService#getConceptProposals(String)}
	 */
	@Deprecated
	@Authorized(PrivilegeConstants.ADD_CONCEPT_PROPOSALS)
	public List<ConceptProposal> findMatchingConceptProposals(String text);
	
	/**
	 * @deprecated use {@link #findConceptAnswers(String, Locale, Concept)}
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptWord> findConceptAnswers(String phrase, Locale locale, Concept concept, boolean includeRetired)
	        throws APIException;
	
	/**
	 * @deprecated use {@link #findConceptAnswers(String, Locale, Concept)}
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	@Deprecated
	public List<ConceptWord> getConceptAnswers(String phrase, Locale locale, Concept concept) throws APIException;
	
	/**
	 * @deprecated use #getConceptsByAnswer(Concept)
	 */
	@Deprecated
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getQuestionsForAnswer(Concept concept) throws APIException;
	
	/**
	 * Returns all possible Concepts to which this concept is a value-coded answer. To navigate in
	 * the other direction, i.e., from Concept to its answers use Concept.getAnswers()
	 * 
	 * @param concept
	 * @return A List<Concept> containing all possible questions to which this concept is a
	 *         valued_Coded answer
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getConceptsByAnswer(Concept concept) throws APIException;
	
	/**
	 * Finds the previous concept in the dictionary that has the next lowest concept id
	 * 
	 * @param concept the offset Concept
	 * @return the foundConcept
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Concept getPrevConcept(Concept concept) throws APIException;
	
	/**
	 * Finds the next concept in the dictionary that has the next largest concept id
	 * 
	 * @param concept the offset Concept
	 * @return the foundConcept
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Concept getNextConcept(Concept concept) throws APIException;
	
	/**
	 * Check if the concepts are locked and if so, throw exception during manipulation of concept
	 * 
	 * @throws ConceptsLockedException
	 */
	@Transactional(readOnly = true)
	public void checkIfLocked() throws ConceptsLockedException;
	
	/**
	 * Get ConceptProposal by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_PROPOSALS)
	public ConceptProposal getConceptProposalByUuid(String uuid);
	
	/**
	 * Convenience method for finding concepts associated with drugs in formulary.
	 * 
	 * @return A List<Concept> object of all concepts that occur as a Drug.concept.
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getConceptsWithDrugsInFormulary() throws APIException;
	
	/**
	 * @deprecated use {@link #updateConceptWord(Concept)}
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateConceptWord(Concept concept) throws APIException;
	
	/**
	 * @deprecated use {@link #updateConceptWords()}
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateConceptWords() throws APIException;
	
	/**
	 * Get ConceptNameTag by its UUID
	 * 
	 * @param uuid
	 * @return the conceptNameTag with a matching uuid
	 * @see Concept#setPreferredName(ConceptName)
	 * @see Concept#setFullySpecifiedName(ConceptName)
	 * @see Concept#setShortName(ConceptName)
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptNameTag getConceptNameTagByUuid(String uuid);
	
	/**
	 * @deprecated use {@link #updateConceptIndexes(Integer, Integer)}
	 */
	@Deprecated
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateConceptWords(Integer conceptIdStart, Integer conceptIdEnd) throws APIException;
	
	/**
	 * Get a ComplexConcept with the given conceptId
	 * 
	 * @param conceptId of the ComplexConcept
	 * @return a ConceptComplex object
	 * @since 1.5
	 * @should return a concept complex object
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptComplex getConceptComplex(Integer conceptId);
	
	/**
	 * Search for a ConceptNameTag by name
	 * 
	 * @param tag String name of ConceptNameTag
	 * @return ConceptNameTag matching the given String tag
	 * @see Concept#getPreferredName(Locale)
	 * @see Concept#getFullySpecifiedName(Locale)
	 * @see Concept#getShortNameInLocale(Locale)
	 * @see Concept#getShortestName(Locale, Boolean)
	 */
	@Transactional(readOnly = true)
	@Authorized( { PrivilegeConstants.VIEW_CONCEPTS })
	public ConceptNameTag getConceptNameTagByName(String tag);
	
	/**
	 * Gets the set of unique Locales used by existing concept names.
	 * 
	 * @return set of used Locales
	 */
	@Transactional(readOnly = true)
	public Set<Locale> getLocalesOfConceptNames();
	
	/**
	 * Return a list of concept sources currently in the database that are not voided
	 * 
	 * @return List of Concept source objects
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_SOURCES)
	public List<ConceptSource> getAllConceptSources() throws APIException;
	
	/**
	 * Return a Concept source matching the given concept source id
	 * 
	 * @param i Integer conceptSourceId
	 * @return ConceptSource
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_SOURCES)
	public ConceptSource getConceptSource(Integer i) throws APIException;
	
	/**
	 * Create a new ConceptSource
	 * 
	 * @param conceptSource ConceptSource to create
	 * @throws APIException
	 * @should not set creator if one is supplied already
	 * @should not set date created if one is supplied already
	 * @should save a ConceptSource with a null hl7Code
	 * @should not save a ConceptSource if voided is null
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_SOURCES)
	public ConceptSource saveConceptSource(ConceptSource conceptSource) throws APIException;
	
	/**
	 * Delete ConceptSource
	 * 
	 * @param cs ConceptSource object delete
	 * @throws APIException
	 */
	@Authorized(PrivilegeConstants.PURGE_CONCEPT_SOURCES)
	public ConceptSource purgeConceptSource(ConceptSource cs) throws APIException;
	
	/**
	 * This effectively removes a concept source from the database. The source can still be
	 * referenced by old data, but no new data should use this source.
	 * 
	 * @param cs the concept source to retire
	 * @param reason why the concept source is to be retired, must not be empty of null
	 * @return the retired concept source
	 * @throws APIException
	 * @should retire concept source
	 */
	@Authorized(PrivilegeConstants.PURGE_CONCEPT_SOURCES)
	public ConceptSource retireConceptSource(ConceptSource cs, String reason) throws APIException;
	
	/**
	 * Creates a new Concept name tag if none exists. If a tag exists with the same name then that
	 * existing tag is returned.
	 * 
	 * @param nameTag the concept name tag to be saved
	 * @return the newly created or existing concept name tag
	 * @should save a concept name tag if tag does not exist
	 * @should not save a concept name tag if tag exists
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_NAME_TAGS)
	public ConceptNameTag saveConceptNameTag(ConceptNameTag nameTag);
	
	/**
	 * Gets the highest concept-id used by a concept.
	 * 
	 * @return highest concept-id
	 */
	@Transactional(readOnly = true)
	public Integer getMaxConceptId();
	
	/**
	 * Returns an iterator for all concepts, including retired and expired.
	 * 
	 * @return the Iterator
	 * @should start with the smallest concept id
	 * @should iterate over all concepts
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Iterator<Concept> conceptIterator();
	
	/**
	 * Looks up a concept via {@link ConceptMap} This will return the {@link Concept} which contains
	 * a {@link ConceptMap} entry whose <code>sourceCode</code> is equal to the passed
	 * <code>conceptCode</code> and whose {@link ConceptSource} has either a <code>name</code> or
	 * <code>hl7Code</code> that is equal to the passed <code>mappingCode</code>. Delegates to
	 * getConceptByMapping(code,sourceName,includeRetired) with includeRetired=true
	 * 
	 * @param code the code associated with a concept within a given {@link ConceptSource}
	 * @param sourceName the name or hl7Code of the {@link ConceptSource} to check
	 * @return the {@link Concept} that has the given mapping, or null if no {@link Concept} found
	 * @throws APIException
	 * @should get concept with given code and and source hl7 code
	 * @should get concept with given code and source name
	 * @should return null if source code does not exist
	 * @should return null if mapping does not exist
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Concept getConceptByMapping(String code, String sourceName) throws APIException;
	
	/**
	 * Looks up a concept via {@link ConceptMap} This will return the {@link Concept} which contains
	 * a {@link ConceptMap} entry whose <code>sourceCode</code> is equal to the passed
	 * <code>conceptCode</code> and whose {@link ConceptSource} has either a <code>name</code> or
	 * <code>hl7Code</code> that is equal to the passed <code>mappingCode</code> . Operates under
	 * the assumption that each mappingCode in a {@link ConceptSource} references one and only one
	 * non-retired {@link Concept): if the underlying dao method returns more than one non-retired concept, this
	 * method will throw an exception; if the underlying dao method returns more than one concept, but
	 * only one non-retired concept, this method will return the non-retired concept; if the dao only
	 * returns retired concepts, this method will simply return the first concept in the list returns by
	 * the dao method; retired concepts can be excluded by setting the includeRetired parameter to false,
	 * but the above logic still applies
	 * 
	 * @param code the code associated with a concept within a given {@link ConceptSource}
	 * @param sourceName the name or hl7Code of the {@link ConceptSource} to check
	 * @param includeRetired whether or not to include retired concepts
	 * @return the {@link Concept} that has the given mapping, or null if no {@link Concept} found
	 * @throws APIException
	 * @should get concept with given code and and source hl7 code
	 * @should get concept with given code and source name
	 * @should return null if source code does not exist
	 * @should return null if mapping does not exist
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Concept getConceptByMapping(String code, String sourceName, Boolean includeRetired) throws APIException;
	
	/**
	 * Looks up a concept via {@link ConceptMap} This will return the list of concepts
	 * {@link Concept}s which contain a {@link ConceptMap} entry whose <code>sourceCode</code> is
	 * equal to the passed <code>conceptCode</code> and whose {@link ConceptSource} has either a
	 * <code>name</code> or <code>hl7Code</code> that is equal to the passed
	 * <code>mappingCode</code>
	 * 
	 * @param code the code associated with a concept within a given {@link ConceptSource}
	 * @param sourceName the name or hl7Code of the {@link ConceptSource} to check
	 * @return the list of non-voided {@link Concept}s that has the given mapping, or null if no
	 *         {@link Concept} found
	 * @throws APIException if the specified source+code maps to more than one concept
	 * @should get concepts with given code and and source hl7 code
	 * @should get concepts with given code and source name
	 * @should return empty list if source code does not exist
	 * @should return empty list if mapping does not exist
	 * @should include retired concepts
	 * @since 1.8
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getConceptsByMapping(String code, String sourceName) throws APIException;
	
	/**
	 * Looks up a concept via {@link ConceptMap} This will return the list of {@link Concept}s which
	 * contain a {@link ConceptMap} entry whose <code>sourceCode</code> is equal to the passed
	 * <code>conceptCode</code> and whose {@link ConceptSource} has either a <code>name</code> or
	 * <code>hl7Code</code> that is equal to the passed <code>mappingCode</code>. Delegates to
	 * getConceptsByMapping(code,sourceName,includeRetired) with includeRetired=true
	 * 
	 * @param code the code associated with a concept within a given {@link ConceptSource}
	 * @param sourceName the name or hl7Code of the {@link ConceptSource} to check
	 * @param includeRetired whether or not to include retired concepts
	 * @return the list of non-voided {@link Concept}s that has the given mapping, or null if no
	 *         {@link Concept} found
	 * @throws APIException if the specified source+code maps to more than one concept
	 * @should get concepts with given code and and source hl7 code
	 * @should get concepts with given code and source name
	 * @should return empty list if source code does not exist
	 * @should return empty list if mapping does not exist
	 * @since 1.8
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getConceptsByMapping(String code, String sourceName, boolean includeRetired) throws APIException;
	
	/**
	 * Get all the concept name tags defined in the database, included voided ones
	 * 
	 * @since 1.5
	 * @return a list of the concept name tags stored in the
	 */
	@Transactional(readOnly = true)
	public List<ConceptNameTag> getAllConceptNameTags();
	
	/**
	 * Gets the {@link ConceptNameTag} with the given database primary key
	 * 
	 * @param id the concept name tag id to find
	 * @return the matching {@link ConceptNameTag} or null if none found
	 * @since 1.5
	 */
	@Transactional(readOnly = true)
	@Authorized( { PrivilegeConstants.VIEW_CONCEPTS })
	public ConceptNameTag getConceptNameTag(Integer id);
	
	/**
	 * Get ConceptDescription by its UUID
	 * 
	 * @param uuid
	 * @return
	 * @should find object given valid uuid
	 * @should return null if no object found with given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized( { PrivilegeConstants.VIEW_CONCEPTS })
	public ConceptDescription getConceptDescriptionByUuid(String uuid);
	
	/**
	 * Lookup a ConceptSource by its name property
	 * 
	 * @param conceptSourceName
	 * @return ConceptSource
	 * @throws APIException
	 * @should get ConceptSource with the given name
	 * @should return null if no ConceptSource with that name is found
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public ConceptSource getConceptSourceByName(String conceptSourceName) throws APIException;
	
	/**
	 * Looks up a list of ConceptMaps for a given ConceptSource
	 * 
	 * @deprecated as of version 1.9, use {@link #getConceptMapsBySource(ConceptSource))}
	 * @param conceptSource
	 * @return a List<ConceptMap> objects
	 * @throws APIException
	 * @should return a List of ConceptMaps if concept mappings found
	 * @should return empty List of ConceptMaps if none found
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptMap> getConceptsByConceptSource(ConceptSource conceptSource) throws APIException;
	
	/**
	 * Checks if there are any observations (including voided observations) for a concept.
	 * 
	 * @param concept which used or not used by an observation
	 * @return boolean true if the concept is used by an observation
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public boolean hasAnyObservation(Concept concept);
	
	/**
	 * Returns the TRUE concept
	 * 
	 * @return true concept
	 * @should return the true concept
	 */
	@Transactional(readOnly = true)
	public Concept getTrueConcept();
	
	/**
	 * Returns the FALSE concept
	 * 
	 * @return false concept
	 * @should return the false concept
	 */
	@Transactional(readOnly = true)
	public Concept getFalseConcept();
	
	/**
	 * Changes the datatype of a concept from boolean to coded when it has observations it is
	 * associated to.
	 * 
	 * @param conceptToChange the concept which to change
	 * @throws APIException
	 * @should convert the datatype of a boolean concept to coded
	 * @should fail if the datatype of the concept is not boolean
	 * @should explicitly add true concept as a value_Coded answer
	 * @should explicitly add false concept as a value_Coded answer
	 */
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void convertBooleanConceptToCoded(Concept conceptToChange) throws APIException;
	
	/**
	 * Checks if there are any observations (including voided observations) using a conceptName.
	 * 
	 * @param conceptName which is used or not used by an observation
	 * @return boolean true if the conceptName is used by an observation otherwise false
	 * @throws APIException
	 * @since Version 1.7
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public boolean hasAnyObservation(ConceptName conceptName) throws APIException;
	
	/**
	 * Searches for concepts by the given parameters.
	 * 
	 * @param phrase matched to the start of any word in any of the names of a concept (if
	 *            blank/null, matches all concepts)
	 * @param locales List<Locale> to restrict to
	 * @param includeRetired boolean if false, will exclude retired concepts
	 * @param requireClasses List<ConceptClass> to restrict to
	 * @param excludeClasses List<ConceptClass> to leave out of results
	 * @param requireDatatypes List<ConceptDatatype> to restrict to
	 * @param excludeDatatypes List<ConceptDatatype> to leave out of results
	 * @param answersToConcept all results will be a possible answer to this concept
	 * @param start all results less than this number will be removed
	 * @param size if non zero, all results after <code>start</code> + <code>size</code> will be
	 *            removed
	 * @return a list of conceptSearchResults
	 * @throws APIException
	 * @should return concept search results that match unique concepts
	 * @should return a search result whose concept name contains a word with more weight
	 * @since 1.8
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptSearchResult> getConcepts(String phrase, List<Locale> locales, boolean includeRetired,
	        List<ConceptClass> requireClasses, List<ConceptClass> excludeClasses, List<ConceptDatatype> requireDatatypes,
	        List<ConceptDatatype> excludeDatatypes, Concept answersToConcept, Integer start, Integer size)
	        throws APIException;
	
	/**
	 * Finds concepts that are possible value coded answers to concept parameter
	 * 
	 * @param phrase
	 * @param locale
	 * @param concept the answers to match on
	 * @return a list of conceptSearchResults
	 * @throws APIException
	 * @since 1.8
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptSearchResult> findConceptAnswers(String phrase, Locale locale, Concept concept) throws APIException;
	
	/**
	 * Iterates over the words in names and synonyms (for each locale) and updates the concept
	 * index, note that this only updates the index of the specified concept. Use
	 * {@link ConceptService#updateConceptIndexes()} if you wish to update the entire concept index.
	 * 
	 * @param concept the concept whose index is to be updated
	 * @throws APIException
	 * @since 1.8
	 */
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateConceptIndex(Concept concept) throws APIException;
	
	/**
	 * Iterates over all concepts and calls upddateConceptIndexes(Concept concept)
	 * 
	 * @throws APIException
	 * @since 1.8
	 */
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateConceptIndexes() throws APIException;
	
	/**
	 * Iterates over all concepts with conceptIds between <code>conceptIdStart</code> and
	 * <code>conceptIdEnd</code> (inclusive) and calls updateConceptIndexes(concept)
	 * 
	 * @param conceptIdStart starts update with this concept_id
	 * @param conceptIdEnd ends update with this concept_id
	 * @throws APIException
	 * @since 1.8
	 */
	@Authorized( { PrivilegeConstants.MANAGE_CONCEPTS })
	public void updateConceptIndexes(Integer conceptIdStart, Integer conceptIdEnd) throws APIException;
	
	/**
	 * Searches for concepts with the given parameters
	 * 
	 * @param phrase the string to search against (if blank/null, matches all concepts)
	 * @param locale the locale in which to search for the concepts
	 * @param includeRetired Specifies whether to include retired concepts
	 * @return a list ConceptSearchResults
	 * @throws APIException
	 * @since 1.8
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptSearchResult> getConcepts(String phrase, Locale locale, boolean includeRetired) throws APIException;
	
	/**
	 * Return the number of concepts matching a search phrase and the specified arguments
	 * 
	 * @param phrase matched to the start of any word in any of the names of a concept
	 * @param locales List<Locale> to restrict to
	 * @param includeRetired Specifies whether to include retired concepts
	 * @param requireClasses List<ConceptClass> to restrict to
	 * @param excludeClasses List<ConceptClass> to leave out of results
	 * @param requireDatatypes List<ConceptDatatype> to restrict to
	 * @param excludeDatatypes List<ConceptDatatype> to leave out of results
	 * @param answersToConcept all results will be a possible answer to this concept
	 * @return the number of concepts matching the given search phrase
	 * @throws APIException
	 * @since 1.8
	 * @should return a count of unique concepts
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Integer getCountOfConcepts(String phrase, List<Locale> locales, boolean includeRetired,
	        List<ConceptClass> requireClasses, List<ConceptClass> excludeClasses, List<ConceptDatatype> requireDatatypes,
	        List<ConceptDatatype> excludeDatatypes, Concept answersToConcept);
	
	/**
	 * Return the number of drugs with matching names or concept drug names
	 * 
	 * @param drugName the name of the drug
	 * @param concept the drug concept
	 * @param searchOnPhrase Specifies if the search should match names starting with or contain the
	 *            text
	 * @param searchDrugConceptNames Specifies whether a search on concept names for the drug's
	 *            concept should be done or not
	 * @param includeRetired specifies whether to include retired drugs
	 * @return the number of matching drugs
	 * @throws APIException
	 * @since 1.8
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public Integer getCountOfDrugs(String drugName, Concept concept, boolean searchOnPhrase, boolean searchDrugConceptNames,
	        boolean includeRetired) throws APIException;
	
	/**
	 * Returns a list of drugs with matching names or concept drug names and returns a specific
	 * number of them from the specified starting position. If start and length are not specified,
	 * then all matches are returned
	 * 
	 * @param drugName the name of the drug
	 * @param concept the drug concept
	 * @param searchOnPhrase Specifies if the search should match names starting with or contain the
	 *            text
	 * @param searchDrugConceptNames Specifies whether a search on concept names for the drug's
	 *            concept should be done or not
	 * @param includeRetired specifies whether to include retired drugs
	 * @param start beginning index for the batch
	 * @param length number of drugs to return in the batch
	 * @return a list of matching drugs
	 * @throws APIException
	 * @since 1.8
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Drug> getDrugs(String drugName, Concept concept, boolean searchOnPhrase, boolean searchDrugConceptNames,
	        boolean includeRetired, Integer start, Integer length) throws APIException;
	
	/**
	 * Gets the list of <code>ConceptStopWord</code> for given locale
	 * 
	 * @param locale The locale in which to search for the <code>ConceptStopWord</code>
	 * @return list of concept stop words for given locale
	 * @should return list of concept stop words for given locale
	 * @should return empty list if no stop words are found for the given locale
	 * @should return default Locale <code>ConceptStopWord</code> if Locale is null
	 * @since 1.8
	 */
	@Transactional(readOnly = true)
	public List<String> getConceptStopWords(Locale locale);
	
	/**
	 * Save the given <code>ConceptStopWord</code> in the database
	 * <p>
	 * If this is a new concept stop word, the returned concept stop word will have a new
	 * {@link org.openmrs.ConceptStopWord#getConceptStopWordId()} inserted into it that was
	 * generated by the database
	 * </p>
	 * 
	 * @param conceptStopWord The <code>ConceptStopWord</code> to save or update
	 * @return the <code>ConceptStopWord</code> that was saved or updated
	 * @throws APIException
	 * @should generated concept stop word id onto returned concept stop word
	 * @should save concept stop word into database
	 * @should assign default Locale
	 * @should save concept stop word in uppercase
	 * @should fail if a duplicate conceptStopWord in a locale is added
	 * @since 1.8
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_STOP_WORDS)
	public ConceptStopWord saveConceptStopWord(ConceptStopWord conceptStopWord) throws APIException;
	
	/**
	 * Delete the given <code>ConceptStopWord</code> in the database
	 * 
	 * @param conceptStopWordId The <code>ConceptStopWord</code> to delete
	 * @throws APIException
	 * @should delete the given concept stop word from the database
	 * @since 1.8
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_STOP_WORDS)
	public void deleteConceptStopWord(Integer conceptStopWordId) throws APIException;
	
	/**
	 * Get all the concept stop words
	 * 
	 * @return List of <code>ConceptStopWord</code>
	 * @should return all the concept stop words
	 * @should return empty list if nothing found
	 * @since 1.8
	 */
	@Transactional(readOnly = true)
	public List<ConceptStopWord> getAllConceptStopWords();
	
	/**
	 * Returns a list of concept map types currently in the database excluding hidden ones
	 * 
	 * @return List of concept map type objects
	 * @since 1.9
	 * @throws APIException
	 * @should return all the concept map types excluding hidden ones
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_MAP_TYPES)
	public List<ConceptMapType> getActiveConceptMapTypes() throws APIException;
	
	/**
	 * Returns a list of concept map types currently in the database including or excluding retired
	 * and hidden ones as specified by the includeRetired and includeHidden arguments
	 * 
	 * @param includeRetired specifies if retired concept map types should be included
	 * @return List of concept map type objects
	 * @since 1.9
	 * @throws APIException
	 * @should return all the concept map types if includeRetired and hidden are set to true
	 * @should return only un retired concept map types if includeRetired is set to false
	 * @should not include hidden concept map types if includeHidden is set to false
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_MAP_TYPES)
	public List<ConceptMapType> getConceptMapTypes(boolean includeRetired, boolean includeHidden) throws APIException;
	
	/**
	 * Return a concept map type matching the given concept map type id
	 * 
	 * @param conceptMapTypeId Integer concept map type id
	 * @return ConceptMapType
	 * @since 1.9
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_MAP_TYPES)
	public ConceptMapType getConceptMapType(Integer conceptMapTypeId) throws APIException;
	
	/**
	 * Return a concept map type matching the given uuid
	 * 
	 * @param uuid the uuid to search against
	 * @return ConceptMapType
	 * @since 1.9
	 * @throws APIException
	 * @should return a conceptMapType matching the specified uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_MAP_TYPES)
	public ConceptMapType getConceptMapTypeByUuid(String uuid) throws APIException;
	
	/**
	 * Return a concept map type matching the given name
	 * 
	 * @param name the name to search against
	 * @return ConceptMapType
	 * @since 1.9
	 * @throws APIException
	 * @should return a conceptMapType matching the specified name
	 * @should be case insensitive
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_MAP_TYPES)
	public ConceptMapType getConceptMapTypeByName(String name) throws APIException;
	
	/**
	 * Saves or updates the specified concept map type in the database
	 * 
	 * @param conceptMapType the concept map type to save
	 * @return the saved conceptMapType
	 * @since 1.9
	 * @throws APIException
	 * @should add the specified concept map type to the database and assign to it an id
	 * @should update an existing concept map type
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_MAP_TYPES)
	public ConceptMapType saveConceptMapType(ConceptMapType conceptMapType) throws APIException;
	
	/**
	 * Retiring a concept map type essentially removes it from circulation
	 * 
	 * @param conceptMapType the concept map type to retire
	 * @param retireReason the reason why the concept map type is being retired
	 * @return the retired concept map type
	 * @since 1.9
	 * @throws APIException
	 * @should retire the specified conceptMapType with the given retire reason
	 * @should should set the default retire reason if none is given
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_MAP_TYPES)
	public ConceptMapType retireConceptMapType(ConceptMapType conceptMapType, String retireReason) throws APIException;
	
	/**
	 * Marks a concept map type that is currently retired as not retired.
	 * 
	 * @param conceptMapType the concept map type to unretire
	 * @return the unretired concept map type
	 * @since 1.9
	 * @throws APIException
	 * @should unretire the specified concept map type and drop all retire related fields
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_MAP_TYPES)
	public ConceptMapType unretireConceptMapType(ConceptMapType conceptMapType) throws APIException;
	
	/**
	 * Completely purges a concept map type from the database
	 * 
	 * @param conceptMapType the concept map type to purge from the database
	 * @since 1.9
	 * @throws APIException
	 * @should delete the specified conceptMapType from the database
	 */
	@Authorized(PrivilegeConstants.PURGE_CONCEPT_MAP_TYPES)
	public void purgeConceptMapType(ConceptMapType conceptMapType) throws APIException;
	
	/**
	 * Returns a list of mappings from concepts to terms in the given reference terminology
	 * 
	 * @param conceptSource
	 * @return a List<ConceptMap> object
	 * @since 1.9
	 * @throws APIException
	 * @should return a List of ConceptMaps from the given source
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<ConceptMap> getConceptMappingsToSource(ConceptSource conceptSource) throws APIException;
	
	/**
	 * Gets a list of all concept reference terms saved in the database
	 * 
	 * @return a list of concept reference terms
	 * @since 1.9
	 * @throws APIException
	 * @should return all concept reference terms in the database
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_REFERENCE_TERMS)
	public List<ConceptReferenceTerm> getAllConceptReferenceTerms() throws APIException;
	
	/**
	 * Gets a list of concept reference terms saved in the database
	 * 
	 * @param includeRetired specifies if retired concept reference terms should be included
	 * @return a list of concept reference terms
	 * @since 1.9
	 * @throws APIException
	 * @should return all the concept reference terms if includeRetired is set to true
	 * @should return only un retired concept reference terms if includeRetired is set to false
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_REFERENCE_TERMS)
	public List<ConceptReferenceTerm> getConceptReferenceTerms(boolean includeRetired) throws APIException;
	
	/**
	 * Gets the concept reference term with the specified concept reference term id
	 * 
	 * @param conceptReferenceTermId the concept reference term id to search against
	 * @return the concept reference term object with the given concept reference term id
	 * @since 1.9
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_REFERENCE_TERMS)
	public ConceptReferenceTerm getConceptReferenceTerm(Integer conceptReferenceTermId) throws APIException;
	
	/**
	 * Gets the concept reference term with the specified uuid
	 * 
	 * @param uuid the uuid to search against
	 * @return the concept reference term object with the given uuid
	 * @since 1.9
	 * @throws APIException
	 * @should return the concept reference term that matches the given uuid
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_REFERENCE_TERMS)
	public ConceptReferenceTerm getConceptReferenceTermByUuid(String uuid) throws APIException;
	
	/**
	 * Gets a concept reference term with the specified name from the specified concept source
	 * ignoring all retired ones
	 * 
	 * @param name the name to match against
	 * @param conceptSource the concept source to match against
	 * @return concept reference term object
	 * @since 1.9
	 * @throws APIException
	 * @should return a concept reference term that matches the given name from the given source
	 * @should be case insensitive
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_REFERENCE_TERMS)
	public ConceptReferenceTerm getConceptReferenceTermByName(String name, ConceptSource conceptSource) throws APIException;
	
	/**
	 * Gets a concept reference term with the specified code from the specified concept source
	 * 
	 * @param code the code to match against
	 * @param conceptSource the concept source to match against
	 * @return concept reference term object
	 * @since 1.9
	 * @throws APIException
	 * @should return a concept reference term that matches the given code from the given source
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_REFERENCE_TERMS)
	public ConceptReferenceTerm getConceptReferenceTermByCode(String code, ConceptSource conceptSource) throws APIException;
	
	/**
	 * Stores the specified concept reference term to the database
	 * 
	 * @param conceptReferenceTerm the concept reference term object to save
	 * @return the saved concept reference term object
	 * @since 1.9
	 * @throws APIException
	 * @should add a concept reference term to the database and assign an id to it
	 * @should update changes to the concept reference term in the database
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_REFERENCE_TERMS)
	public ConceptReferenceTerm saveConceptReferenceTerm(ConceptReferenceTerm conceptReferenceTerm) throws APIException;
	
	/**
	 * Retiring a concept reference term essentially removes it from circulation
	 * 
	 * @param conceptReferenceTerm the concept reference term object to retire
	 * @param retireReason the reason why the concept reference term is being retired
	 * @return the retired concept reference term object
	 * @since 1.9
	 * @throws APIException
	 * @should retire the specified concept reference term with the given retire reason
	 * @should should set the default retire reason if none is given
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_REFERENCE_TERMS)
	public ConceptReferenceTerm retireConceptReferenceTerm(ConceptReferenceTerm conceptReferenceTerm, String retireReason)
	        throws APIException;
	
	/**
	 * Marks a concept reference term that is currently retired as not retired.
	 * 
	 * @param conceptReferenceTerm the concept reference term to unretire
	 * @return the unretired concept reference term
	 * @since 1.9
	 * @throws APIException
	 * @should unretire the specified concept reference term and drop all retire related fields
	 */
	@Authorized(PrivilegeConstants.MANAGE_CONCEPT_REFERENCE_TERMS)
	public ConceptReferenceTerm unretireConceptReferenceTerm(ConceptReferenceTerm conceptReferenceTerm) throws APIException;
	
	/**
	 * Purges the specified concept reference term from the database
	 * 
	 * @param conceptReferenceTerm the concept reference term object to purge
	 * @since 1.9
	 * @throws APIException
	 */
	@Authorized(PrivilegeConstants.PURGE_CONCEPT_REFERENCE_TERMS)
	public void purgeConceptReferenceTerm(ConceptReferenceTerm conceptReferenceTerm) throws APIException;
	
	/**
	 * Finds the concept reference term in the database that have a code or name that contains the
	 * specified search phrase.
	 * 
	 * @param query the string to match against the reference term names or codes
	 * @param conceptSource the concept source from which the terms should be looked up
	 * @param start beginning index for the batch
	 * @param length number of terms to return in the batch
	 * @param includeRetired specifies if the retired terms should be included
	 * @return a list if {@link ConceptReferenceTerm}s
	 * @since 1.9
	 * @throws APIException
	 * @should return unique terms with a code or name containing the search phrase
	 * @should return only the concept reference terms from the given concept source
	 */
	@Transactional(readOnly = true)
	@Authorized( { PrivilegeConstants.VIEW_CONCEPT_REFERENCE_TERMS })
	public List<ConceptReferenceTerm> getConceptReferenceTerms(String query, ConceptSource conceptSource, Integer start,
	        Integer length, boolean includeRetired) throws APIException;
	
	/**
	 * Returns the count of concept reference terms that match the specified arguments
	 * 
	 * @param query the string to match against the reference term names
	 * @param conceptSource the concept source from which the terms should be looked up
	 * @param includeRetired specifies if retired concept reference terms should be included
	 * @return the count of matching concept reference terms
	 * @since 1.9
	 * @throws APIException
	 * @should include retired terms if includeRetired is set to true
	 * @should not include retired terms if includeRetired is set to false
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_REFERENCE_TERMS)
	public Integer getCountOfConceptReferenceTerms(String query, ConceptSource conceptSource, boolean includeRetired)
	        throws APIException;
	
	/**
	 * Fetches all the {@link ConceptReferenceTermMap} where the specified reference term is the
	 * termB i.e mappings added to other terms pointing to it
	 * 
	 * @param term the term to match against
	 * @return a list of {@link ConceptReferenceTermMap}s
	 * @since 1.9
	 * @throws APIException
	 * @should return all concept reference term maps where the specified term is the termB
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_REFERENCE_TERMS)
	public List<ConceptReferenceTermMap> getReferenceTermMappingsTo(ConceptReferenceTerm term) throws APIException;
	
	/**
	 * Returns a list of concepts with the same name in the given locale.
	 * <p>
	 * This method is case insensitive. It searches for exactly matching names and close matching
	 * locales. It considers only non-voided names and all concepts.
	 * 
	 * @param name
	 * @param locale <code>null</code> = all locales
	 * @return the list of concepts
	 * @throws APIException
	 * @since 1.10
	 * @should return concepts for all countries and global language given language only locale
	 * @should return concepts for specific country and global language given language and country
	 *         locale
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPTS)
	public List<Concept> getConceptsByName(String name, Locale locale) throws APIException;
	
	/**
	 * Gets the concept map type to be used as the default. It is specified by the
	 * <code>concept.defaultConceptMapType</code> global property.
	 * 
	 * @since 1.9
	 * @return the {@link ConceptMapType}
	 * @throws APIException
	 * @should return same as by default
	 * @should return type as set in gp
	 */
	@Transactional(readOnly = true)
	@Authorized(PrivilegeConstants.VIEW_CONCEPT_MAP_TYPES)
	public ConceptMapType getDefaultConceptMapType() throws APIException;
}
