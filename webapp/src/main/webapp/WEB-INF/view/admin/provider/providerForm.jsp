<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Provider" otherwise="/login.htm" redirect="/admin/provider/provider.form" />

<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>

<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
<openmrs:htmlInclude file="/scripts/dojoConfig.js" />
<openmrs:htmlInclude file="/scripts/dojo/dojo.js" />

<style>
	#table th { text-align: left; }
	td.fieldNumber { 
		width: 5px;
		white-space: nowrap;
	}
</style>

<h2><spring:message code="Provider.manage.title"/></h2>

<spring:hasBindErrors name="provider">
	<spring:message code="fix.error"/>
	<br />
</spring:hasBindErrors>

<b class="boxHeader">
<c:if test="${provider.providerId == null}">
	<spring:message code="Provider.create"/>
</c:if>
<c:if test="${provider.providerId != null}">
	<spring:message code="Provider.edit"/>
</c:if>
</b>

<div class="box">
	<form method="post">
		
		<table cellpadding="3" cellspacing="0">
		
			<tr>
				<th><spring:message code="Provider.identifier"/></th>
				<td colspan="4">
					<spring:bind path="provider.identifier">			
						<input type="text" name="${status.expression}" size="10" 
							   value="${status.value}" />
					   
						<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if> 
					</spring:bind>
				</td>
			</tr>
			<tr>
				<th><spring:message code="Provider.person"/></th>
				<td>
					<spring:bind path="provider.person">
					<openmrs:fieldGen type="org.openmrs.Person" formFieldName="${status.expression}" val="${status.editor.value}"/>
						<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
					</spring:bind>
				</td>
				<td>&nbsp;&nbsp;&nbsp;<spring:message code="general.or" />&nbsp;&nbsp;&nbsp;</td>
				<th><spring:message code="Provider.name"/></th>
				<td>
					<spring:bind path="provider.name">			
						<input type="text" name="${status.expression}" size="25" 
							   value="${status.value}" />
					   
						<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if> 
					</spring:bind>
				</td>
			</tr>
		
		</table>
		
		<br/>
	
	<input type="hidden" name="phrase" value='<request:parameter name="phrase" />'/>
	<input type="submit" name="saveProviderButton" value='<spring:message code="Provider.save"/>'>
	&nbsp;
	<input type="button" value='<spring:message code="general.cancel"/>' onclick="document.location='index.htm'">
	
	</form>
	</div>
	
	<br/>
    <br/>

	<c:if test="${provider.providerId != null}">
		<div class="box">
			<form method="post">
				<table cellpadding="3" cellspacing="0">
					<tr>
						<th><spring:message code="general.createdBy" /></th>
						<td>
							<a href="#View User" onclick="return gotoUser(null, '${provider.creator.userId}')">${provider.creator.personName}</a> -
							<openmrs:formatDate date="${provider.dateCreated}" type="medium" />
						</td>
					</tr>
					<c:if test="${provider.retiredBy == null}">
						<tr id="retiredReason">
							<th><spring:message code="general.retiredReason" /></th>
							<td>
								<spring:bind path="provider.retired">
									<input type="hidden" name="${status.expression}" value="true"/>
								</spring:bind>

								<spring:bind path="provider.retireReason">
									<input type="text" id="retire" value="${status.value}" name="${status.expression}" size="40" />
									<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
								</spring:bind>
							</td>
						</tr>
						<tr>
							<td><input type="submit" name="retireProviderButton"
								value='<spring:message code="Provider.retire"/>'></td>
						</tr>
					</c:if>	
														
					<c:if test="${provider.retiredBy != null}">
						<tr id="retiredBy">
							<th><spring:message code="general.retiredBy" /></th>
							<td>
								<a href="#View User" onclick="return gotoUser(null, '${provider.retiredBy.userId}')">${provider.retiredBy.personName}</a> -
								<openmrs:formatDate date="${provider.dateRetired}" type="medium" />
							</td>
						</tr>
						<tr>
							<th><spring:message code="general.retiredReason" /></th>
							<td>${provider.retireReason}</td>
						</tr>
						<tr>
							<td><input type="submit" name="unretireProviderButton"
								value='<spring:message code="Provider.unretire"/>'></td>
						</tr>
					</c:if>		
				</table>
			</form>
		</div>
	</c:if>

<%@ include file="/WEB-INF/template/footer.jsp" %>