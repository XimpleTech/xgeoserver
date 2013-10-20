package com.ximple.eofms.geoserver.batch.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ximple.eofms.geoserver.batch.job.BatchJobContext;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;

/**
 *
 */
public class ConfirmExecutePanel extends Panel {
    List<? extends BatchJobContext> roots;

    public ConfirmExecutePanel(String id, BatchJobContext... roots) {
        this(id, Arrays.asList(roots));
    }

    public ConfirmExecutePanel(String id, List<? extends BatchJobContext> roots) {
        super(id);
        this.roots = roots;

        // track objects that could not be executed
        Map<BatchJobContext, StringResourceModel> notExecuted = new HashMap();

        // collect the objects that will be executed (besides the roots)
        /*
        Catalog catalog = GeoServerApplication.get().getCatalog();
        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);
        for (Iterator<CatalogInfo> i = (Iterator<CatalogInfo>) roots.iterator(); i.hasNext();) {
            CatalogInfo root = i.next();
            StringResourceModel reason = canRemove(root);
            if ( reason != null ) {
                notRemoved.put(root, reason);
                i.remove();
            }
            else {
                root.accept(visitor);
            }
        }
        visitor.removeAll(roots);
        */

        // add roots
        WebMarkupContainer root = new WebMarkupContainer("rootObjects");
        root.add(new Label("rootObjectNames", names(roots)));
        root.setVisible( !roots.isEmpty() );
        add(root);

        // objects that could not be executed
        WebMarkupContainer nr = new WebMarkupContainer("notExecutedObjects");
        nr.setVisible(!notExecuted.isEmpty());
        // nr.add(notRemovedList(notRemoved));
        add(nr);

        // executed objects root (we show it if any executed object is on the list)
        WebMarkupContainer executed = new WebMarkupContainer("executedObjects");
        /*
        List<CatalogInfo> cascaded = visitor.getObjects(CatalogInfo.class, DELETE);
        // remove the resources, they are cascaded, but won't be show in the UI
        for (Iterator it = cascaded.iterator(); it.hasNext();) {
            CatalogInfo catalogInfo = (CatalogInfo) it.next();
            if(catalogInfo instanceof ResourceInfo)
                it.remove();
        }
        executed.setVisible(cascaded.size() > 0);
        */
        add(executed);

        /*
        // executed workspaces
        WebMarkupContainer wsr = new WebMarkupContainer("workspacesRemoved");
        executed.add(wsr);
        List<WorkspaceInfo> workspaces = visitor.getObjects(WorkspaceInfo.class);
        if(workspaces.size() == 0)
            wsr.setVisible(false);
        wsr.add(new Label("workspaces", names(workspaces)));

        // executed stores
        WebMarkupContainer str = new WebMarkupContainer("storesRemoved");
        executed.add(str);
        List<StoreInfo> stores = visitor.getObjects(StoreInfo.class);
        if(stores.size() == 0)
            str.setVisible(false);
        str.add(new Label("stores", names(stores)));

        // executed layers
        WebMarkupContainer lar = new WebMarkupContainer("layersRemoved");
        executed.add(lar);
        List<LayerInfo> layers = visitor.getObjects(LayerInfo.class, DELETE);
        if(layers.size() == 0)
            lar.setVisible(false);
        lar.add(new Label("layers", names(layers)));

        // executed groups
        WebMarkupContainer grr = new WebMarkupContainer("groupsRemoved");
        executed.add(grr);
        List<LayerGroupInfo> groups = visitor.getObjects(LayerGroupInfo.class, DELETE);
        if(groups.size() == 0)
            grr.setVisible(false);
        grr.add(new Label("groups", names(groups)));

        // modified objects root (we show it if any modified object is on the list)
        WebMarkupContainer modified = new WebMarkupContainer("modifiedObjects");
        modified.setVisible(visitor.getObjects(null, EXTRA_STYLE_REMOVED, GROUP_CHANGED, STYLE_RESET).size() > 0);
        add(modified);

        // layers modified
        WebMarkupContainer lam = new WebMarkupContainer("layersModified");
        modified.add(lam);
        layers = visitor.getObjects(LayerInfo.class,
                STYLE_RESET, EXTRA_STYLE_REMOVED);
        if(layers.size() == 0)
            lam.setVisible(false);
        lam.add(new Label("layers", names(layers)));

        // groups modified
        WebMarkupContainer grm = new WebMarkupContainer("groupsModified");
        modified.add(grm);
        groups = visitor.getObjects(LayerGroupInfo.class, GROUP_CHANGED);
        if(groups.size() == 0)
            grm.setVisible(false);
        grm.add(new Label("groups", names(groups)));
        */
    }

    public List<? extends BatchJobContext> getRoots() {
        return roots;
    }

    String names(List objects) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < objects.size(); i++) {
            sb.append(name(objects.get(i)));
            if(i < (objects.size() - 1))
                sb.append(", ");
        }
        return sb.toString();
    }

    String name(Object object) {
        try {
            return (String) BeanUtils.getProperty(object, "jobName");
        } catch(Exception e) {
            throw new RuntimeException("A catalog object that does not have " +
                        "a 'name' property has been used, this is unexpected", e);
        }
    }

    ListView notExecutedList(final Map<BatchJobContext,StringResourceModel> notExecuted) {
        List<BatchJobContext> items = new ArrayList(notExecuted.keySet());
        ListView lv = new ListView("notExecutedList", items) {

            @Override
            protected void populateItem(ListItem item) {
                BatchJobContext object = (BatchJobContext) item.getModelObject();
                StringResourceModel reason = notExecuted.get(object);
                item.add( new Label( "name", name(object) ) );
                item.add( new Label( "reason", reason));
            }
        };
        return lv;
    }

    protected StringResourceModel canExecute(BatchJobContext info) {
        return null;
    }
}
