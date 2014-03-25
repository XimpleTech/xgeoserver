package org.geoserver.catalog.rest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.XMarkInfo;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geotools.renderer.style.XShapeMarks;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class XMarkResource extends AbstractCatalogResource {

    public XMarkResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, XMarkInfo.class, catalog);

    }

    @Override
    protected Object handleObjectGet() throws Exception {
        String workspace = getAttribute("workspace");
        String xmark = getAttribute("xmark");

        LOGGER.fine("GET xmark " + xmark);
        XMarkInfo xinfo = workspace == null ? catalog.getXMarkByName(xmark) :
            catalog.getXMarkByName(workspace, xmark);

        //check the format, if specified as sld, return the sld itself
        DataFormat format = getFormatGet();
        /*
        if ( format instanceof SLDFormat ) {
            try {
                return xinfo.getXMark();
            }
            catch (IOException e) {
                throw new RestletException( "", Status.SERVER_ERROR_INTERNAL, e );
            }
        }
        */
        return xinfo;
    }

    @Override
    public boolean allowPost() {
        if (getAttribute("workspace") == null && !isAuthenticatedAsAdmin()) {
            return false;
        }
        return getAttribute("xmark") == null;
    }

    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String workspace = getAttribute("workspace");

        if (object instanceof XMarkInfo) {
            XMarkInfo xmark = (XMarkInfo) object;

            if (workspace != null) {
                xmark.setWorkspace(catalog.getWorkspaceByName(workspace));
            }

            catalog.add(xmark);
            LOGGER.info("POST xmark " + xmark.getName());

            return xmark.getName();
        } else if (object instanceof XShapeMarks) {
            XShapeMarks xmark = (XShapeMarks) object;

            //figure out the name of the new xmark, first check if specified directly
            String name = getRequest().getResourceRef().getQueryAsForm().getFirstValue("name");

            if (name == null) {
                //infer name from sld
                name = xmark.getName();
            }

            if (name == null) {
                throw new RestletException("XMark must have a name.", Status.CLIENT_ERROR_BAD_REQUEST);
            }

            //ensure that the xmark does not already exist
            if (catalog.getXMarkByName(workspace, name) != null) {
                throw new RestletException("XMark " + name + " already exists.", Status.CLIENT_ERROR_FORBIDDEN);
            }

            //serialize the xmark out into the data directory
            GeoServerResourceLoader loader = catalog.getResourceLoader();
            String path = "xmarks/" + name + ".xmk";
            if (workspace != null) {
                path = "workspaces/" + workspace + "/" + path;
            }

            File f;
            try {
                f = loader.find(path);
            } catch (IOException e) {
                throw new RestletException("Error looking up file", Status.SERVER_ERROR_INTERNAL, e);
            }

            if (f != null) {
                String msg = "XMK file " + path + ".xmk already exists.";
                throw new RestletException(msg, Status.CLIENT_ERROR_FORBIDDEN);
            }

            //TODO: have the writing out of the xmark delegate to ResourcePool.writeXMark()
            try {
                f = loader.createFile(path);

                //serialize the file to the xmarks directory
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(f));

                // SLDFormat format = new SLDFormat(true);
                // format.toRepresentation(xmark).write(out);

                out.flush();
                out.close();
            } catch (IOException e) {
                throw new RestletException("Error creating file", Status.SERVER_ERROR_INTERNAL, e);
            }

            //create a xmark info object
            XMarkInfo xinfo = catalog.getFactory().createXMark();
            xinfo.setName(name);
            xinfo.setFilename(f.getName());

            if (workspace != null) {
                xinfo.setWorkspace(catalog.getWorkspaceByName(workspace));
            }

            catalog.add(xinfo);

            LOGGER.info("POST XMK " + name);
            return name;
        }

        return null;
    }

    @Override
    public boolean allowPut() {
        if (getAttribute("workspace") == null && !isAuthenticatedAsAdmin()) {
            return false;
        }
        return getAttribute("xmark") != null;
    }

    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String xmark = getAttribute("xmark");
        String workspace = getAttribute("workspace");

        if (object instanceof XMarkInfo) {
            XMarkInfo x = (XMarkInfo) object;
            XMarkInfo original = catalog.getXMarkByName(workspace, xmark);

            //ensure no workspace change
            if (x.getWorkspace() != null) {
                if (!x.getWorkspace().equals(original.getWorkspace())) {
                    throw new RestletException("Can't change the workspace of a xmark, instead " +
                        "DELETE from existing workspace and POST to new workspace", Status.CLIENT_ERROR_FORBIDDEN);
                }
            }

            new CatalogBuilder(catalog).updateXMark(original, x);
            catalog.save(original);
        } else if (object instanceof XShapeMarks) {
            /*
             * Force the .sld file to be overriden and it's XMark object cleared from the
             * ResourcePool cache
             */
            XMarkInfo x = catalog.getXMarkByName(workspace, xmark);
            catalog.getResourcePool().writeXMark(x, (XShapeMarks) object, true);
            /*
             * make sure to save the XMarkInfo so that the Catalog issues the notification events
             */
            catalog.save(x);
        }

        LOGGER.info("PUT xmark " + xmark);
    }

    @Override
    public boolean allowDelete() {
        return getAttribute("xmark") != null;
    }

    @Override
    protected void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        String xmark = getAttribute("xmark");
        XMarkInfo x = workspace != null ? catalog.getXMarkByName(workspace, xmark) :
            catalog.getXMarkByName(xmark);

        catalog.remove(x);

        //check purge parameter to determine if the underlying file
        // should be deleted
        String p = getRequest().getResourceRef().getQueryAsForm().getFirstValue("purge");
        boolean purge = (p != null) ? Boolean.parseBoolean(p) : false;
        catalog.getResourcePool().deleteXMark(x, purge);

        LOGGER.info("DELETE xmark " + xmark);

    }
}
