<%@ include file="/WEB-INF/template/include.jsp"%>

<openmrs:require privilege="Manage Concepts" otherwise="/login.htm" redirect="/dictionary/concept.form" />

<c:choose>
	<c:when test="${command.concept.conceptId != null}">
		<spring:message var="pageTitle" code="Concept.edit.titlebar" scope="page" arguments="${command.concept.name}"/>
	</c:when>
	<c:otherwise>
		<spring:message var="pageTitle" code="Concept.creatingNewConcept.titlebar" scope="page"/>
	</c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/template/header.jsp"%>

<openmrs:htmlInclude file="/scripts/dojo/dojo.js" />
<openmrs:htmlInclude file="/dwr/util.js" />
<openmrs:htmlInclude file="/dwr/interface/DWRConceptService.js" />

<openmrs:htmlInclude file="/dictionary/conceptForm.js" />
<openmrs:htmlInclude file="/scripts/jquery/autocomplete/OpenmrsAutoComplete.js" />


<script type="text/javascript">
	function addName(anchor) {
		if (anchor.href.lastIndexOf("=") == anchor.href.length - 1)
			anchor.href += $("conceptName_${locale}").value;
	}
	
	// concept name tab functionality
	function selectTab(tab) {
		var displays = new Array();
		
		var tabs = tab.parentNode.getElementsByTagName("a");
		for (var tabIndex=0; tabIndex<tabs.length; tabIndex++) {
			var index = tabs[tabIndex].id.indexOf("Tab");
			var tabName = tabs[tabIndex].id.substr(0, index);
			if (tabs[tabIndex] == tab) {
				displays[tabName] = "";
				addClass(tabs[tabIndex], 'selectedTab');
			}
			else {
				displays[tabName] = "none";
				removeClass(tabs[tabIndex], 'selectedTab');
			}
		}
		
		var parent = tab.parentNode.parentNode.parentNode;
		var elements = parent.getElementsByTagName("td");	
		for (var i=0; i<elements.length; i++) {
			if (displays[elements[i].className] != null)
					elements[i].style.display = displays[elements[i].className];
		}
		
		tab.blur();
		return false;
	}
	
	function jumpToConcept(which) {
		var action = document.getElementById('jumpAction');
		action.value = which;
		var jumpForm = document.getElementById('jumpForm');
		jumpForm.submit();
		return false;
	}

</script>

<style>
	.inlineForm { padding: 0px; margin: 0px; display: inline; }
	#conceptTable th { text-align: right; padding-right: 15px; }
	#conceptNameTable th { text-align: left; }
	.localeSpecific td, a.selectedTab { background-color: whitesmoke; }
	a.tab { border-bottom: 1px solid whitesmoke; padding-left: 3px; padding-right: 3px; }
	.hidden, #newConceptSynonym, #newConceptMapping { display: none; }
	.checkbox_void{ margin-left: 80px; }
	.help_icon_bottom{ vertical-align: bottom; }
	.help_icon_top{ vertical-align: top; }
	#preferredLabel{ padding-left: 345px; padding-top:10px; }
	#addAnswerError{ margin-bottom: 0.5em; border: 1px dashed black; background: #FAA; line-height: 2em; text-align: center; display: none; }
</style>

<c:choose>
	<c:when test="${command.concept.conceptId != null}">
		<h2><spring:message code="Concept.edit.title" arguments="${command.concept.name}" /></h2>
	</c:when>
	<c:otherwise>
		<h2><spring:message code="Concept.creatingNewConcept" /></h2>
	</c:otherwise>
</c:choose>

<c:if test="${command.concept.conceptId != null}">
	<form class="inlineForm" id="jumpForm" action="" method="post">
		<input type="hidden" name="jumpAction" id="jumpAction" value="previous"/>
		<a href="#previousConcept" id="previousConcept" valign="middle" accesskey="," onclick="return jumpToConcept('previous')"><spring:message code="general.previous"/></a>
			|
		<a href="concept.htm?conceptId=${command.concept.conceptId}" id="viewConcept" accesskey="v"><spring:message code="general.view"/></a>
			|
		<a href="conceptStats.form?conceptId=${command.concept.conceptId}" id="conceptStats" accesskey="s" valign="middle"><spring:message code="Concept.stats"/></a>
			|
		<a href="#nextConcept" id="nextConcept" valign="middle" accesskey="." onclick="return jumpToConcept('next')"><spring:message code="general.next"/></a>
	</form>
</c:if>

<openmrs:globalProperty key="concepts.locked" var="conceptsLocked"/>

<c:if test="${conceptsLocked != 'true'}">
	| <a href="concept.form" id="newConcept" valign="middle"><spring:message code="general.new"/></a>
</c:if>

<form class="inlineForm" action="index.htm" method="get">
  &nbsp; &nbsp; 
  <input type="text" id="searchPhrase" name="phrase" size="18"> 
  <input type="submit" class="smallButton" value="<spring:message code="general.search"/>"/>
</form>

<br/><br/>
<c:if test="${command.concept.retired}">
	<div class="retiredMessage">
	<div><spring:message code="Concept.retiredMessage"/></div>
    <div>  <c:if test="${command.concept.retiredBy.personName != null}">  <spring:message code="general.byPerson"/> ${command.concept.retiredBy.personName} </c:if> <c:if test="${command.concept.dateRetired != null}"> <spring:message code="general.onDate"/>  <openmrs:formatDate date="${command.concept.dateRetired}" type="long" /> </c:if> <c:if test="${command.concept.retireReason!=''}"> - ${command.concept.retireReason} </c:if> </div>
	<openmrs:hasPrivilege privilege="Manage Concepts">
		<div>
			<form action="" method="post" ><input type="submit" name="action" value="<spring:message code="general.unretire"/>" />
			</form>
		</div> 
	</openmrs:hasPrivilege>
	</div>
