package org.geoserver.web.data.xmark;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.XMarkInfo;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.SelectionRemovalLink;
import org.geoserver.web.data.workspace.WorkspaceEditPage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

/**
 *
 */
public class XMarkPage extends GeoServerSecuredPage {

    GeoServerTablePanel<XMarkInfo> table;

    SelectionRemovalLink removal;

    GeoServerDialog dialog;

    public XMarkPage() {
        XMarkProvider provider = new XMarkProvider();
        add(table = new GeoServerTablePanel<XMarkInfo>("table", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    GeoServerDataProvider.Property<XMarkInfo> property) {

                if ( property == XMarkProvider.NAME ) {
                    return xmarkLink(id, itemModel);
                }
                if (property == XMarkProvider.WORKSPACE) {
                    return workspaceLink(id, itemModel);
                }
                return null;
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                removal.setEnabled(table.getSelection().size() > 0);
                target.addComponent(removal);
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
        header.add(new BookmarkablePageLink("addNew", XMarkNewPage.class));

        // the removal button
        header.add(removal = new SelectionRemovalLink("removeSelected", table, dialog) {
            @Override
            protected StringResourceModel canRemove(CatalogInfo object) {
                XMarkInfo s = (XMarkInfo) object;
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
        removal.setOutputMarkupId(true);
        removal.setEnabled(false);

        return header;
    }

    Component xmarkLink( String id, IModel model ) {
        IModel nameModel = XMarkProvider.NAME.getModel(model);
        IModel wsModel = XMarkProvider.WORKSPACE.getModel(model);

        String name = (String) nameModel.getObject();
        String wsName = (String) wsModel.getObject();

        return new SimpleBookmarkableLink(id, XMarkEditPage.class, nameModel,
            XMarkEditPage.NAME, name, XMarkEditPage.WORKSPACE, wsName);
    }

    Component workspaceLink( String id, IModel model ) {
        IModel wsNameModel = XMarkProvider.WORKSPACE.getModel(model);
        String wsName = (String) wsNameModel.getObject();
        if (wsName != null) {
            return new SimpleBookmarkableLink(
                id, WorkspaceEditPage.class, new Model(wsName), "name", wsName);
        }
        else {
            return new WebMarkupContainer(id);
        }
    }

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
