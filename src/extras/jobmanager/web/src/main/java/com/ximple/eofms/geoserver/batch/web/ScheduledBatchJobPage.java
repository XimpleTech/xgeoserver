package com.ximple.eofms.geoserver.batch.web;

import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.ximple.eofms.geoserver.batch.job.BatchJobContext;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.XMarkInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.xmark.XMarkNewPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geotools.util.logging.Logging;
import org.quartz.Scheduler;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.ximple.eofms.geoserver.batch.job.Constants;

public class ScheduledBatchJobPage extends GeoServerSecuredPage {
    static Logger LOGGER = Logging.getLogger(ScheduledBatchJobPage.class);

    public static Scheduler getScheduler(ServletContext context)  {
        return (Scheduler) WebApplicationContextUtils.getWebApplicationContext(context)
                .getBean(Constants.GEOS_SCHEDULER_FACTORY);
    }

    GeoServerTablePanel<BatchJobContext> table;

    GeoServerDialog dialog;

    SelectionExecuteJobLink executeJob;

    public ScheduledBatchJobPage() {
        // add(new Label("id", new PropertyModel(model, "id")));

        BatchJobProvider provider = new BatchJobProvider();
        add(table = new GeoServerTablePanel<BatchJobContext>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                                                        GeoServerDataProvider.Property<BatchJobContext> property) {
                return null;
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                executeJob.setEnabled(table.getSelection().size() > 0);
                target.addComponent(executeJob);
            }

        });
        table.setOutputMarkupId(true);

        // the confirm dialog
        add(dialog = new GeoServerDialog("dialog"));
        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        // header.add(new BookmarkablePageLink("addNew", XMarkNewPage.class));

        // the removal button
        header.add(executeJob = new SelectionExecuteJobLink("executeSelected", table, dialog) {
            @Override
            protected StringResourceModel canExecute(BatchJobContext object) {
                BatchJobContext s = (BatchJobContext) object;
                /*
                if ( XMarkInfo.DEFAULT_POINT.equals( s.getName() ) ||
                    XMarkInfo.DEFAULT_LINE.equals( s.getName() ) ||
                    XMarkInfo.DEFAULT_POLYGON.equals( s.getName() ) ||
                    XMarkInfo.DEFAULT_RASTER.equals( s.getName() ) ) {
                    return new StringResourceModel("cantRemoveDefaultXMark", XMarkPage.this, null );
                }
                */
                return null;
            }
        });
        executeJob.setOutputMarkupId(true);
        executeJob.setEnabled(false);

        return header;
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