</c:if>

<spring:hasBindErrors name="command">
	<spring:message code="fix.error"/>
	<div class="error">
		<c:forEach items="${errors.allErrors}" var="error">
			<spring:message code="${error.code}" text="${error.code}"/><br/><!-- ${error} -->
		</c:forEach>
	</div>
	<br />
</spring:hasBindErrors>

<c:if test="${command.concept.conceptId != null}">
	<c:if test="${command.concept.conceptClass.name == 'Question' && command.concept.datatype.name == 'N/A'}">
		<div class="highlighted">
			<spring:message code="Concept.checkClassAndDatatype"/>
		</div>
		<br/>
	</c:if>
</c:if>

<openmrs:extensionPoint pointId="org.openmrs.dictionary.conceptFormHeader" type="html" />

<form method="post" action="">

<table id="conceptTable" cellpadding="2" cellspacing="0">

	<tr>
		<th title="<spring:message code="Concept.id.help"/>"><spring:message code="general.id"/></th>
		<td colspan="${fn:length(locales)}">${command.concept.conceptId}</td>
	</tr>
	<c:if test="${command.concept.conceptId != null}">
	<tr>
		<th title="<spring:message code="Concept.uiid.help"/>"><spring:message code="general.uuid"/></th>
		<td colspan="${fn:length(locales)}">${command.concept.uuid}</td>
	</tr>
	</c:if>
	<tr>
		<th title="<spring:message code="Concept.locale.help"/>"><spring:message code="general.locale"/></th>
		<td style="padding-bottom: 0px; padding-left: 0px;">
			<c:forEach items="${command.locales}" var="loc" varStatus="varStatus">
				<a id="${loc}Tab" class="tab ${loc}" href="#select${loc.displayName}" onclick="return selectTab(this)">${loc.displayName}</a><c:if test="${varStatus.last==false}"> | </c:if>
			</c:forEach>
		</td>
	</tr>
	<tr class="localeSpecific">
		<th valign="bottom">
			<spring:message code="Concept.fullySpecifiedName" /> 
			<img class="help_icon" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.fullySpecified.help"/>"/>
		</th>
		<c:forEach items="${command.locales}" var="loc">
			<td class="${loc}" style = "padding: 0px 0px 0px 0px;" >	
				<table id = "containerTable[${loc}]">
					<tr>
						<td valign="bottom" >
								<spring:bind path="command.namesByLocale[${loc}].name">
								<input type="text" name="${status.expression}" value="${status.value}" id="${status.expression}" class="largeWidth" onchange="setRadioValue(this, 'fullySpecPreferred[${loc}]')" />
								</spring:bind>
						</td>
						<!-- belown code displays (radio button, help icon and label) as a completed preffered group -->
						<td id="preferredContainer[${loc}]">
							<span 
								<c:if test="${fn:length(command.synonymsByLocale[loc]) > 0}">style = "visibility: visible"</c:if> 
								<c:if test="${fn:length(command.synonymsByLocale[loc]) == 0}">style = "visibility: hidden"</c:if>>
								<spring:message code="Concept.name.localePreferred" /> 
							</span>
							<img class="help_icon" src="${pageContext.request.contextPath}/images/help.gif" border="0" 
								<c:if test="${fn:length(command.synonymsByLocale[loc]) > 0}">style = "visibility: visible"</c:if>
								<c:if test="${fn:length(command.synonymsByLocale[loc]) == 0}">style = "visibility: hidden"</c:if> title="<spring:message code="Concept.name.localePreferred.help"/>" />
							<br />
							<spring:bind path="command.preferredNamesByLocale[${loc}]" ignoreNestedPath="true">			
							<input id="fullySpecPreferred[${loc}]" type="radio" name="${status.expression}" value="${command.namesByLocale[loc].name}"  
								<c:if test="${command.namesByLocale[loc].localePreferred}">checked=checked</c:if>
								<c:if test="${fn:length(command.synonymsByLocale[loc]) > 0}">style = "visibility: visible"</c:if>
								<c:if test="${fn:length(command.synonymsByLocale[loc]) == 0}">style = "visibility: hidden"</c:if>/>
							</spring:bind>
							<spring:bind path="command.namesByLocale[${loc}].name">
							<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
							</spring:bind>
						</td>
					</tr>
				</table>
			</td>
		</c:forEach>
	</tr>
	<tr class="localeSpecific">
		<th valign="top">
			<spring:message code="Concept.synonyms" /> <img class="help_icon_bottom" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.synonyms.help"/>"/>
		</th>
		<c:forEach items="${command.locales}" var="loc">
			<td class="${loc}">
				<c:forEach var="synonym" items="${command.synonymsByLocale[loc]}" varStatus="varStatus">
					<spring:nestedPath path="command.synonymsByLocale[${loc}][${varStatus.index}]">
						<input type="hidden" name="_synonymsByLocale[${loc}][${varStatus.index}].name" value="" />
						<div>							
							<spring:bind path="name">																
							<input type="text" name="${status.expression}" value="${status.value}" class="largeWidth" 
							<c:if test="${conceptNameHasObsMap[synonym.conceptNameId] ==  null}">onchange="setRadioValue(this, 'possiblePrefName_[${loc}][${varStatus.index}]')" </c:if> 
							<c:if test="${conceptNameHasObsMap[synonym.conceptNameId] !=  null}">readonly="readonly"</c:if> />							
							</spring:bind>
							<spring:bind path="command.preferredNamesByLocale[${loc}]" ignoreNestedPath="true">							
							<input id="possiblePrefName_[${loc}][${varStatus.index}]" type="radio" name="${status.expression}" value="${synonym.name}" <c:if test="${synonym.localePreferred}">checked=checked</c:if> />							
							</spring:bind>
							<!-- If this was a new synonym that failed validation, it can be removed without a void reason  -->
							<c:if test="${command.synonymsByLocale[loc][varStatus.index].conceptNameId == null}">
							<input type="button" value='<spring:message code="general.remove"/>' class="smallButton" onClick="removeSynonymElement(this, 'newConceptSynonym-${loc}', 'preferredContainer[${loc}]', '${fn:length(command.synonymsByLocale[loc])}', null)"/>							
							</c:if>	
							<c:if test="${command.synonymsByLocale[loc][varStatus.index].conceptNameId != null}">
							<input type="button" value='<spring:message code="general.remove"/>' class="smallButton" onClick="removeSynonymElement(this, 'newConceptSynonym-${loc}', 'preferredContainer[${loc}]', '${fn:length(command.synonymsByLocale[loc])}', 'synonymsByLocale[${loc}][${varStatus.index}].isVoided')"/>
							</c:if>
							<spring:bind path="name">
							<c:if test="${status.errorMessage != ''}">
							<span class="error">${status.errorMessage}</span>
							</c:if>
							</spring:bind>														
							<spring:bind path="voided">
								<input type="hidden" name="_${status.expression}" value="" />
								<input id="synonymsByLocale[${loc}][${varStatus.index}].isVoided" type="hidden" name="${status.expression}" value="${status.value}" />
							</spring:bind>							
						</div>
					</spring:nestedPath>
				</c:forEach>
				<div id="newConceptSynonym-${loc}" style="display: none">
					<input type="text" name="[x].name" value="" class="largeWidth" onchange="setCloneRadioValue(this)">
					<input type="radio" name="preferredNamesByLocale[${loc}]" value="" />
					<input type="button" value='<spring:message code="general.remove"/>' class="smallButton" onClick="removeSynonymElement(this, 'newConceptSynonym-${loc}', 'preferredContainer[${loc}]', null, null)" />
				</div>
				<input type="button" value='<spring:message code="Concept.synonym.add"/>' class="smallButton" 
				       onClick="cloneSynonymElement('newConceptSynonym-${loc}', ${fn:length(command.synonymsByLocale[loc])}, 'synonymsByLocale[${loc}]', 'preferredContainer[${loc}]')" />				
			</td>			
		</c:forEach>
	</tr>
	<tr class="localeSpecific">
		<th valign="top">
			<spring:message code="Concept.indexTerms" /> <img class="help_icon_bottom" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.indexTerms.help"/>"/>
		</th>
		<c:forEach items="${command.locales}" var="loc">
			<td class="${loc}">
				<c:forEach var="indexTerm" items="${command.indexTermsByLocale[loc]}" varStatus="varStatus">
					<spring:nestedPath path="command.indexTermsByLocale[${loc}][${varStatus.index}]">
						<input type="hidden" name="_indexTermsByLocale[${loc}][${varStatus.index}].name" value="" />
						<div>							
							<spring:bind path="name">
							<input type="text" name="${status.expression}" value="${status.value}" class="largeWidth" 
							<c:if test="${conceptNameHasObsMap[indexTerm.conceptNameId] !=  null}">readonly="readonly"</c:if> />								
							</spring:bind>
							<!-- A place holder radio button just to maintain alignment of the voided checkbox on the page  -->
							<input type="radio" name="placeHolder" value="" style="visibility: hidden" />
							<!-- If this was a new index term that failed validation, it can be removed without a void reason  -->
							<c:if test="${command.indexTermsByLocale[loc][varStatus.index].conceptNameId == null}">
							<input type="button" value='<spring:message code="general.remove"/>' class="smallButton" onClick="removeParentElement(this)"/>							
							</c:if>
							<c:if test="${command.indexTermsByLocale[loc][varStatus.index].conceptNameId != null}">
							<input type="button" value='<spring:message code="general.remove"/>' class="smallButton" onClick="voidName(this, 'indexTermsByLocale[${loc}][${varStatus.index}].isVoided')"/>							
							</c:if>
							<spring:bind path="name">
							<c:if test="${status.errorMessage != ''}">
								<span class="error">${status.errorMessage}</span>
							</c:if>
							</spring:bind>
							<spring:bind path="voided">
								<input type="hidden" name="_${status.expression}" value="" />
								<input id="indexTermsByLocale[${loc}][${varStatus.index}].isVoided" type="hidden" name="${status.expression}" value="${status.value}" />
							</spring:bind>							
						</div>
					</spring:nestedPath>
				</c:forEach>
				<div id="newConceptIndexTerm-${loc}" style="display: none">
					<input type="text" name="[x].name" value="" class="largeWidth" />
					<input type="radio" name="placeHolder" value="" style="visibility: hidden" />
					<input type="button" value='<spring:message code="general.remove"/>' class="smallButton" onClick="removeParentElement(this)" />
				</div>
				<input type="button" id="addSearch" value='<spring:message code="Concept.indexTerm.add"/>' class="smallButton" onClick="cloneElement('newConceptIndexTerm-${loc}', ${fn:length(command.indexTermsByLocale[loc])}, 'indexTermsByLocale[${loc}]')" />
			</td>
			
		</c:forEach>
	</tr>
	<tr class="localeSpecific">
		<th>
			<spring:message code="Concept.shortName" /> <img class="help_icon_bottom" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.shortName.help"/>"/>
		</th>
		<c:forEach items="${command.locales}" var="loc">
			<td class="${loc}">
				<spring:bind path="command.shortNamesByLocale[${loc}].name">
					<input class="smallWidth" type="text" name="${status.expression}" value="${status.value}" />					
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>
			</td>
		</c:forEach>
	</tr>
	<tr class="localeSpecific">
		<th>
			<spring:message code="general.description" /> <img class="help_icon_top" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.description.help"/>"/>
		</th>
		<c:forEach items="${command.locales}" var="loc">
			<td valign="top" class="${loc}">
				<spring:bind path="command.descriptionsByLocale[${loc}].description">
					<textarea name="${status.expression}" rows="4" cols="50">${status.value}</textarea>					
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>
			</td>
		</c:forEach>
	</tr>		
	<tr>
		<th>
 			<spring:message code="Concept.conceptClass" /> <img class="help_icon_bottom" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.conceptClass.help"/>"/>
		</th>
		<td valign="top">
			<spring:bind path="command.concept.conceptClass">
				<select class="smallWidth" name="${status.expression}" id="conceptClass" onChange="changeClass(this);">
					<c:forEach items="${classes}" var="cc">
						<option value="${cc.conceptClassId}"
							<c:if test="${cc.conceptClassId == status.value}">selected="selected"</c:if>>${cc.name}
						</option>
					</c:forEach>
				</select>
				<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
			</spring:bind>
		</td>
	</tr>
	<tr>
		<th valign="top">
			<spring:message code="Concept.set"/> <img class="help_icon" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.isSet.help"/>"/>
		</th>
		<td>
			<spring:bind path="command.concept.set">
				<input type="hidden" name="_${status.expression}" value=""/>
				<input type="checkbox" name="${status.expression}" id="conceptSet" <c:if test="${status.value}">checked="checked"</c:if> onClick="changeSetStatus(this)" />
				<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
			</spring:bind>
		</td>
	</tr>
	<tr id="conceptSetRow">
		<th valign="top"><spring:message code="Concept.conceptSets"/></th>
		<td valign="top">
			<spring:bind path="command.concept.conceptSets">
				<input type="hidden" name="${status.expression}" id="conceptSets" size="40" value='<c:forEach items="${command.concept.conceptSets}" var="set">${set.concept.conceptId} </c:forEach>' />
			</spring:bind>
			<table cellpadding="0" cellspacing="0">
				<tr>
					<td valign="top">
						<select class="largeWidth" size="6" id="conceptSetsNames" multiple="multiple" onkeyup="listKeyPress('conceptSetsNames', 'conceptSets', ' ', event);">
							<c:forEach items="${command.concept.conceptSets}" var="set">
								<option value="${set.concept.conceptId}"><openmrs:format concept="${set.concept}"/> (${set.concept.conceptId})</option>
							</c:forEach>
						</select>
					</td>
					<td valign="top" class="buttons">
						<span dojoType="ConceptSearch" widgetId="sSearch"></span><span dojoType="OpenmrsPopup" searchWidget="sSearch" searchTitle='<spring:message code="Concept.find"/>' changeButtonValue='<spring:message code="general.add"/>' showConceptIds="true"></span>
						<input type="button" value="<spring:message code="general.remove"/>" class="smallButton" onClick="removeItem('conceptSetsNames', 'conceptSets', ' ');" style="display: block" />
						<input type="button" value="<spring:message code="general.move_up"/>" class="smallButton" onClick="moveUp('conceptSetsNames', 'conceptSets');" style="display: block" />
						<input type="button" value="<spring:message code="general.move_down"/>" class="smallButton" onClick="moveDown('conceptSetsNames', 'conceptSets');" style="display: block" />
					</td>
				</tr>
			</table>
		</td>
	</tr>
	<tr>
		<th>
			<spring:message code="Concept.datatype" /> <img class="help_icon" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.datatype.help"/>"/>
		</th>
		<td valign="top">
			<spring:bind path="command.concept.datatype">
				<select class="smallWidth" name="${status.expression}" id="datatype" onChange="changeDatatype(this);"
										   <c:if test="${dataTypeReadOnly == true}">disabled="disabled"</c:if>>
					<c:forEach items="${datatypes}" var="cd">
						<option value="${cd.conceptDatatypeId}"
							<c:if test="${cd.conceptDatatypeId == status.value}">selected="selected"</c:if>>${cd.name}</option>
					</c:forEach>
				</select>
				<c:if test="${dataTypeReadOnly == true && isBoolean != null && isBoolean == true}">					
					<input type="button" value="<spring:message code="Concept.boolean.add.answer"/>" 
						   onclick="addAnswerToBooleanConcept('<spring:message code="Concept.boolean.confirm.add.answer"/>', '${command.concept.conceptId}')" 
						   title="<spring:message code="Concept.boolean.change.tooltip"/>"/>
					<spring:message code="Concept.boolean.warning.irreversible"/>
					<span id="addAnswerError" class="error" style="display:none"></span>
				</c:if>				
				<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
			</spring:bind>
			<c:if test="${dataTypeReadOnly == true && isBoolean == null}">(<spring:message code="Concept.datatype.readonly"/>)</c:if>
		</td>
	</tr>
	<tr id="codedDatatypeRow">
		<th valign="top">
			<spring:message code="Concept.answers"/> 
			<img class="help_icon" id="tooltipCodedIcon" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.datatype.coded.help"/>" />
		</th>
		<td>
			<spring:bind path="command.concept.answers">
				<input type="hidden" name="${status.expression}" id="answerIds" size="40" value='<c:forEach items="${command.conceptAnswers}" var="answer">${answer.key} </c:forEach>' />
				<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
			</spring:bind>
			<table cellspacing="0" cellpadding="0">
				<tr>
					<td valign="top">
						<select class="largeWidth" size="6" id="answerNames" multiple="multiple" onKeyUp="listKeyPress('answerNames', 'answerIds', ' ', event)">
							<c:forEach items="${command.conceptAnswers}" var="answer">
								<option value="${answer.key}">${answer.value} (${fn:substring(answer.key, 0, fn:indexOf(answer.key, '^'))})</option>
							</c:forEach>
						</select>
					</td>
					<td valign="top" class="buttons">
						<input type="button" value="<spring:message code="general.add"/>" class="smallButton" onClick="addAnswer();"/><br/>
						<input type="button" value="<spring:message code="general.remove"/>" class="smallButton" onClick="removeItem('answerNames', 'answerIds', ' ');"/><br/>
						<input type="button" value="<spring:message code="general.move_up"/>" class="smallButton" onClick="moveUp('answerNames', 'answerIds');" style="display: block" />
						<input type="button" value="<spring:message code="general.move_down"/>" class="smallButton" onClick="moveDown('answerNames', 'answerIds');" style="display: block" />
					</td>
				</tr>
			</table>
		</td>
	</tr>
	<tr id="numericDatatypeRow">
		<th valign="top">
			<spring:message code="ConceptNumeric.name"/>
			<img class="help_icon" id="tooltipNumericIcon" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.datatype.numeric.help"/>" />
		</th>
		<td>
			<table border="0">
				<tr>
					<th valign="middle"><spring:message code="ConceptNumeric.absoluteHigh"/></th>
					<td valign="middle">
						<spring:bind path="command.hiAbsolute">
							<input type="text" name="${status.expression}" value="${status.value}" class="smallWidth" />
							<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
				</tr>
				<tr>
					<th valign="middle"><spring:message code="ConceptNumeric.criticalHigh"/></th>
					<td valign="middle">
						<spring:bind path="command.hiCritical">
							<input type="text" name="${status.expression}" value="${status.value}" class="smallWidth" />
							<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
				</tr>
				<tr>
					<th valign="middle"><spring:message code="ConceptNumeric.normalHigh"/></th>
					<td valign="middle">
						<spring:bind path="command.hiNormal">
							<input type="text" name="${status.expression}" value="${status.value}" class="smallWidth" />
							<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
				</tr>
				<tr>
					<th valign="middle"><spring:message code="ConceptNumeric.normalLow"/></th>
					<td valign="middle">
						<spring:bind path="command.lowNormal">
							<input type="text" name="${status.expression}" value="${status.value}" class="smallWidth" />
							<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
				</tr>
				<tr>
					<th valign="middle"><spring:message code="ConceptNumeric.criticalLow"/></th>
					<td valign="middle">
						<spring:bind path="command.lowCritical">
							<input type="text" name="${status.expression}" value="${status.value}" class="smallWidth" />
							<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
				</tr>
				<tr>
					<th valign="middle"><spring:message code="ConceptNumeric.absoluteLow"/></th>
					<td valign="middle">
						<spring:bind path="command.lowAbsolute">
							<input type="text" name="${status.expression}" value="${status.value}" class="smallWidth" />
							<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
				</tr>
				<tr>
					<td></td>
					<td colspan="2"><small><em>(<spring:message code="ConceptNumeric.inclusive"/>)</em></small>
					</td>
				</tr>
				<tr>
					<th><spring:message code="ConceptNumeric.units"/></th>
					<td colspan="2">
						<spring:bind path="command.units">
							<input type="text" name="${status.expression}" value="${status.value}" class="mediumWidth" />
							<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
				</tr>
				<tr>
					<th><spring:message code="ConceptNumeric.precise"/></th>
					<td colspan="2">
						<spring:bind path="command.precise">
							<input type="hidden" name="_${status.expression}" value=""/>
							<input type="checkbox" name="${status.expression}" <c:if test="${status.value}">checked="checked"</c:if>/>
							<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
				</tr>
			</table>
		</td>
	</tr>
    <tr id="complexDatatypeRow">
        <th valign="top">
			<spring:message code="ConceptComplex.handler"/>
			<img class="help_icon" id="tooltipComplexIcon" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.datatype.complex.help"/>" />
		</th>
        <td>
			<spring:bind path="command.handlerKey">
				<select name="${status.expression}"> 
					<option value=""><spring:message code="general.select"/>...</option>
					<c:forEach var="handler" items="${handlers}">
						<option value="${handler.key}" <c:if test="${handler.key == status.value}">selected="selected"</c:if>>
					        ${handler.key}
					    </option>
					</c:forEach>
				</select>
				<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
			</spring:bind>
                 
            <!-- All handler key to class mappings
            <table>
             <c:forEach var="h" items="${handlers }">
                <tr name="handlerRow" id="handler_${h.key}">
                    <th >
                        <spring:message code="general.class"/>
                    </th>
                    <td>
                        ${h.value.class.name }
                    </td>
                </tr>
             </c:forEach>
            </table>
            -->
        </td>
    </tr>
	<tr id="conceptMapRow">
		<th valign="top">
			<spring:message code="Concept.mappings"/> <img class="help_icon" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.mappings.help"/>"/>
		</th>
		<td>
			<table id="conceptMapTable" cellpadding="3" cellspacing="1">
				<tr class="headerRow">
					<th class="alignCenter" align="center"><spring:message code="Concept.mappings.relationship"/></th>
					<th><spring:message code="ConceptReferenceTerm.source"/></th>
					<th><spring:message code="ConceptReferenceTerm.code"/></th>
					<th><spring:message code="general.name"/></th>
					<th class="removeButtonCol">&nbsp;</th>
				</tr>
				<c:forEach var="mapping" items="${command.conceptMappings}" varStatus="mapStatus">
				<spring:nestedPath path="command.conceptMappings[${mapStatus.index}]">
				<tr id="mapping-${mapStatus.index}">
					<c:choose>
					<c:when test="${mapping.conceptMapId != null}">
					<td>
						<spring:bind path="conceptMapType">
						<select name="${status.expression}">
							<openmrs:forEachRecord name="conceptMapType">
							<option value="${record.conceptMapTypeId}" <c:if test="${record.conceptMapTypeId == status.value}">selected="selected"</c:if> >
								${record.name}
							</option>
						</openmrs:forEachRecord>
						</select>
						<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
					<td <c:if test="${mapStatus.index % 2 == 0}">class='evenRow'</c:if>>${mapping.conceptReferenceTerm.conceptSource.name}</td>
					<td <c:if test="${mapStatus.index % 2 == 0}">class='evenRow'</c:if>>${mapping.conceptReferenceTerm.code}
					<spring:bind path="conceptReferenceTerm">
						<input type="hidden" name="${status.expression}" value="${status.value}" />
					</spring:bind>
					</td>
					<td <c:if test="${mapStatus.index % 2 == 0}">class='evenRow'</c:if>>${mapping.conceptReferenceTerm.name}</td>
					</c:when>
					<c:otherwise>
					<td>
						<spring:bind path="conceptMapType">
						<select name="${status.expression}">
							<openmrs:forEachRecord name="conceptMapType">
							<option value="${record.conceptMapTypeId}" <c:if test="${record.conceptMapTypeId == status.value}">selected="selected"</c:if> >
								${record.name}
							</option>
						</openmrs:forEachRecord>
						</select>
						<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
					<td>
						<select id="term[${mapStatus.index}].source">
							<option value=""><spring:message code="ConceptReferenceTerm.searchAllSources" /></option>
							<openmrs:forEachRecord name="conceptSource">
							<option value="${record.conceptSourceId}" <c:if test="${record.conceptSourceId == mapping.conceptReferenceTerm.conceptSource.conceptSourceId}">selected="selected"</c:if>>
								${record.name}
							</option>
							</openmrs:forEachRecord>
						</select>
					</td>
					<td>
						<spring:bind path="conceptReferenceTerm">
						<input type="text" id="term[${mapStatus.index}].code" name="term.code" value="${mapping.conceptReferenceTerm.code}" size="25" />
						<input type="hidden" id="${status.expression}" name="${status.expression}" value="${status.value}" />
						<script type="text/javascript">
							addAutoComplete('term[${mapStatus.index}].code', 'term[${mapStatus.index}].source', 'conceptMappings[${mapStatus.index}].conceptReferenceTerm', 'term[${mapStatus.index}].name')
						</script>
						</spring:bind>
					</td>
					<td <c:if test="${mapStatus.index % 2 == 0}">class='evenRow'</c:if>>
						<input type="text" id="term[${mapStatus.index}].name" size="25" value="${mapping.conceptReferenceTerm.name}" readonly="readonly" />
					</td>
					</c:otherwise>
					</c:choose>
					<td>
						<input type="button" value='<spring:message code="general.remove"/>' class="smallButton" onClick="removeParentElement(this.parentNode)" />
						<spring:bind path="command.conceptMappings[${mapStatus.index}]" ignoreNestedPath="true">
							<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
						</spring:bind>
					</td>
				</tr>
				</spring:nestedPath>
				</c:forEach>
				<tr id="newConceptMapping" style="display: none">
					<td valign="top">
						<select name="type.name">
							<openmrs:forEachRecord  name="conceptMapType">
								<option value="${record.conceptMapTypeId}">
									${record.name}
								</option>
							</openmrs:forEachRecord>
						</select>
					</td>
					<td valign="top">
						<select name="term.source" >
							<option value=""><spring:message code="ConceptReferenceTerm.searchAllSources" /></option>
							<openmrs:forEachRecord  name="conceptSource">
							<option value="${record.conceptSourceId}">
								${record.name}
							</option>
							</openmrs:forEachRecord>
						</select>
					</td>
					<td valign="top">
						<input type="text" name="term.code" size="25" />
						<input type="hidden" name="termId" />
					</td>
					<td>
						<input type="text" name="term.name" size="25" readonly="readonly" />
					</td>
					<td>
						<input type="button" value='<spring:message code="general.remove"/>' class="smallButton" onClick="removeParentElement(this.parentNode)" />
					</td>
				</tr>
				
				<tr>
					<td colspan="3" valign="top" align="left">
						<input id="addMapButton" type="button" value='<spring:message code="Concept.mapping.add"/>' class="smallButton" 
							   onClick="addConceptMapping(${fn:length(command.conceptMappings)})" />
					</td>
					<td align="right">
						<openmrs:hasPrivilege privilege="Create Reference Terms While Editing Concepts">
						<input class="smallButton" type="button" 
						       value="<spring:message code="ConceptReferenceTerm.createNewTerm" />" 
						       onclick="javascript:$j('#create-new-term-dialog').dialog('open')" />
						</openmrs:hasPrivilege>
					</td>
					<td></td>
				</tr>
				
			</table>
		</td>
	</tr>
	<tr>
		<th>
			<spring:message code="Concept.version" /> <img class="help_icon_bottom" src="${pageContext.request.contextPath}/images/help.gif" border="0" title="<spring:message code="Concept.version.help"/>"/>
		</th>
		<td>
			<spring:bind path="command.concept.version">
				<input class="smallWidth" type="text" name="${status.expression}" value="${status.value}" class="smallWidth" />
				<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
			</spring:bind>
		</td>
	</tr>
	<c:if test="${command.concept.creator != null}">
		<tr>
			<th><spring:message code="general.createdBy" /></th>
			<td>
				${command.concept.creator.personName} -
				<openmrs:formatDate date="${command.concept.dateCreated}" type="long" />
			</td>
		</tr>
	</c:if>
	<c:if test="${command.concept.changedBy != null}">
		<tr>
			<th><spring:message code="general.changedBy" /></th>
			<td>
				${command.concept.changedBy.personName} -
				<openmrs:formatDate date="${command.concept.dateChanged}" type="long" />
			</td>
		</tr>
	</c:if>
	
	<tr><td colspan="2"><br/></td></tr>
	
	<c:if test="${fn:length(command.questionsAnswered) > 0}">
		<tr>
			<th valign="top"><spring:message code="dictionary.questionsAnswered" /></th>
			<td>
				<c:forEach items="${command.questionsAnswered}" var="question">
					<a href="concept.htm?conceptId=${question.conceptId}"><openmrs:format concept="${question}" /></a><br/>
				</c:forEach>
			</td>
		</tr>
	</c:if>
	
	<c:if test="${fn:length(command.containedInSets) > 0}">
		<tr>
			<th valign="top"><spring:message code="dictionary.containedInSets" /></th>
			<td>
				<c:forEach items="${command.containedInSets}" var="set">
					<a href="concept.htm?conceptId=${set.conceptSet.conceptId}"><openmrs:format concept="${set.conceptSet}" /></a><br/>
				</c:forEach>
			</td>
		</tr>
	</c:if>
	
	<c:if test="${fn:length(command.formsInUse) > 0}">
		<tr>
			<th valign="top"><spring:message code="dictionary.forms" /></th>
			<td>
				<c:forEach items="${command.formsInUse}" var="form">
					<a href="${pageContext.request.contextPath}/admin/forms/formSchemaDesign.form?formId=${form.formId}">${form.name}</a><br/>
				</c:forEach>
			</td>
		</tr>
	</c:if>
	
	<tr><td colspan="2"><br/></td></tr>
	
	<tr>	
		<th valign="top">
			<b><spring:message code="Concept.resources" /></b>
		</th>
		<td>
			<a href="index.htm?phrase=<openmrs:format concept="${command.concept}" />"
			       target="_similar_terms" onclick="addName(this)">Similar Concepts</a><br/>
			<a href="http://www2.merriam-webster.com/cgi-bin/mwmednlm?book=Medical&va=<openmrs:format concept="${command.concept}" />"
			       target="_blank" onclick="addName(this)">Merriam Webster&reg;</a><br/>
			<a href="http://www.google.com/search?q=<openmrs:format concept="${command.concept}" />"
			       target="_blank" onclick="addName(this)">Google&trade;</a><br/>
			<a href="http://www.utdol.com/application/vocab.asp?submit=Go&search=<openmrs:format concept="${command.concept}" />"
			       target="_blank" onclick="addName(this)">UpToDate&reg;</a><br/>
			<a href="http://dictionary.reference.com/search?submit=Go&q=<openmrs:format concept="${command.concept}" />"
			       target="_blank" onclick="addName(this)">Dictionary.com&reg;</a><br/>
			<a href="http://search.atomz.com/search/?sp-a=sp1001878c&sp-q=<openmrs:format concept="${command.concept}" />"
			       target="_blank" onclick="addName(this)">Lab Tests Online</a><br/>
			<a href="http://en.wikipedia.org/wiki/<openmrs:format concept="${command.concept}" />"
			       target="_blank"><spring:message code="Concept.wikipedia" /></a>
		</td>
	</tr>
	
