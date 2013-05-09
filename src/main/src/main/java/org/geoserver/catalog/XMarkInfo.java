package org.geoserver.catalog;

import java.io.IOException;

import org.geotools.renderer.style.XShapeMarks;
import org.geotools.styling.Style;

public interface XMarkInfo extends CatalogInfo {

    /**
     * Name of the style.
     * <p>
     * This value is unique among all styles and can be used to identify the
     * style.
     * </p>
     *
     * @uml.property name="name"
     */
    String getName();

    /**
     * Sets the name of the style.
     *
     * @uml.property name="name"
     */
    void setName(String name);

    /**
     * The workspace the style is part of, or <code>null</code> if the style is global.
     */
    WorkspaceInfo getWorkspace();

    /**
     * Sets the workspace the style is part of.
     */
    void setWorkspace(WorkspaceInfo workspace);

    /**
     * The name of the file the style originates from.
     */
    String getFilename();

    /**
     * Sets the name of the file the style originated from.
     */
    void setFilename( String fileName );

    /**
     * The xmark object.
     */
    XShapeMarks getXMark() throws IOException;
}
