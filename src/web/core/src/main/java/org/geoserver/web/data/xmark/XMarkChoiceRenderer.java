package org.geoserver.web.data.xmark;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.XMarkInfo;

public class XMarkChoiceRenderer implements IChoiceRenderer {
    @Override
    public Object getDisplayValue(Object o) {
        return ((XMarkInfo) o).getName();
    }

    @Override
    public String getIdValue(Object o, int i) {
        return ((XMarkInfo) o).getId();
    }
}