</table>

<%-- These will be used when the from is submitted to determine existing mappings that have been removed by the user --%>
<c:forEach items="${command.conceptMappings}" varStatus="vStatus">
<input type="hidden" name="_conceptMappings[${vStatus.index}].conceptReferenceTerm" value="" />
</c:forEach>

<div id="saveDeleteButtons" style="margin-top: 15px">
<c:if test="${conceptsLocked != 'true'}">	
	<input type="submit" name="action" value="<spring:message code="Concept.save"/>" onMouseUp="removeHiddenRows()"/>
	
	<c:if test="${command.concept.conceptId != null}">
		<openmrs:hasPrivilege privilege="Delete Concepts">
			 &nbsp; &nbsp; &nbsp;
			<input type="submit" name="action" value="<spring:message code="Concept.delete"/>" onclick="return confirm('<spring:message code="Concept.confirmDelete"/>')"/>
		</openmrs:hasPrivilege>
	</c:if>
</c:if>
</div>

</form>

<br/>
<br/>

<openmrs:hasPrivilege privilege="Manage Concepts">
	<c:if test="${command.concept.conceptId!=null && command.concept.retired==false }">
	<form action="" method="post">
		<fieldset>
			<h4><spring:message code="general.retire"/> <spring:message code="Concept"/></h4>
			<b><spring:message code="general.reason"/></b>
			<input type="text" value="" size="40" name="retiredReason" />
		
			<br/>
			<input type="submit" value='<spring:message code="general.retire"/>' name="action"/>
		</fieldset>
	</form>
	</c:if>
