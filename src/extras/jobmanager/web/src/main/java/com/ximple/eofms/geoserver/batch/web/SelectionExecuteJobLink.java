package com.ximple.eofms.geoserver.batch.web;

import java.util.List;

import com.ximple.eofms.geoserver.batch.job.BatchJobContext;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 *
 */
public class SelectionExecuteJobLink extends AjaxLink {
    GeoServerTablePanel<? extends BatchJobContext> jobContextObjects;
    GeoServerDialog dialog;

    public SelectionExecuteJobLink(String id, GeoServerTablePanel<BatchJobContext> jobContextObjects, GeoServerDialog dialog) {
        super(id);
        this.jobContextObjects = jobContextObjects;
        this.dialog = dialog;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        // see if the user selected anything
        final List<? extends BatchJobContext> selection = jobContextObjects.getSelection();
        if(selection.size() == 0)
            return;

        dialog.setTitle(new ParamResourceModel("confirmExecute", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
            protected Component getContents(String id) {
                // show a confirmation panel for all the objects we have to remove
                return new ConfirmExecutePanel(id, selection) {
                    @Override
                    protected StringResourceModel canExecute(BatchJobContext info) {
                        return SelectionExecuteJobLink.this.canExecute(info);
                    }
                };
            }

            protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                /*
                // cascade delete the whole selection
                Catalog catalog = GeoServerApplication.get().getCatalog();
                CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
                for (CatalogInfo ci : selection) {
                    ci.accept(visitor);
                }
                */
                for (BatchJobContext bjc : selection) {
                    JobManagerWebUtils.jobManager().executeJob(bjc);
                }

                // the deletion will have changed what we see in the page
                // so better clear out the selection
                jobContextObjects.clearSelection();
                return true;
            }

            @Override
            public void onClose(AjaxRequestTarget target) {
                // if the selection has been cleared out it's sign a deletion
                // occurred, so refresh the table
                if(jobContextObjects.getSelection().size() == 0) {
                    setEnabled(false);
                    target.addComponent(SelectionExecuteJobLink.this);
                    target.addComponent(jobContextObjects);
                }
            }

        });

    }

    protected StringResourceModel canExecute(BatchJobContext info) {
        return null;
    }
}
