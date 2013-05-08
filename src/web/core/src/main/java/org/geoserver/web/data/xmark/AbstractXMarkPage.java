package org.geoserver.web.data.xmark;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.ajax.calldecorator.AjaxPreprocessingCallDecorator;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.XMarkInfo;
import org.geoserver.catalog.XMarks;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.data.workspace.WorkspaceChoiceRenderer;
import org.geoserver.web.data.workspace.WorkspacesModel;
import org.geoserver.web.wicket.CodeMirrorEditor;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.xml.sax.SAXParseException;

@SuppressWarnings("serial")
public abstract class AbstractXMarkPage extends GeoServerSecuredPage {

    protected TextField nameTextField;

    protected FileUploadField fileUploadField;

    protected DropDownChoice xmarks;

    protected AjaxSubmitLink copyLink;

    protected Form uploadForm;

    protected Form xmarkForm;

    protected CodeMirrorEditor editor;

    String rawXMark;

    public AbstractXMarkPage() {
    }

    public AbstractXMarkPage(XMarkInfo xmarkInfo) {
        initUI(xmarkInfo);
    }

    protected void initUI(XMarkInfo xmark) {
        IModel<XMarkInfo> xmarkModel = new CompoundPropertyModel(xmark != null ?
            new XMarkDetachableModel(xmark) : getCatalog().getFactory().createXMark());

        xmarkForm = new Form("form", xmarkModel) {
            @Override
            protected void onSubmit() {
                super.onSubmit();
                onXMarkFormSubmit();
            }
        };
        xmarkForm.setMarkupId("mainForm");
        add(xmarkForm);

        xmarkForm.add(nameTextField = new TextField("name"));
        nameTextField.setRequired(true);

        DropDownChoice<WorkspaceInfo> wsChoice =
            new DropDownChoice("workspace", new WorkspacesModel(), new WorkspaceChoiceRenderer());
        wsChoice.setNullValid(true);
        if (!isAuthenticatedAsAdmin()) {
            wsChoice.setNullValid(false);
            wsChoice.setRequired(true);
        }

        xmarkForm.add(wsChoice);
        xmarkForm.add(editor = new CodeMirrorEditor("XMark", new PropertyModel(this, "rawXMark")));
        // force the id otherwise this blasted thing won't be usable from other forms
        editor.setTextAreaMarkupId("editor");
        editor.setOutputMarkupId(true);
        editor.setRequired(true);
        xmarkForm.add(editor);

        if (xmark != null) {
            try {
                setRawXMark(readFile(xmark));
            } catch (IOException e) {
                // ouch, the xmark file is gone! Register a generic error message
                Session.get().error(new ParamResourceModel("xmarkNotFound", this, xmark.getFilename()).getString());
            }
        }

        // xmark copy functionality
        xmarks = new DropDownChoice("existingXMarks", new Model(), new XMarksModel(), new XMarkChoiceRenderer());
        xmarks.setOutputMarkupId(true);
        xmarks.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                xmarks.validate();
                copyLink.setEnabled(xmarks.getConvertedInput() != null);
                target.addComponent(copyLink);
            }
        });
        xmarkForm.add(xmarks);
        copyLink = copyLink();
        copyLink.setEnabled(false);
        xmarkForm.add(copyLink);

        uploadForm = uploadForm(xmarkForm);
        uploadForm.setMultiPart(true);
        uploadForm.setMaxSize(Bytes.megabytes(1));
        uploadForm.setMarkupId("uploadForm");
        add(uploadForm);

        uploadForm.add(fileUploadField = new FileUploadField("filename"));


        add(validateLink());
        Link cancelLink = new Link("cancel") {
            @Override
            public void onClick() {
                doReturn(XMarkPage.class);
            }
        };
        add(cancelLink);
    }

    Form uploadForm(final Form form) {
        return new Form("uploadForm") {
            @Override
            protected void onSubmit() {
                FileUpload upload = fileUploadField.getFileUpload();
                if (upload == null) {
                    warn("No file selected.");
                    return;
                }
                ByteArrayOutputStream bout = new ByteArrayOutputStream();

                try {
                    IOUtils.copy(upload.getInputStream(), bout);
                    setRawXMark(new InputStreamReader(new ByteArrayInputStream(bout.toByteArray()), "UTF-8"));
                    editor.setModelObject(rawXMark);
                } catch (IOException e) {
                    throw new WicketRuntimeException(e);
                }

                // update the xmark object
                XMarkInfo s = (XMarkInfo) form.getModelObject();
                if (s.getName() == null || "".equals(s.getName().trim())) {
                    // set it
                    nameTextField.setModelValue(ResponseUtils.stripExtension(upload
                        .getClientFileName()));
                    nameTextField.modelChanged();
                }
            }
        };
    }

    Component validateLink() {
        return new GeoServerAjaxFormLink("validate", xmarkForm) {

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                editor.processInput();
                List<Exception> errors = validateXMark();

                if (errors.isEmpty()) {
                    form.info("No validation errors.");
                } else {
                    for (Exception e : errors) {
                        form.error(xmarkErrorWithLineNo(e));
                    }
                }
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return editor.getSaveDecorator();
            }

            ;
        };
    }

    private String xmarkErrorWithLineNo(Exception e) {
        if (e instanceof SAXParseException) {
            SAXParseException se = (SAXParseException) e;
            return "line " + se.getLineNumber() + ": " + e.getLocalizedMessage();
        }
        String message = e.getLocalizedMessage();
        if (message != null) {
            return message;
        } else {
            return new ParamResourceModel("genericError", this).getString();
        }
    }

    List<Exception> validateXMark() {
        try {
            final String mark = editor.getInput();
            ByteArrayInputStream input = new ByteArrayInputStream(mark.getBytes());
            // List<Exception> validationErrors = XMarks.validate(input, null);
            // TODO: Ximple Mark Validate
            List<Exception> validationErrors = new ArrayList<Exception>();
            return validationErrors;
        } catch (Exception e) {
            return Arrays.asList(e);
        }
    }

    AjaxSubmitLink copyLink() {
        return new AjaxSubmitLink("copy") {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                // we need to force validation or the value won't be converted
                xmarks.processInput();
                XMarkInfo xmark = (XMarkInfo) xmarks.getConvertedInput();

                if (xmark != null) {
                    try {
                        // same here, force validation or the field won't be udpated
                        editor.reset();
                        setRawXMark(readFile(xmark));
                    } catch (Exception e) {
                        error("Errors occurred loading the '" + xmark.getName() + "' xmark");
                    }
                    target.addComponent(xmarkForm);
                }
            }

            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new AjaxPreprocessingCallDecorator(super.getAjaxCallDecorator()) {

                    @Override
                    public CharSequence preDecorateScript(CharSequence script) {
                        return "if(event.view.document.gsEditors."
                            + editor.getTextAreaMarkupId()
                            + ".getCode() != '' &&"
                            + "!confirm('"
                            + new ParamResourceModel("confirmOverwrite", AbstractXMarkPage.this)
                            .getString() + "')) return false;" + script;
                    }
                };
            }

            @Override
            public boolean getDefaultFormProcessing() {
                return false;
            }

        };
    }

    Reader readFile(XMarkInfo xmark) throws IOException {
        ResourcePool pool = getCatalog().getResourcePool();
        return pool.readXMark(xmark);
    }

    public void setRawXMark(Reader in) throws IOException {
        BufferedReader bin = null;
        if (in instanceof BufferedReader) {
            bin = (BufferedReader) in;
        } else {
            bin = new BufferedReader(in);
        }

        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = bin.readLine()) != null) {
            builder.append(line).append("\n");
        }

        this.rawXMark = builder.toString();
        editor.setModelObject(rawXMark);
        in.close();
    }

    /**
     * Subclasses must implement to define the submit behavior
     */
    protected abstract void onXMarkFormSubmit();

    @Override
    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.WORKSPACE_ADMIN;
    }
}