</openmrs:hasPrivilege>

<openmrs:hasPrivilege privilege="Create Reference Terms While Editing Concepts">
<div id="create-new-term-dialog" title="<spring:message code="ConceptReferenceTerm.newTermForm"/>">
	<div id="newTermErrorMsg" class="error" style="display: none" align="center"></div>
	<div id="successMsg" class="newTermSuccessMsg" align="center" style="display: none">
		<spring:message code="ConceptReferenceTerm.saved"/>
	</div><br />
	<fieldset>
	<legend><spring:message code="ConceptReferenceTerm.details"/></legend>
	<table cellpadding="3" cellspacing="3" align="center">
		<tr>
            <th class="alignRight" valign="top"><spring:message code="ConceptReferenceTerm.code"/><span class="required">*</span></th>
            <td valign="top">
                <input type="text" id="newTermCode" value=""/>
            </td>
        </tr>
        <tr>
            <th class="alignRight" valign="top"><spring:message code="general.name"/></th>
            <td valign="top">
                <input type="text" id="newTermName" value=""/>
            </td>
        </tr>
        <tr>
        <tr>
            <th class="alignRight" valign="top"><spring:message code="ConceptReferenceTerm.source"/><span class="required">*</span></th>
            <td valign="top">
				<select id="newTermSource">
					<option value=""></option>
					<openmrs:forEachRecord name="conceptSource">
					<option value="${record.conceptSourceId}">
						${record.name}
					</option>
					</openmrs:forEachRecord>
				</select>
			</td>
        </tr>
     </table>
     </fieldset>
     <br />
     <div align="center">
     	<input type="button" value="<spring:message code="general.save"/>" onclick="createNewTerm()"/> &nbsp;
        <input id="cancelOrDone" type="button" value="<spring:message code="general.cancel"/>" 
        			onclick="javascript:$j('#create-new-term-dialog').dialog('close')"/>
     </div>
