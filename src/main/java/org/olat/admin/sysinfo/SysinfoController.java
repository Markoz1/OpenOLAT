/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.admin.sysinfo;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.olat.admin.sysinfo.manager.SessionStatsManager;
import org.olat.admin.sysinfo.model.SessionsStats;
import org.olat.basesecurity.BaseSecurity;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.helpers.Settings;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.WebappHelper;


/**
*  Description:<br>
*  all you wanted to know about your running OLAT system
*
* @author Felix Jost
*/
public class SysinfoController extends FormBasicController {
	
	private final BaseSecurity securityManager;
	private final SessionStatsManager sessionStatsManager;
	
	/**
	 * @param ureq
	 * @param wControl
	 */
	public SysinfoController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl, "sysinfo");
		
		securityManager = CoreSpringFactory.getImpl(BaseSecurity.class);
		sessionStatsManager = CoreSpringFactory.getImpl(SessionStatsManager.class);

		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		Formatter format = Formatter.getInstance(getLocale());

		//runtime informations
		FormLayoutContainer runtimeCont = FormLayoutContainer.createDefaultFormLayout("runtime", getTranslator());
		formLayout.add(runtimeCont);
		formLayout.add("runtime", runtimeCont);

		String startup = format.formatDateAndTime(new Date(WebappHelper.getTimeOfServerStartup()));
		uifactory.addStaticTextElement("runtime.startup", "runtime.startup", startup, runtimeCont);

		String time = format.formatDateAndTime(new Date()) + " (" + Calendar.getInstance().getTimeZone().getDisplayName(false, TimeZone.SHORT, ureq.getLocale()) + ")";
		uifactory.addStaticTextElement("runtime.time", "runtime.time", time, runtimeCont);

		//memory
		String memoryPage = velocity_root + "/memory.html";
		FormLayoutContainer memoryCont = FormLayoutContainer.createCustomFormLayout("memory", getTranslator(), memoryPage);
		runtimeCont.add(memoryCont);
		memoryCont.contextPut("used", getHeapValue());
		memoryCont.contextPut("tooltip", getHeapTooltip());
		memoryCont.setLabel("runtime.memory", null);
		
		FormLayoutContainer permGenCont = FormLayoutContainer.createCustomFormLayout("permgen", getTranslator(), memoryPage);
		runtimeCont.add(permGenCont);
		permGenCont.contextPut("used", getNonHeapValue());
		permGenCont.contextPut("tooltip", getNonHeapTooltip());
		permGenCont.setLabel("runtime.memory.permGen", null);
		
		//controllers
		int controllerCnt = DefaultController.getControllerCount();
		uifactory.addStaticTextElement("controllercount", "runtime.controllercount", Integer.toString(controllerCnt), runtimeCont);
		int numOfDispatchingThreads = sessionStatsManager.getConcurrentCounter();
		uifactory.addStaticTextElement("dispatchingthreads", "runtime.dispatchingthreads", Integer.toString(numOfDispatchingThreads), runtimeCont);
		
		//sessions and clicks
		String sessionAndClicksPage = velocity_root + "/session_clicks.html";
		FormLayoutContainer sessionAndClicksCont = FormLayoutContainer.createCustomFormLayout("session_clicks", getTranslator(), sessionAndClicksPage);
		runtimeCont.add(sessionAndClicksCont);
		sessionAndClicksCont.setLabel("sess.and.clicks", null);
		
		Calendar lastLoginMonthlyLimit = Calendar.getInstance();
		//users monthly
		lastLoginMonthlyLimit.add(Calendar.MONTH, -1);
		Long userLastMonth = securityManager.countUniqueUserLoginsSince(lastLoginMonthlyLimit.getTime());
		lastLoginMonthlyLimit.add(Calendar.MONTH, -5); // -1 -5 = -6 for half a year
		Long userLastSixMonths = securityManager.countUniqueUserLoginsSince(lastLoginMonthlyLimit.getTime());
		lastLoginMonthlyLimit.add(Calendar.MONTH, -11); // -1 -11 = -12 for one year
		Long userLastYear = securityManager.countUniqueUserLoginsSince(lastLoginMonthlyLimit.getTime());
		sessionAndClicksCont.contextPut("users1month", userLastMonth.toString());
		sessionAndClicksCont.contextPut("users6month", userLastSixMonths.toString());
		sessionAndClicksCont.contextPut("usersyear", userLastYear.toString());
		
		//users daily
		Calendar lastLoginDailyLimit = Calendar.getInstance();
		lastLoginDailyLimit.add(Calendar.DAY_OF_YEAR, -1);
		Long userLastDay = securityManager.countUniqueUserLoginsSince(lastLoginDailyLimit.getTime());
		lastLoginDailyLimit.add(Calendar.DAY_OF_YEAR, -6); // -1 - 6 = -7 for last week
		Long userLast6Days = securityManager.countUniqueUserLoginsSince(lastLoginDailyLimit.getTime());
		sessionAndClicksCont.contextPut("userslastday", userLastDay.toString());
		sessionAndClicksCont.contextPut("userslastweek", userLast6Days.toString());
		
		//last 5 minutes
		long activeSessions = sessionStatsManager.getActiveSessions(300);
		sessionAndClicksCont.contextPut("count5Minutes", String.valueOf(activeSessions));
		SessionsStats stats = sessionStatsManager.getSessionsStatsLast(300);
		sessionAndClicksCont.contextPut("click5Minutes", String.valueOf(stats.getAuthenticatedClickCalls()));
		sessionAndClicksCont.contextPut("poll5Minutes", String.valueOf(stats.getAuthenticatedPollerCalls()));
		sessionAndClicksCont.contextPut("request5Minutes", String.valueOf(stats.getRequests()));
		sessionAndClicksCont.contextPut("minutes", String.valueOf(5));
		
		//last minute
		activeSessions = sessionStatsManager.getActiveSessions(60);
		sessionAndClicksCont.contextPut("count1Minute", String.valueOf(activeSessions));
		stats = sessionStatsManager.getSessionsStatsLast(60);
		sessionAndClicksCont.contextPut("click1Minute", String.valueOf(stats.getAuthenticatedClickCalls()));
		sessionAndClicksCont.contextPut("poll1Minute", String.valueOf(stats.getAuthenticatedPollerCalls()));
		sessionAndClicksCont.contextPut("request1Minute", String.valueOf(stats.getRequests()));
		sessionAndClicksCont.contextPut("oneMinute", "1");

		//server informations
		FormLayoutContainer serverCont = FormLayoutContainer.createDefaultFormLayout("server", getTranslator());
		formLayout.add(serverCont);
		formLayout.add("server", serverCont);
		
		//version
		uifactory.addStaticTextElement("version", "sysinfo.version", Settings.getFullVersionInfo(), serverCont);
		uifactory.addStaticTextElement("version.hg", "sysinfo.version.hg", WebappHelper.getChangeSet(), serverCont);
		String buildDate = format.formatDateAndTime(Settings.getBuildDate());
		uifactory.addStaticTextElement("version.date", "sysinfo.version.date", buildDate, serverCont);

		//cluster
		boolean clusterMode = "Cluster".equals(Settings.getClusterMode());
		MultipleSelectionElement clusterEl
			= uifactory.addCheckboxesHorizontal("cluster", "sysinfo.cluster", serverCont, new String[]{"xx"}, new String[]{""});
		clusterEl.setEnabled(false);
		clusterEl.select("xx", clusterMode);
		
		String nodeId = StringHelper.containsNonWhitespace(Settings.getNodeInfo()) ? Settings.getNodeInfo() : "N1";
		uifactory.addStaticTextElement("node", "sysinfo.node", nodeId, serverCont);

		File baseDir = new File(WebappHelper.getContextRoot());
		String baseDirPath = null;
		try {
			baseDirPath = baseDir.getCanonicalPath();
		} catch (IOException e1) {
			baseDirPath = baseDir.getAbsolutePath();
		}
		uifactory.addStaticTextElement("sysinfo.basedir", "sysinfo.basedir", baseDirPath, serverCont);
		uifactory.addStaticTextElement("sysinfo.olatdata", "sysinfo.olatdata", WebappHelper.getUserDataRoot(), serverCont);
	}
	
	private String getHeapValue() {
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		long used = memoryBean.getHeapMemoryUsage().getUsed();
		long max = memoryBean.getHeapMemoryUsage().getMax();
		return toPercent(used, max);
	}
	
	private String getHeapTooltip() {
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		long used = toMB(memoryBean.getHeapMemoryUsage().getUsed());
		long max = toMB(memoryBean.getHeapMemoryUsage().getMax());
		return translate("runtime.memory.tooltip", new String[]{ Long.toString(used), Long.toString(max)});
	}
	
	private String getNonHeapValue() {
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		long used = memoryBean.getNonHeapMemoryUsage().getUsed();
		long max = memoryBean.getNonHeapMemoryUsage().getMax();
		if(max == -1) {
			max = used;
		}
		return toPercent(used, max);
	}
	
	private String getNonHeapTooltip() {
		MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
		long used = toMB(memoryBean.getNonHeapMemoryUsage().getUsed());
		long maxBytes = memoryBean.getNonHeapMemoryUsage().getMax();
		long max = toMB(maxBytes);
		if(maxBytes == -1) {
			max = used;
		}
		return translate("runtime.memory.tooltip", new String[]{ Long.toString(used), Long.toString(max)});
	}
	
	private final String toPercent(long used, long max) {
		double ratio = (double)used / (double)max;
		double percent = ratio * 100.0d;
		return Math.round(percent) + "%";
	}
	
	private final long toMB(long val) {
		return val / (1024 * 1024);
	}
	
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}
}