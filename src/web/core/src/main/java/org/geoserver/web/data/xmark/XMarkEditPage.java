package org.geoserver.web.data.xmark;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.wicket.PageParameters;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.XMarkInfo;
import org.geoserver.catalog.XMarks;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.Version;

public class XMarkEditPage extends AbstractXMarkPage {
    public static final String NAME = "name";
    public static final String WORKSPACE = "workspace";

    public XMarkEditPage(PageParameters parameters) {
        String name = parameters.getString(NAME);
        String workspace = parameters.getString(WORKSPACE);

        XMarkInfo mi = workspace != null ? getCatalog().getXMarkByName(workspace, name) :
            getCatalog().getXMarkByName(name);

        if (mi == null) {
            error(new ParamResourceModel("XMarkEditPage.notFound", this, name).getString());
            doReturn(XMarkPage.class);
            return;
        }

        initUI(mi);

        if (!isAuthenticatedAsAdmin()) {
            Form f = (Form) get("form");

            //global styles only editable by full admin
            if (mi.getWorkspace() == null) {
                xmarkForm.setEnabled(false);
                nameTextField.setEnabled(false);
                uploadForm.setEnabled(false);

                editor.add(new AttributeAppender("class", new Model("disabled"), " "));
                get("validate").add(new AttributeAppender("style", new Model("display:none;"), " "));
                add(new AbstractBehavior() {
                    @Override
                    public void renderHead(IHeaderResponse response) {
                        response.renderOnLoadJavascript(
                            "document.getElementById('mainFormSubmit').xmark.display = 'none';");
                        response.renderOnLoadJavascript(
                            "document.getElementById('uploadFormSubmit').xmark.display = 'none';");
                    }
                });

                info(new StringResourceModel("globalXMarkReadOnly", this, null).getString());
            }

            //always disable the workspace toggle
            f.get("workspace").setEnabled(false);
        }
    }

    public XMarkEditPage(XMarkInfo xmark) {
        super(xmark);
        uploadForm.setVisible(false);
    }

    @Override
    protected void onXMarkFormSubmit() {
        // write out the file and save name modifications
        try {
            XMarkInfo xmark = (XMarkInfo) xmarkForm.getModelObject();

            // write out the XMark
            try {
                getCatalog().getResourcePool().writeXMark(xmark,
                    new ByteArrayInputStream(rawXMark.getBytes()));
            } catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
            getCatalog().save(xmark);
            doReturn(XMarkPage.class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred saving the xmark", e);
            xmarkForm.error(e);
        }
    }
}