</div>
<script type="text/javascript">
$j(document).ready( function() {
	$j("#create-new-term-dialog").dialog({
		autoOpen: false,
		resizable: false,
		width:'auto',
		height:'auto',
		modal: true,
		beforeClose: function(event, ui){
			//clear all field and hide the error message just in case it is visible 
			//due to a previous unsuccessful attempt to make a hidden app visible
			resetNewTermForm();
			$j("#cancelOrDone").val('<spring:message code="general.cancel"/>');
		}
	});
});

function createNewTerm(){
	DWRConceptService.createConceptReferenceTerm( $j('#newTermCode').val(), $j('#newTermSource').val(),$j('#newTermName').val(), function(errors) {
		if(errors){
			var errorMessages = "";
			for (var i in errors) {
				errorMessages+=errors[i]+"<br />";
			}
			
			$j('#newTermErrorMsg').html(errorMessages);
			$j('#successMsg').hide();//just incase it is visible
			$j('#newTermErrorMsg').show();
		}else{
			$j('#newTermErrorMsg').hide();//just incase it is visible
			$j('#successMsg').show();
			window.setTimeout("resetNewTermForm()", 1500);
			$j("#cancelOrDone").val('<spring:message code="general.done"/>');
		}
	});
}

function resetNewTermForm(){
	$j('#newTermCode').val('');
	$j('#newTermName').val('');
	$j('select#newTermSource').val(0);
	$j('#newTermErrorMsg').html('');
	$j('#newTermErrorMsg').hide();
	$j('#successMsg').hide();
}
</script>
</openmrs:hasPrivilege>

