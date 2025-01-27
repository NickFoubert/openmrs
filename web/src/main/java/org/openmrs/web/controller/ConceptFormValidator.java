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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptName;
import org.openmrs.api.context.Context;
import org.openmrs.web.controller.ConceptFormController.ConceptFormBackingObject;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * The web validator for the concept editing form
 */
public class ConceptFormValidator implements Validator {
	
	/** Log for this class and subclasses */
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Determines if the command object being submitted is a valid type
	 * 
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	public boolean supports(Class c) {
		return c.equals(ConceptFormBackingObject.class);
	}
	
	/**
	 * Checks the form object for any inconsistencies/errors
	 * 
	 * @see org.springframework.validation.Validator#validate(java.lang.Object,
	 *      org.springframework.validation.Errors)
	 */
	public void validate(Object obj, Errors errors) {
		ConceptFormBackingObject backingObject = (ConceptFormBackingObject) obj;
		Set<String> localesWithErrors = new HashSet<String>();
		if (backingObject.getConcept() == null) {
			errors.rejectValue("concept", "error.general");
		} else {
			// validate the concept term mappings
			for (int x = 0; x < backingObject.getConceptMappings().size(); x++) {
				ConceptMap map = backingObject.getConceptMappings().get(x);
				//this mapping has been removed or ignore it
				if (map.getConceptReferenceTerm() == null)
					continue;
				//The user should select from existing reference terms
				if (map.getConceptReferenceTerm().getConceptReferenceTermId() == null)
					errors.rejectValue("conceptMappings[" + x + "]", "ConceptReferenceTerm.term.notInDatabase");
				
				if (map.getConceptMapType() == null)
					errors.rejectValue("conceptMappings[" + x + "].conceptMapType", "Concept.map.typeRequired");
			}
			
			boolean foundAtLeastOneFullySpecifiedName = false;
			
			for (Locale locale : backingObject.getLocales()) {
				// validate that a void reason was given for voided synonyms
				for (int x = 0; x < backingObject.getSynonymsByLocale().get(locale).size(); x++) {
					ConceptName synonym = backingObject.getSynonymsByLocale().get(locale).get(x);
					// validate that synonym names are non-empty (null name means it was invalid and then removed)
					if (synonym.getName() != null && synonym.getName().length() == 0) {
						errors.rejectValue("synonymsByLocale[" + locale + "][" + x + "].name",
						    "Concept.synonyms.textRequired");
						localesWithErrors.add(locale.getDisplayName());
					}
				}
				
				for (int x = 0; x < backingObject.getIndexTermsByLocale().get(locale).size(); x++) {
					ConceptName indexTerm = backingObject.getIndexTermsByLocale().get(locale).get(x);
					// validate that indexTerm names are non-empty (null name means it was invalid and then removed)
					if (indexTerm.getName() != null && indexTerm.getName().length() == 0) {
						errors.rejectValue("indexTermsByLocale[" + locale + "][" + x + "].name",
						    "Concept.indexTerms.textRequired");
						localesWithErrors.add(locale.getDisplayName());
					}
				}
				
				// validate that at least one name in a locale is non-empty
				if (StringUtils.hasText(backingObject.getNamesByLocale().get(locale).getName())) {
					foundAtLeastOneFullySpecifiedName = true;
					
				}// if this is a new name and user has changed it into an empty string, reject it
				else if (backingObject.getNamesByLocale().get(locale).getConceptNameId() != null) {
					errors.rejectValue("namesByLocale[" + locale + "].name", "Concept.fullySpecified.textRequired");
					localesWithErrors.add(locale.getDisplayName());
				}
			}
			
			if (!foundAtLeastOneFullySpecifiedName)
				errors.reject("Concept.name.atLeastOneRequired");
			
		}
		
		if (errors.hasErrors() && localesWithErrors.size() > 0) {
			StringBuilder sb = new StringBuilder(Context.getMessageSourceService().getMessage("Concept.localesWithErrors"));
			boolean isFirst = true;
			for (String localeName : localesWithErrors) {
				if (isFirst) {
					sb.append(localeName);
					isFirst = false;
				} else
					sb.append(", ").append(localeName);
			}
			errors.rejectValue("concept", sb.toString());
		}
	}
	
}
