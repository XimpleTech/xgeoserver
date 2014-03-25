package org.geoserver.catalog.impl;

import java.io.IOException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.XMarkInfo;
import org.geotools.renderer.style.XShapeMarks;
import org.geotools.styling.Style;

public class XMarkInfoImpl implements XMarkInfo {
    protected String id;

    protected String name;

    protected WorkspaceInfo workspace;

    protected String filename;

    protected transient Catalog catalog;

    public XMarkInfoImpl() {
    }

    public XMarkInfoImpl(Catalog catalog) {
        this.catalog = catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public WorkspaceInfo getWorkspace() {
        return workspace;
    }

    @Override
    public void setWorkspace(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String fileName) {
        this.filename = fileName;
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        visitor.visit( this );
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public XShapeMarks getXMark() throws IOException {
        return catalog.getResourcePool().getXMark(this);
    }

    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((filename == null) ? 0 : filename.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof XMarkInfo))
            return false;
        final XMarkInfo other = (XMarkInfo) obj;
        if (filename == null) {
            if (other.getFilename() != null)
                return false;
        } else if (!filename.equals(other.getFilename()))
            return false;
        if (id == null) {
            if (other.getId() != null)
                return false;
        } else if (!id.equals(other.getId()))
            return false;
        if (name == null) {
            if (other.getName() != null)
                return false;
        } else if (!name.equals(other.getName()))
            return false;
        if (workspace == null) {
            if (other.getWorkspace() != null)
                return false;
        } else if (!workspace.equals(other.getWorkspace()))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[').append(name).append(']')
                .toString();
    }

    private Object readResolve() {
        return this;
    }
}