<openmrs:extensionPoint pointId="org.openmrs.dictionary.conceptFormFooter" type="html" />

<div id="addAnswer" style="display: none">
	<div id="addAnswerError"><spring:message code="Concept.noConceptSelected"/></div>
	<div id="addConceptOrDrug">
		<h3><a href="#"><spring:message code="Concept.find"/></a></h3>
		<div><input type="text" name="newAnswerConcept" id="newAnswerConcept" size="20"/></div>
		<h3><a href="#"><spring:message code="ConceptDrug.find"/></a></h3>
		<div><input type="text" name="newAnswerDrug" id="newAnswerDrug" size="20"/></div>
	</div>
	<input type="hidden" name="newAnswerId" id="newAnswerId"/>
	<input type="hidden" name="newAnswerType" id="newAnswerType"/>
</div>

<script type="text/javascript">
	$j(document).ready(function(){
		// create the Add Answer dialog
		$j('#addAnswer').dialog({
			autoOpen: false,
			modal: true,
			title: '<spring:message code="conceptAnswer.title" javaScriptEscape="true"/>',
			width: 'auto',
			open: function() {
				$j("#newAnswerConcept").val(""); 
				$j("#newAnswerDrug").val(""); 
				$j("input[name=newAnswerId]").val(""); 
				$j("input[name=newAnswerType]").val(""); },
			close: function() { 
				$j("#addAnswerError").hide(); 
				$j("#newAnswerConcept").autocomplete("close"); 
				$j("#newAnswerDrug").autocomplete("close"); },
			buttons: { '<spring:message code="general.add"/>': function() { handleAddAnswer(); },
					   '<spring:message code="general.cancel"/>': function() { $j(this).dialog("close"); }
			}
		});
		
		// set up accordion for adding concepts or drugs
		$j('#addConceptOrDrug').accordion({
			autoHeight: false,
			change: function(event, ui){
				// only hide the error if it is visible
				$j("#addAnswerError:visible").hide('blind'); 
				// clear previously selected data
				ui.oldContent.find('input').val("");
				$j("input[name=newAnswerId]").val("");
				$j("input[name=newAnswerType]").val("");
				// focus on the newly revealed input
				ui.newContent.find('input').focus();
			}
		});
		
		// concept answer autocompletes
		var answerCallback = new CreateCallback();
		var autoAddAnswerConcept = new AutoComplete("newAnswerConcept", answerCallback.conceptCallback(), {
			select: function(event, ui) {
				$j("input[name=newAnswerId]").val(ui.item.object.conceptId);
				$j("input[name=newAnswerType]").val("concept");
			}
		});
		var autoAddAnswerDrug = new AutoComplete("newAnswerDrug", answerCallback.drugCallback(), {
			select: function(event, ui) {
				$j("input[name=newAnswerId]").val(ui.item.object.drugId);
				$j("input[name=newAnswerType]").val("drug");
			}
		});
		
	});

	function addAnswer() {
		$j('#addAnswer').dialog('open');
		$j('#addConceptOrDrug input:visible').focus();
	}
	
	function handleNewAnswerObject(newAnswer) {
		var nameListBox = document.getElementById("answerNames");
		var idListBox = document.getElementById("answerIds");
		var options = nameListBox.options;
		addOption(newAnswer, options);
		copyIds(nameListBox.id, idListBox.id, ' ');
		$j("#addAnswer").dialog('close');
	}

	function handleAddAnswer() {
		var newAnswerId = $j("input[name=newAnswerId]").val();
		var newAnswerType = $j("input[name=newAnswerType]").val();
		if (newAnswerId == "" || newAnswerType == "") {
			$j("#addAnswerError").show('highlight', 1000);
			return;
		}
		
		if (newAnswerType == "concept") {
			DWRConceptService.getConcept(newAnswerId, handleNewAnswerObject);
		} else if (newAnswerType == "drug") {
			DWRConceptService.getDrug(newAnswerId, handleNewAnswerObject);
		}
	}

	selectTab(document.getElementById("${command.locales[0]}Tab"));
</script>

<%@ include file="/WEB-INF/template/footer.jsp"%>
