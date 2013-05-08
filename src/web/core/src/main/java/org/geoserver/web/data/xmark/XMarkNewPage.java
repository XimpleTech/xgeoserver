package org.geoserver.web.data.xmark;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.XMarkInfo;
import org.geotools.util.Version;

public class XMarkNewPage extends AbstractXMarkPage {
    public XMarkNewPage() {
        initUI(null);
    }

    @Override
    protected void initUI(XMarkInfo style) {
        super.initUI(style);

        if (!isAuthenticatedAsAdmin()) {
            //initialize the workspace drop down
            DropDownChoice<WorkspaceInfo> wsChoice =
                    (DropDownChoice<WorkspaceInfo>) get("form:workspace");

            //default to first available workspace
            List<WorkspaceInfo> ws = getCatalog().getWorkspaces();
            if (!ws.isEmpty()) {
                wsChoice.setModelObject(ws.get(0));
            }
        }
    }

    @Override
    protected void onXMarkFormSubmit() {
        // add the style
        Catalog catalog = getCatalog();
        XMarkInfo s = (XMarkInfo) xmarkForm.getModelObject();

        // write out the SLD before creating the style
        try {
            if (s.getFilename() == null) {
                // TODO: check that this does not overriDe any existing files
                s.setFilename(s.getName() + ".sld");
            }
            catalog.getResourcePool().writeXMark(s,
                    new ByteArrayInputStream(rawXMark.getBytes()));
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }

        // store in the catalog
        try {
            getCatalog().add(s);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred saving the style", e);
            error(e);
            return;
        }

        doReturn(XMarkPage.class);
    }
}
