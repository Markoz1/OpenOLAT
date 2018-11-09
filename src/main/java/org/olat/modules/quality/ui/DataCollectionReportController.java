/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.modules.quality.ui;

import static org.olat.modules.quality.ui.QualityUIFactory.formatTopic;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.modules.forms.EvaluationFormManager;
import org.olat.modules.forms.EvaluationFormSurvey;
import org.olat.modules.forms.FiguresBuilder;
import org.olat.modules.forms.SessionFilter;
import org.olat.modules.forms.SessionFilterFactory;
import org.olat.modules.forms.model.xml.Form;
import org.olat.modules.forms.ui.EvaluationFormFormatter;
import org.olat.modules.forms.ui.EvaluationFormReportsController;
import org.olat.modules.forms.ui.ReportSegment;
import org.olat.modules.quality.QualityDataCollection;
import org.olat.modules.quality.QualityDataCollectionView;
import org.olat.modules.quality.QualityDataCollectionViewSearchParams;
import org.olat.modules.quality.QualityExecutorParticipationSearchParams;
import org.olat.modules.quality.QualityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 20.07.2018<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class DataCollectionReportController extends FormBasicController {
	
	private Controller reportHeaderCtrl;
	private Controller reportsCtrl;
	
	private QualityDataCollection dataCollection;
	
	@Autowired
	private QualityService qualityService;
	@Autowired
	private EvaluationFormManager evaluationFormManager;

	public DataCollectionReportController(UserRequest ureq, WindowControl wControl, QualityDataCollection dataCollection) {
		super(ureq, wControl, "report");
		this.dataCollection = dataCollection;
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		QualityDataCollectionViewSearchParams searchParams = new QualityDataCollectionViewSearchParams();
		searchParams.setDataCollectionRef(dataCollection);
		QualityDataCollectionView dataCollectionView = qualityService.loadDataCollections(getTranslator(), searchParams, 0, -1).get(0);
		reportHeaderCtrl = new DataCollectionReportHeaderController(ureq, getWindowControl(), dataCollectionView);
		
		FiguresBuilder builder = FiguresBuilder.builder();
		QualityExecutorParticipationSearchParams countSearchParams = new QualityExecutorParticipationSearchParams();
		countSearchParams.setDataCollectionRef(dataCollection);
		Long participationCount = qualityService.getExecutorParticipationCount(countSearchParams);
		builder.withNumberOfParticipations(participationCount);
		builder.addCustomFigure(translate("data.collection.figures.title"), dataCollectionView.getTitle());
		builder.addCustomFigure(translate("data.collection.figures.topic"), formatTopic(dataCollectionView));
		if (StringHelper.containsNonWhitespace(dataCollectionView.getPreviousTitle())) {
			builder.addCustomFigure(translate("data.collection.figures.previous.title"), dataCollectionView.getPreviousTitle());
		}
		String period = EvaluationFormFormatter.period(dataCollectionView.getStart(), dataCollectionView.getDeadline(),
				getLocale());
		builder.addCustomFigure(translate("data.collection.figures.period"), period);
		
		EvaluationFormSurvey survey = evaluationFormManager.loadSurvey(dataCollection, null);
		Form form = evaluationFormManager.loadForm(survey.getFormEntry());
		SessionFilter filter = SessionFilterFactory.createSelectDone(survey);
		
		reportsCtrl = new EvaluationFormReportsController(ureq, getWindowControl(), form,
				filter, ReportSegment.OVERVIEW, reportHeaderCtrl.getInitialComponent(), builder.build());
		flc.put("report", reportsCtrl.getInitialComponent());
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	@Override
	protected void doDispose() {
		removeAsListenerAndDispose(reportHeaderCtrl);
		removeAsListenerAndDispose(reportsCtrl);
		reportHeaderCtrl = null;
		reportsCtrl = null;
	}
}
